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


