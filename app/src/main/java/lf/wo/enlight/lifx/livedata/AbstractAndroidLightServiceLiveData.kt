/*

  Copyright (c) 2018 Florian Sprenger

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

 */

package lf.wo.enlight.lifx.livedata

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import lf.wo.enlight.lifx.AndroidLightService
import lf.wo.enlight.lifx.IAndroidLightService

abstract class AbstractAndroidLightServiceLiveData<T>(private val context: Context) : LiveData<T>() {

    protected var service: IAndroidLightService? = null
    protected var bound = false

    override fun onActive() {
        if (!bound) {
            context.bindService(Intent(context, AndroidLightService::class.java), connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onInactive() {
        if (bound) {
            unbindService(service)
            context.unbindService(connection)
            bound = false
        }
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName,
                                        serviceBinder: IBinder) {
            val binder = serviceBinder as AndroidLightService.AndroidLightServiceBinder
            this@AbstractAndroidLightServiceLiveData.service = binder.service.apply {
                bindService(this)
            }
            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }

    abstract fun unbindService(service: IAndroidLightService?)
    abstract fun bindService(service: IAndroidLightService)
}