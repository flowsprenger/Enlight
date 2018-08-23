/*

  Copyright (c) 2018 Florian Sprenger

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

 */

package lf.wo.enlight.ui.main

import androidx.lifecycle.ViewModel
import javax.inject.Inject

data class MainParameters(val text: String)

class MainViewModel @Inject constructor(): ViewModel() {
}
