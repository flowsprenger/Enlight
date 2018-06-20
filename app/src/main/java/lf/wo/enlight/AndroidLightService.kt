package lf.wo.enlight

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import wo.lf.lifx.api.ILightsChangeDispatcher
import wo.lf.lifx.api.Light
import wo.lf.lifx.api.LightProperty
import wo.lf.lifx.api.LightService
import wo.lf.lifx.net.UdpTransport
import java.util.concurrent.TimeUnit

class AndroidLightService : Service(), IAndroidLightService {

    private var unbindTimeout: Disposable? = null

    private val binder = AndroidLightServiceBinder()

    private val listeners = mutableSetOf<ILightsAddedDispatcher>()

    override fun addChangeListener(listener: ILightsAddedDispatcher): Boolean {
        return listeners.add(listener)
    }

    override fun removeChangeListener(listener: ILightsAddedDispatcher): Boolean {
        return listeners.remove(listener)
    }

    override var lights = listOf<Light>()
        private set(value) {
            field = value
            listeners.forEach { it.lightsChanged(value) }
        }

    override fun getLight(id: Long): Light? {
        return lights.firstOrNull { it.id == id }
    }

    val service = LightService(transportFactory = UdpTransport, changeDispatcher = object : ILightsChangeDispatcher {
        override fun onLightAdded(light: Light) {
            Log.w("AndroidLightService", "light added : ${light.id}")
            lights += light
        }

        override fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {
            Log.w("AndroidLightService", "light ${light.id} changed $property from $oldValue to $newValue")
        }
    }, ioScheduler = Schedulers.io(), observeScheduler = AndroidSchedulers.mainThread())

    override fun onBind(intent: Intent?): IBinder {
        startService(intent)
        return binder.also {
            Log.w("AndroidLightService", "service started")
            service.start()
        }
    }

    inner class AndroidLightServiceBinder : Binder() {
        val service: IAndroidLightService
            get() = this@AndroidLightService
    }

    override fun onUnbind(intent: Intent?): Boolean {
        unbindTimeout = Completable.timer(15L, TimeUnit.SECONDS).subscribe {
            Log.w("AndroidLightService", "service stopped")
            service.stop()
            lights = listOf()
            stopSelf()
        }
        return true
    }

    override fun onRebind(intent: Intent?) {
        unbindTimeout?.dispose()
        super.onRebind(intent)
    }
}

interface IAndroidLightService {
    fun addChangeListener(listener: ILightsAddedDispatcher): Boolean
    fun removeChangeListener(listener: ILightsAddedDispatcher): Boolean
    val lights: List<Light>
    fun getLight(id: Long): Light?

}

interface ILightsAddedDispatcher {
    fun lightsChanged(value: List<Light>)
}
