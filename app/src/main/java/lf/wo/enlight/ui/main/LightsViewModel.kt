package lf.wo.enlight.ui.main

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import wo.lf.lifx.api.*
import wo.lf.lifx.extensions.fireAndForget
import wo.lf.lifx.net.UdpTransport
import javax.inject.Inject

class LightsViewModel  @Inject constructor(): ViewModel() {

    private val mutableLights = MutableLiveData<List<Light>>().apply { value = listOf() }
    val lights: LiveData<List<Light>> = mutableLights

    val service: LightService

    init {

        service = LightService(UdpTransport, object : ILightChangeDispatcher {
            override fun onLightAdded(light: Light) {
                Log.w("MainActivity", "light added : ${light.id}")
                LightSetPowerCommand.create(light, true, 10000).fireAndForget()
                mutableLights.value = (mutableLights.value ?: listOf()) + light
            }

            override fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {
                Log.w("MainActivity", "light ${light.id} changed $property from $oldValue to $newValue")
            }
        }, Schedulers.io(), AndroidSchedulers.mainThread()).apply { start() }


    }

    override fun onCleared() {
        service.stop()
        super.onCleared()
    }
}