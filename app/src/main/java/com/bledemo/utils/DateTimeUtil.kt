package com.bledemo.utils

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtil {
    const val FULL_DATE_FORMAT = "dd/MM/yyyy HH:mm:ss"

    fun millisToFormat(millis: Long, format: String?, tz: TimeZone?): String {
        var milli = millis
        if (milli < 1000000000000L) {
            milli *= 1000
        }
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = milli
        val sdf = SimpleDateFormat(format, Locale.ENGLISH)
        sdf.timeZone = tz
        return sdf.format(cal.time)
    }
}