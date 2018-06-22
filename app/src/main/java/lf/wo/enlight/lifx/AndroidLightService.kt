package lf.wo.enlight.lifx

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import wo.lf.lifx.net.UdpTransport
import java.util.concurrent.TimeUnit
import android.arch.persistence.room.Room
import lf.wo.enlight.lifx.persistence.LightDatabase
import lf.wo.enlight.lifx.persistence.LightEntity
import wo.lf.lifx.api.*
import wo.lf.lifx.domain.HSBK
import wo.lf.lifx.domain.LifxMessagePayload
import wo.lf.lifx.domain.PowerState
import java.net.InetAddress


class AndroidLightService : Service(), IAndroidLightService, ILightsChangeDispatcher, ILightFactory {

    private var unbindTimeout: Disposable? = null

    private val binder = AndroidLightServiceBinder()

    private val listeners = mutableSetOf<ILightsAddedDispatcher>()

    val db by lazy {
        Room.databaseBuilder(this,
                LightDatabase::class.java, "database-name").build()
    }

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

    private lateinit var lightsById: MutableMap<Long, Light>

    override fun getLight(id: Long): Light? {
        return lightsById[id]
    }

    override fun onLightAdded(light: Light) {
        if (lightsById.containsKey(light.id)) {
            Log.w("AndroidLightService", "light added skipped already known : ${light.id}")
        } else {
            Log.w("AndroidLightService", "light added : ${light.id}")
            lightsById[light.id] = light
            lights += light
        }
    }

    override fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {
        Log.w("AndroidLightService", "light ${light.id} changed $property from $oldValue to $newValue")
    }

    override fun create(id: Long, source: ILightSource<LifxMessage<LifxMessagePayload>>, changeDispatcher: ILightsChangeDispatcher): Light {
        return lightsById[id] ?: Light(id, source, changeDispatcher)
    }

    val service = LightService(
            transportFactory = UdpTransport,
            changeDispatcher = this,
            lightFactory = this,
            ioScheduler = Schedulers.io(),
            observeScheduler = AndroidSchedulers.mainThread()
    )

    override fun onBind(intent: Intent?): IBinder {
        startService(intent)
        return binder.also {
            Log.w("AndroidLightService", "service started")
            async {
                val lightEntities = db.lightDao().getAll()
                val lights = lightEntities.map { Light(it.id, service, this, it.toDefaultState()) }
                lightsById = lights.associateBy { it.id }.toMutableMap()
                this.lights = lights
                service.start()
            }
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
            db.lightDao().insertAll(lights.map { it.toEntity() })
            lights = listOf()
            db.close()
            stopSelf()
        }
        return true
    }

    override fun onRebind(intent: Intent?) {
        unbindTimeout?.dispose()
        super.onRebind(intent)
    }
}

private fun LightEntity.toDefaultState(): DefaultLightState {
    return DefaultLightState(
            address = InetAddress.getByName(address),
            lastSeenAt = lastSeenAt,
            reachable = false,
            label = label,
            color = HSBK(hue.toShort(), saturation.toShort(), brightness.toShort(), kelvin.toShort()),
            power = if (power == 1) PowerState.ON else PowerState.OFF
    )
}

private fun Light.toEntity(): LightEntity {
    return LightEntity(
            id = id,
            address = address.hostAddress,
            lastSeenAt = lastSeenAt,
            label = label,
            hue = color.hue.toInt() and 0xFFFF,
            saturation = color.saturation.toInt() and 0xFFFF,
            brightness = color.brightness.toInt() and 0xFFFF,
            kelvin = color.kelvin.toInt() and 0xFFFF,
            power = power.ordinal
    )
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

fun async(block: () -> Unit): Disposable {
    return Completable.create { block() }.subscribeOn(Schedulers.io()).subscribe()
}