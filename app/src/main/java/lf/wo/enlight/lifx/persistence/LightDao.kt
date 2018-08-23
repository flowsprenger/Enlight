/*

  Copyright (c) 2018 Florian Sprenger

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

 */

package lf.wo.enlight.lifx.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class LightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(users: List<LightEntity>)

    @Query("SELECT * FROM lights")
    abstract fun getAll(): List<LightEntity>
}