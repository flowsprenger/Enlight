/*

  Copyright (c) 2018 Florian Sprenger

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

 */

package lf.wo.enlight

import android.app.Application
import lf.wo.enlight.viewmodel.LightDetailViewModel
import lf.wo.enlight.viewmodel.LightsViewModel
import org.koin.android.ext.android.startKoin
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

class EnLightApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin(this, listOf(appModule))
    }

    val appModule = module {

        single<Application> { this@EnLightApp }

        viewModel { LightDetailViewModel(get()) }

        viewModel { LightsViewModel(get()) }

    }
}