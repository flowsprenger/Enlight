/*

  Copyright (c) 2018 Florian Sprenger

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

 */

package lf.wo.enlight.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import lf.wo.enlight.lifx.livedata.LightLiveData
import javax.inject.Inject

class LightDetailViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    val light = LightLiveData(application.applicationContext)

    private val mutableSettings = MutableLiveData<LightDetailViewSettings>().apply { value = LightDetailViewSettings(ZoneSelectionMode.ALL, setOf()) }
    val settings = mutableSettings

    fun initialize(lightId: Long) {
        light.id = lightId
    }

    fun setZoneSelectionMode(mode: ZoneSelectionMode) {
        mutableSettings.value = settings.value?.copy(selectionMode = mode)
    }

    fun setSelection(zone: Int, selected: Boolean) {
        mutableSettings.value = settings.value?.let { it.copy(selectedZones = if (selected) it.selectedZones.plus(zone) else it.selectedZones.minus(zone)) }
    }
}

enum class ZoneSelectionMode {
    ALL,
    INDIVIDUAL
}

data class LightDetailViewSettings(val selectionMode: ZoneSelectionMode, val selectedZones: Set<Int>)