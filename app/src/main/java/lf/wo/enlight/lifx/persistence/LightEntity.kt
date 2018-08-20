package lf.wo.enlight.lifx.persistence

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

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
        val power: Int,

        @Embedded(prefix = "location")
        val location: LocationOrGroupEntity,

        @Embedded(prefix = "group")
        val group: LocationOrGroupEntity,

        @Embedded(prefix = "hostFirmware")
        val hostFirmware: FirmwareVersionEntity,

        @Embedded(prefix = "wifiFirmware")
        val wifiFirmware: FirmwareVersionEntity,

        @Embedded
        val productVersion: ProductVersionEntity,

        @ColumnInfo(name = "infrared_brightness")
        val infraredBrightness: Short,

        @ColumnInfo(name = "zones")
        val zones: String
)

data class LocationOrGroupEntity(
        @ColumnInfo(name = "id")
        val id: ByteArray,

        @ColumnInfo(name = "label")
        val label: String,

        @ColumnInfo(name = "updated_at")
        val updatedAt: Long
)

data class FirmwareVersionEntity(
        @ColumnInfo(name = "build")
        val build: Long,
        @ColumnInfo(name = "version")
        val version: Int
)

data class ProductVersionEntity(
        @ColumnInfo(name = "vendor_id")
        val vendorId: Int,
        @ColumnInfo(name = "product_id")
        val productId: Int,
        @ColumnInfo(name = "product_version")
        val version: Int
)