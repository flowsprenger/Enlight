package lf.wo.enlight.lifx.persistence

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "lights")
data class LightEntity(

        @PrimaryKey
        val id: Long,

        @ColumnInfo(name = "address")
        val address: String,

        @ColumnInfo(name = "last_seen_at")
        val lastSeenAt: Long,

        @ColumnInfo(name = "label")
        val label: String,

        @ColumnInfo(name = "hue")
        val hue: Int,

        @ColumnInfo(name = "saturation")
        val saturation: Int,

        @ColumnInfo(name = "brightness")
        val brightness: Int,

        @ColumnInfo(name = "kelvin")
        val kelvin: Int,

        @ColumnInfo(name = "power")
        val power: Int
)