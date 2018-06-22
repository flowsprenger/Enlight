package lf.wo.enlight.lifx.persistence

import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.Database


@Database(entities = [(LightEntity::class)], version = 1)
abstract class LightDatabase : RoomDatabase() {
    abstract fun lightDao(): LightDao
}