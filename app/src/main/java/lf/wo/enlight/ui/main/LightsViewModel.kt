package lf.wo.enlight.ui.main

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import lf.wo.enlight.AndroidLightService
import lf.wo.enlight.IAndroidLightService
import lf.wo.enlight.ILightsAddedDispatcher
import wo.lf.lifx.api.Light
import javax.inject.Inject


class LightsViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    val lights = LightsLiveData(application.applicationContext)
}

class LightsLiveData(private val context: Context) : LiveData<List<Light>>(), ILightsAddedDispatcher {
    override fun lightsChanged(value: List<Light>) {
        postValue(value)
    }

    private var service: IAndroidLightService? = null
    private var bound = false

    override fun onActive() {
        if (!bound) {
            context.bindService(Intent(context, AndroidLightService::class.java), connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onInactive() {
        if (bound) {
            context.unbindService(connection)
            bound = false
        }
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName,
                                        serviceBinder: IBinder) {
            val binder = serviceBinder as AndroidLightService.AndroidLightServiceBinder
            this@LightsLiveData.service = binder.service.apply {
                addChangeListener(this@LightsLiveData)
                value = lights
            }
            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }
}