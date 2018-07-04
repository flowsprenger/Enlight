package lf.wo.enlight.ui.main

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
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

class LightDetailFragment : Fragment() {

    companion object {
        fun newInstance() = LightDetailFragment()
    }

    private lateinit var viewModel: LightDetailViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              syavedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.light_detail_fragment, container, false)
    }

    private var inUpdate: Boolean = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(LightDetailViewModel::class.java)

        val lightId = LightDetailFragmentArgs.fromBundle(arguments).lightId.toLong()
        viewModel.initialize(lightId)


        viewModel.light.observe(this, Observer<Light> { light: Light? ->
            if (light == null) {

                ledState.adapter = null

            } else {
                inUpdate = true

                if (light.productInfo.hasMultiZoneSupport) {
                    zoneGroup.visibility = View.GONE
                } else {
                    zoneGroup.visibility = View.VISIBLE
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

                val rgb = Color.HSVToColor(floatArrayOf(light.color.hue.degreesToFloat, light.color.saturation.toUnsignedFloat, light.color.brightness.toUnsignedFloat))
                if (rgb != colorPicker.color) {
                    Log.w("color", "updating to : $rgb ${light.color}")
                    colorPicker.color = rgb
                }

                ledState.adapter = ZonesAdapter(light, light.zones) // Zones(20, List(20) { light.color })) //)

                inUpdate = false
            }
        })

        ledState.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        ledState.adapter = ZonesAdapter(viewModel.light.value, Zones(0, listOf()))
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
                        LightSetBrightness.create(light, progress.toUnsignedShort, 1000).fireAndForget()
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        colorPicker.setOnColorChangedListener { color ->
            val hsv = floatArrayOf(0f, 0f, 0f)
            Color.colorToHSV(color, hsv)
            Log.w("color", "onChanged : $color $hsv")
            viewModel.light.value?.let { light ->
                if (!inUpdate) {
                    LightSetColorCommand.create(light, light.color.copy(hue = hsv[0].toDegreeShort, saturation = hsv[1].toUnsignedShort, brightness = hsv[2].toUnsignedShort), 1000).fireAndForget()
                }
            }
        }
    }
}

class ZonesAdapter(light: Light?, zones: Zones) : RecyclerView.Adapter<ZonesAdapter.ViewHolder>() {

    var light: Light? = light
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var zones: Zones = zones
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.zone_list_item, parent, false))
    }

    override fun getItemCount(): Int {
        return zones.count
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(light, zones.colors[position])
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.ledIcon)

        fun bind(light: Light?, hsbk: HSBK) {
            if (hsbk.brightness.toUnsignedFloat > 0.005 && light?.power == PowerState.ON) {
                icon.setImageResource(R.drawable.ic_led_on)
            } else {
                icon.setImageResource(R.drawable.ic_led_off)
            }
            icon.setColorFilter(hsbk.toColor())
        }

    }
}



