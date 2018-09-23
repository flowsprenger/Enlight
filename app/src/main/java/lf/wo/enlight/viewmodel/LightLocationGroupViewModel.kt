package lf.wo.enlight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import lf.wo.enlight.lifx.livedata.LightLiveData
import lf.wo.enlight.lifx.livedata.LightLocationLiveData

class LightLocationGroupViewModel constructor(val id: Long, application: Application) : AndroidViewModel(application) {
    val light = LightLiveData(id, application.applicationContext)

    val locations = LightLocationLiveData(application.applicationContext)

}