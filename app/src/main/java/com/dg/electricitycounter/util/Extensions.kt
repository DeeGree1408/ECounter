package com.dg.electricitycounter.util

import java.text.SimpleDateFormat
import java.util.*

fun Long.formatToDisplay(): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Double.format(decimals: Int = 2): String {
    return String.format("%.${decimals}f", this)
}
