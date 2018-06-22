package lf.wo.enlight.lifx.persistence

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query


@Dao
abstract class LightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(users: List<LightEntity>)

    @Query("SELECT * FROM lights")
    abstract fun getAll(): List<LightEntity>
}