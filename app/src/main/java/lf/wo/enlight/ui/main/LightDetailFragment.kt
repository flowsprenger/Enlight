/*

  Copyright (c) 2018 Florian Sprenger

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

 */

package lf.wo.enlight.ui.main

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.include_light_controls.*
import kotlinx.android.synthetic.main.light_detail_fragment.*
import lf.wo.enlight.R
import lf.wo.enlight.kotlin.*
import lf.wo.enlight.viewmodel.LightDetailViewModel
import lf.wo.enlight.viewmodel.ZoneSelectionMode
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import wo.lf.lifx.api.*
import wo.lf.lifx.domain.HSBK
import wo.lf.lifx.domain.PowerState
import wo.lf.lifx.extensions.copy
import wo.lf.lifx.extensions.fireAndForget

class LightDetailFragment : Fragment(), IZoneClickedHandler {
    override fun zoneClicked(zone: Int, selected: Boolean) {
        lightDetailViewModel.setSelection(zone, selected)
    }

    private val lightDetailViewModel: LightDetailViewModel by viewModel { parametersOf(LightDetailFragmentArgs.fromBundle(arguments).lightId) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              syavedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.light_detail_fragment, container, false)
    }

    private var inUpdate: Boolean = false

    private lateinit var adapter: ZonesAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        lightDetailViewModel.settings.observe(this, Observer { settings ->
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

        lightDetailViewModel.light.observe(this, Observer<Light> { light: Light? ->
            (activity as AppCompatActivity).supportActionBar?.title = light?.label

            if (light == null) {

                ledState.adapter = null

            } else {
                inUpdate = true

                if (light.productInfo.hasTileSupport) {
                    tileGroup.visibility = View.VISIBLE
                } else {
                    tileGroup.visibility = View.GONE
                }

                if (light.productInfo.hasMultiZoneSupport) {
                    zoneGroup.visibility = View.VISIBLE
                } else {
                    zoneGroup.visibility = View.GONE
                }

                if (lightName.text.toString() != light.label) {
                    lightName.setText(light.label)
                }

                if (locationName.text.toString() != light.location()?.name) {
                    locationName.setText(light.location()?.name)
                }

                if (groupName.text.toString() != light.group()?.name) {
                    groupName.setText(light.group()?.name)
                }

                if (light.on != powerSwitch.isChecked) {
                    powerSwitch.isChecked = light.on
                }

                if (light.color.brightness.toPercent != brightnessBar.progress) {
                    brightnessBar.progress = light.color.brightness.toPercent
                }

                val displayedColor = if (lightDetailViewModel.settings.value?.selectionMode == ZoneSelectionMode.INDIVIDUAL) {
                    light.zones.colors[lightDetailViewModel.settings.value?.selectedZones?.sorted()?.first()
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

        (activity as AppCompatActivity).supportActionBar?.let { actionBar ->
            actionBar.title = lightDetailViewModel.light.value?.label
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(true)
        }

        powerSwitch.setOnCheckedChangeListener { _, isChecked ->
            lightDetailViewModel.light.value?.let { light ->
                if (light.on != isChecked) {
                    LightSetPowerCommand.create(light, isChecked, 50).fireAndForget()
                }
            }
        }

        brightnessBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                lightDetailViewModel.light.value?.let { light ->
                    if (!inUpdate) {
                        val brightness = progress.toUnsignedShort
                        if (lightDetailViewModel.settings.value?.selectionMode == ZoneSelectionMode.INDIVIDUAL) {
                            lightDetailViewModel.settings.value?.selectedZones?.let {
                                it.sorted().fold(listOf<Pair<IntRange, HSBK>>()) { acc, i ->
                                    val last = acc.lastOrNull()
                                    val color = light.zones.colors[i]
                                    if (last != null && last.first.last + 1 == i && last.second == color) {
                                        acc.dropLast(1).plus(Pair(IntRange(last.first.start, i), last.second))
                                    } else {
                                        acc.plus(Pair(IntRange(i, i), color))
                                    }
                                }.forEach {
                                    MultiZoneSetColorZonesCommand.create(light, it.second.copy(brightness = brightness), it.first.start, it.first.endInclusive, 50).fireAndForget()
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
            lightDetailViewModel.light.value?.let { light ->
                if (!inUpdate) {
                    val hsv = floatArrayOf(0f, 0f, 0f)
                    Color.colorToHSV(color, hsv)
                    Log.w("color", "onChanged : $color $hsv")
                    val color = light.color.copy(hue = hsv[0].toDegreeShort, saturation = hsv[1].toUnsignedShort, brightness = hsv[2].toUnsignedShort)
                    if (lightDetailViewModel.settings.value?.selectionMode == ZoneSelectionMode.INDIVIDUAL) {
                        lightDetailViewModel.settings.value?.selectedZones?.let {
                            it.sorted().fold(listOf<Pair<IntRange, HSBK>>()) { acc, i ->
                                val last = acc.lastOrNull()
                                if (last != null && last.first.last + 1 == i) {
                                    acc.dropLast(1).plus(Pair(IntRange(last.first.start, i), last.second))
                                } else {
                                    acc.plus(Pair(IntRange(i, i), color))
                                }
                            }.forEach {
                                MultiZoneSetColorZonesCommand.create(light, color, it.first.start, it.first.endInclusive, 50).fireAndForget()
                            }
                        }
                    } else {
                        LightSetColorCommand.create(light, color, 1000).fireAndForget()
                    }
                }
            }
        }

        selectionMode.setOnClickListener {
            if (lightDetailViewModel.settings.value?.selectionMode == ZoneSelectionMode.ALL) {
                lightDetailViewModel.setZoneSelectionMode(ZoneSelectionMode.INDIVIDUAL)
            } else {
                lightDetailViewModel.setZoneSelectionMode(ZoneSelectionMode.ALL)
            }
        }

        lightName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!inUpdate) {
                    lightDetailViewModel.light.value?.let { light ->
                        DeviceSetLabelCommand.create(light, s.toString()).fireAndForget()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })

        tileColorButton.setOnClickListener {
            lightDetailViewModel.light.value?.tile()?.let { tile ->
                val colors = List(64) { HSBK((it * (Short.MAX_VALUE.toInt() * 2) / 64).toShort(), (Short.MAX_VALUE.toInt() * 2).toShort(), Short.MAX_VALUE, 0) }

                for (index in 0 until tile.chain.size) {
                    TileSetTileState64Command.create(
                            tileService = tile.tileService,
                            light = tile.light,
                            tileIndex = index,
                            colors = colors
                    ).fireAndForget()
                }
            }
        }

        locationName.setOnClickListener {
            val action = LightDetailFragmentDirections.actionLightDetailFragmentToLocationGroupFragment(lightDetailViewModel.id)
            view?.findNavController()?.navigate(action)
        }
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



