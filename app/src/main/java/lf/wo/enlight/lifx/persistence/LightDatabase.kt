/*

  Copyright (c) 2018 Florian Sprenger

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

 */

package lf.wo.enlight.lifx.persistence

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [(LightEntity::class)], version = 2)
abstract class LightDatabase : RoomDatabase() {
    abstract fun lightDao(): LightDao
}