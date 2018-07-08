package lf.wo.enlight.ui.main

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import kotlinx.android.synthetic.main.light_detail_fragment.*
import lf.wo.enlight.R
import lf.wo.enlight.kotlin.*
import wo.lf.lifx.api.*
import wo.lf.lifx.domain.HSBK
import wo.lf.lifx.domain.PowerState
import wo.lf.lifx.extensions.copy
import wo.lf.lifx.extensions.fireAndForget

class LightDetailFragment : Fragment(), IZoneClickedHandler {
    override fun zoneClicked(zone: Int, selected: Boolean) {
        viewModel.setSelection(zone, selected)
    }

    companion object {
        fun newInstance() = LightDetailFragment()
    }

    private lateinit var viewModel: LightDetailViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              syavedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.light_detail_fragment, container, false)
    }

    private var inUpdate: Boolean = false

    private lateinit var adapter: ZonesAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(LightDetailViewModel::class.java)

        val lightId = LightDetailFragmentArgs.fromBundle(arguments).lightId.toLong()
        viewModel.initialize(lightId)

        viewModel.settings.observe(this, Observer { settings ->
            if (settings == null) {

            } else {
                when (settings.selectionMode) {
                    ZoneSelectionMode.ALL -> {
                        selectionMode.setImageResource(R.drawable.ic_selection_off)
                        adapter.selectedZones = setOf()
                    }
                    ZoneSelectionMode.INDIVIDUAL -> {
                        selectionMode.setImageResource(R.drawable.ic_selection)
                        adapter.selectedZones = settings.selectedZones
                    }
                }

            }
        })

        viewModel.light.observe(this, Observer<Light> { light: Light? ->
            if (light == null) {

                ledState.adapter = null

            } else {
                inUpdate = true

                if (light.productInfo.hasMultiZoneSupport) {
                    zoneGroup.visibility = View.VISIBLE
                } else {
                    zoneGroup.visibility = View.GONE
                }

                if (lightName.text.toString() != light.label) {
                    lightName.setText(light.label)
                }

                if (light.on != powerSwitch.isChecked) {
                    powerSwitch.isChecked = light.on
                }

                if (light.color.brightness.toPercent != brightnessBar.progress) {
                    brightnessBar.progress = light.color.brightness.toPercent
                }

                val displayedColor = if (viewModel.settings.value?.selectionMode == ZoneSelectionMode.INDIVIDUAL) {
                    light.zones.colors[viewModel.settings.value?.selectedZones?.sorted()?.first()
                            ?: 0]
                } else {
                    light.color
                }
                val rgb = Color.HSVToColor(floatArrayOf(displayedColor.hue.degreesToFloat, displayedColor.saturation.toUnsignedFloat, displayedColor.brightness.toUnsignedFloat))
                if (rgb != colorPicker.color) {
                    Log.w("color", "updating to : $rgb ${light.color}")
                    colorPicker.color = rgb
                }

                adapter.light = light
                adapter.zones = light.zones

                inUpdate = false
            }
        })

        ledState.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        adapter = ZonesAdapter(null, Zones(0, listOf()), setOf(), this)
        ledState.adapter = adapter
    }

    override fun onPause() {
        super.onPause()
        powerSwitch.setOnCheckedChangeListener(null)
        brightnessBar.setOnSeekBarChangeListener(null)
        colorPicker.setOnColorChangedListener(null)
    }

    override fun onResume() {
        super.onResume()
        powerSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.light.value?.let { light ->
                if (light.on != isChecked) {
                    LightSetPowerCommand.create(light, isChecked, 50).fireAndForget()
                }
            }
        }

        brightnessBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.light.value?.let { light ->
                    if (!inUpdate) {
                        val brightness = progress.toUnsignedShort
                        if (viewModel.settings.value?.selectionMode == ZoneSelectionMode.INDIVIDUAL) {
                            viewModel.settings.value?.selectedZones?.let {
                                it.sorted().fold(listOf<Pair<IntRange, HSBK>>()) { acc, i ->
                                    val last = acc.lastOrNull()
                                    val color = light.zones.colors[i]
                                    if (last != null && last.first.last + 1 == i && last.second == color) {
                                        acc.dropLast(1).plus(Pair(IntRange(last.first.start, i), last.second))
                                    } else {
                                        acc.plus(Pair(IntRange(i, i), color))
                                    }
                                }.forEach {
                                    MultiZoneSetColorCommand.create(light, it.second.copy(brightness = brightness), it.first.start, it.first.endInclusive, 50).fireAndForget()
                                }
                            }
                        } else {
                            LightSetBrightness.create(light, brightness, 1000).fireAndForget()
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        colorPicker.setOnColorChangedListener { color ->
            viewModel.light.value?.let { light ->
                if (!inUpdate) {
                    val hsv = floatArrayOf(0f, 0f, 0f)
                    Color.colorToHSV(color, hsv)
                    Log.w("color", "onChanged : $color $hsv")
                    val color = light.color.copy(hue = hsv[0].toDegreeShort, saturation = hsv[1].toUnsignedShort, brightness = hsv[2].toUnsignedShort)
                    if (viewModel.settings.value?.selectionMode == ZoneSelectionMode.INDIVIDUAL) {
                        viewModel.settings.value?.selectedZones?.let {
                            it.sorted().fold(listOf<Pair<IntRange, HSBK>>()) { acc, i ->
                                val last = acc.lastOrNull()
                                if (last != null && last.first.last + 1 == i) {
                                    acc.dropLast(1).plus(Pair(IntRange(last.first.start, i), last.second))
                                } else {
                                    acc.plus(Pair(IntRange(i, i), color))
                                }
                            }.forEach {
                                MultiZoneSetColorCommand.create(light, color, it.first.start, it.first.endInclusive, 50).fireAndForget()
                            }
                        }
                    } else {
                        LightSetColorCommand.create(light, color, 1000).fireAndForget()
                    }
                }
            }
        }

        selectionMode.setOnClickListener {
            if (viewModel.settings.value?.selectionMode == ZoneSelectionMode.ALL) {
                viewModel.setZoneSelectionMode(ZoneSelectionMode.INDIVIDUAL)
            } else {
                viewModel.setZoneSelectionMode(ZoneSelectionMode.ALL)
            }
        }

        lightName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!inUpdate) {
                    viewModel.light.value?.let { light ->
                        DeviceSetLabelCommand.create(light, s.toString()).fireAndForget()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })
    }
}

interface IZoneClickedHandler {
    fun zoneClicked(zone: Int, selected: Boolean)
}

class ZonesAdapter(light: Light?, zones: Zones, selectedZones: Set<Int>, private val clickHandler: IZoneClickedHandler) : RecyclerView.Adapter<ZonesAdapter.ViewHolder>() {

    var light: Light? = light
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var zones: Zones = zones
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var selectedZones: Set<Int> = selectedZones
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.zone_list_item, parent, false))
    }

    override fun getItemCount(): Int {
        return zones.count
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(light, position, zones.colors[position], selectedZones.contains(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.ledIcon)

        fun bind(light: Light?, zone: Int, hsbk: HSBK, selected: Boolean) {
            if (hsbk.brightness.toUnsignedFloat > 0.005 && light?.power == PowerState.ON) {
                icon.setImageResource(R.drawable.ic_led_on)
            } else {
                icon.setImageResource(R.drawable.ic_led_off)
            }
            icon.setColorFilter(hsbk.toColorFullBrightness())
            if (selected) {
                icon.background = icon.resources.getDrawable(R.drawable.ic_selection)
            } else {
                icon.background = null
            }

            icon.setOnClickListener {
                clickHandler.zoneClicked(zone, !selected)
            }
        }

    }
}



