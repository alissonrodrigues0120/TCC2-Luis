package com.project.ui.home

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    fun formatMillisToDateString(millis: Long): String {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        return format.format(Date(millis))
    }

    fun parseDateStringToMillis(dateStr: String): Long? {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        return try {
            format.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    fun calculateAge(birthDateMillis: Long): Int {
        val dob = Calendar.getInstance().apply { timeInMillis = birthDateMillis }
        val today = Calendar.getInstance()

        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return if (age < 0) 0 else age
    }
}
