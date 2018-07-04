package lf.wo.enlight.kotlin

import android.graphics.Color
import wo.lf.lifx.api.Light
import wo.lf.lifx.domain.HSBK
import wo.lf.lifx.domain.PowerState

fun HSBK.toColor(): Int {
    return Color.HSVToColor(floatArrayOf(hue.degreesToFloat, saturation.toUnsignedFloat, brightness.toUnsignedFloat))
}

val Float.toDegreeShort: Short
    get() {
        return (this * 65_535 / 360).toInt().toShort()
    }

val Float.toUnsignedShort: Short
    get() {
        return (this * 65_535).toInt().toShort()
    }

val Int.toUnsignedShort: Short
    get() {
        return (this * 65_535 / 100).toShort()
    }

val Short.toUnsignedFloat: Float
    get() {
        return (this.toInt() and 0xFFFF).toFloat() / 65_535
    }

val Short.degreesToFloat: Float
    get() {
        return (this.toInt() and 0xFFFF).toFloat() / 65_535 * 360
    }



val Short.toPercent: Int
    get() {
        return (this.toInt() and 0xFFFF) * 100 / 65_535
    }

val Light.on: Boolean
    get() = power == PowerState.ON