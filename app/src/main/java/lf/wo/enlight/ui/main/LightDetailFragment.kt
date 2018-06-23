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
import lf.wo.enlight.kotlin.*
import wo.lf.lifx.api.Light
import wo.lf.lifx.api.LightSetColorCommand
import wo.lf.lifx.api.LightSetPowerCommand
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

                if (light.color.brightness.toPercent != brightnessBar.progress) {
                    brightnessBar.progress = light.color.brightness.toPercent
                }

                val rgb = Color.HSVToColor(floatArrayOf(light.color.hue.degreesToFloat, light.color.saturation.toUnsignedFloat, light.color.brightness.toUnsignedFloat))
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
                    LightSetColorCommand.create(light, light.color.copy(brightness = progress.toUnsignedShort), 1000).fireAndForget()
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
                LightSetColorCommand.create(light, light.color.copy(hue = hsv[0].toDegreeShort, saturation = hsv[1].toUnsignedShort, brightness = hsv[2].toUnsignedShort), 1000).fireAndForget()
            }
        }
    }
}



