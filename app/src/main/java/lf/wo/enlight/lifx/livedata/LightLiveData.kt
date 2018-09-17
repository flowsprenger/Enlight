/*

  Copyright (c) 2018 Florian Sprenger

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

 */

package lf.wo.enlight.lifx.livedata

import android.content.Context
import lf.wo.enlight.lifx.IAndroidLightService
import lf.wo.enlight.lifx.ILightsChangedDispatcher
import wo.lf.lifx.api.ILightChangeDispatcher
import wo.lf.lifx.api.Light
import wo.lf.lifx.api.LightProperty

class LightLiveData(val id: Long, context: Context) : AbstractAndroidLightServiceLiveData<Light>(context), ILightChangeDispatcher {

    inner class AwaitLight : ILightsChangedDispatcher {
        override fun groupsLocationChanged() {

        }

        override fun lightChanged(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {

        }

        override fun lightsChanged(value: List<Light>) {
            value.firstOrNull { it.id == id }?.let { light ->
                service?.removeChangeListener(this)
                bindLight(light)
                postValue(light)
            }
        }
    }

    val awaitLightListener by lazy {
        AwaitLight()
    }


    override fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {
        postValue(light)
    }

    override fun unbindService(service: IAndroidLightService?) {
        val light = value
        if (light != null) {
            unbindLight(light)
        } else {
            service?.removeChangeListener(awaitLightListener)
        }
    }

    override fun bindService(service: IAndroidLightService) {
        val light = service.getLight(id)
        if (light != null) {
            bindLight(light)
            postValue(light)
        } else {
            postValue(null)
            service.addChangeListener(awaitLightListener)
        }
    }

    private fun unbindLight(light: Light) {
        light.removeChangeDispatcher(this)
    }

    private fun bindLight(light: Light) {
        light.addChangeDispatcher(this)
    }
}