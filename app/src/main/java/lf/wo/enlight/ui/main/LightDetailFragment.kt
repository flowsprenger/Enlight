package lf.wo.enlight.ui.main

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import kotlinx.android.synthetic.main.light_detail_fragment.*
import lf.wo.enlight.R
import wo.lf.lifx.api.Light
import wo.lf.lifx.api.LightSetColorCommand
import wo.lf.lifx.api.LightSetPowerCommand
import wo.lf.lifx.api.LightSetWaveformOptionalCommand
import wo.lf.lifx.domain.HSBK
import wo.lf.lifx.domain.PowerState
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(LightDetailViewModel::class.java)

        val lightId = LightDetailFragmentArgs.fromBundle(arguments).lightId.toLong()
        viewModel.initialize(lightId)


        viewModel.light.observe(this, Observer<Light> { light: Light? ->
            if (light == null) {

            } else {
                if (lightName.text.toString() != light.label) {
                    lightName.setText(light.label)
                }

                if (light.on != powerSwitch.isChecked) {
                    powerSwitch.isChecked = light.on
                }

                if (light.color.brightness.sliderScale != brightnessBar.progress) {
                    brightnessBar.progress = light.color.brightness.sliderScale
                }

                val rgb = Color.HSVToColor(floatArrayOf(light.color.hue.degreesAsFloat, light.color.saturation.asFloat, light.color.brightness.asFloat))
                if (rgb != colorPicker.color) {
                    Log.w("color", "updating to : $rgb ${light.color}")
                    colorPicker.color = rgb
                }
            }
        })

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
                    LightSetColorCommand.create(light, light.color.copy(brightness = progress.asProtocolShort), 1000).fireAndForget()
                    // TODO debounce, use waveform optional (when appropriate?)
                    //LightSetWaveformOptionalCommand.create(light, )
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
                LightSetColorCommand.create(light, light.color.copy(hue = hsv[0].asDegreeProtocolShort, saturation = hsv[1].asProtocolShort, brightness = hsv[2].asProtocolShort), 1000).fireAndForget()
            }
        }
    }
}

private val Short.asFloat: Float
    get() {
        return (this.toInt() and 0xFFFF).toFloat() / 65_535
    }

private val Short.degreesAsFloat: Float
    get() {
        return (this.toInt() and 0xFFFF).toFloat() / 65_535 * 360
    }

private fun HSBK.copy(hue: Short? = null, saturation: Short? = null, brightness: Short? = null, kelvin: Short? = null): HSBK {
    return HSBK(hue = hue ?: this.hue, saturation = saturation
            ?: this.saturation, brightness = brightness ?: this.brightness, kelvin = kelvin
            ?: this.kelvin)
}

private val Float.asDegreeProtocolShort: Short
    get() {
        return (this * 65_535 / 360).toInt().toShort()
    }

private val Float.asProtocolShort: Short
    get() {
        return (this * 65_535).toInt().toShort()
    }

private val Int.asProtocolShort: Short
    get() {
        return (this * 65_535 / 100).toShort()
    }

private val Short.sliderScale: Int
    get() {
        return (this.toInt() and 0xFFFF) * 100 / 65_535
    }

private val Light.on: Boolean
    get() = power == PowerState.ON
