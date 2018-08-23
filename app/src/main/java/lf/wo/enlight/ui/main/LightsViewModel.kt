/*

  Copyright (c) 2018 Florian Sprenger

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

 */

package lf.wo.enlight.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import lf.wo.enlight.lifx.livedata.LightLocationLiveData
import lf.wo.enlight.lifx.livedata.LightsLiveData
import javax.inject.Inject


class LightsViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    val lights = LightsLiveData(application.applicationContext)

    val locations = LightLocationLiveData(application.applicationContext)
}


