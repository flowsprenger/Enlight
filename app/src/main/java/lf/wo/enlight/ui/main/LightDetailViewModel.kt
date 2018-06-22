package lf.wo.enlight.ui.main

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import lf.wo.enlight.lifx.AndroidLightService
import lf.wo.enlight.lifx.IAndroidLightService
import lf.wo.enlight.lifx.ILightsAddedDispatcher
import wo.lf.lifx.api.ILightChangeDispatcher
import wo.lf.lifx.api.Light
import wo.lf.lifx.api.LightProperty
import javax.inject.Inject

class LightDetailViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    val light = LightLiveData(application.applicationContext)

    fun initialize(lightId: Long) {
        light.id = lightId
    }
}

class LightLiveData(private val context: Context) : LiveData<Light>(), ILightChangeDispatcher, ILightsAddedDispatcher {
    override fun lightsChanged(value: List<Light>) {
        value.firstOrNull { it.id == id }?.let { light ->
            service?.removeChangeListener(this)
            bindLight(light)
            postValue(light)
        }
    }

    override fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {
        postValue(light)
    }

    private var service: IAndroidLightService? = null
    private var bound = false

    var id: Long? = null
        set(value) {
            if (bound && id != value) {
                throw IllegalAccessError("can only set before the livedata is bound")
            }
            field = value
        }

    override fun onActive() {
        if (!bound) {
            context.bindService(Intent(context, AndroidLightService::class.java), connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onInactive() {
        if (bound) {
            val light = value
            if (light != null) {
                unbindLight(light)
            } else {
                service?.removeChangeListener(this)
            }
            context.unbindService(connection)
            bound = false
        }
    }

    private fun unbindLight(light: Light) {
        light.removeChangeDispatcher(this)
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName,
                                        serviceBinder: IBinder) {
            val binder = serviceBinder as AndroidLightService.AndroidLightServiceBinder
            this@LightLiveData.service = binder.service.apply {
                id?.let {
                    val light = getLight(it)
                    if (light != null) {
                        bindLight(light)
                        postValue(light)
                    } else {
                        postValue(null)
                        addChangeListener(this@LightLiveData)
                    }
                }
            }
            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }

    private fun bindLight(light: Light) {
        light.addChangeDispatcher(this)
    }
}