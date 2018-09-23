/*

  Copyright (c) 2018 Florian Sprenger

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

 */

package lf.wo.enlight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import lf.wo.enlight.lifx.livedata.LightLiveData

class LightDetailViewModel constructor(val id: Long, application: Application) : AndroidViewModel(application) {
    val light = LightLiveData(id, application.applicationContext)

    private val mutableSettings = MutableLiveData<LightDetailViewSettings>().apply { value = LightDetailViewSettings(ZoneSelectionMode.ALL, setOf()) }
    val settings = mutableSettings

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