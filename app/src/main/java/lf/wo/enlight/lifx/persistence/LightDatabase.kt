package lf.wo.enlight.lifx.persistence

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [(LightEntity::class)], version = 1)
abstract class LightDatabase : RoomDatabase() {
    abstract fun lightDao(): LightDao
}