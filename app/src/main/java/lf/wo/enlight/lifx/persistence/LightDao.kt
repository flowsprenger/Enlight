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