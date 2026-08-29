package com.dhan.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {
    /** [startOfMonthMillis, startOfNextMonthMillis) for the month containing [atMillis]. */
    fun monthRange(atMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = atMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }

    fun monthKey(atMillis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(atMillis)

    fun monthLabel(atMillis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("MMMM", Locale.getDefault()).format(atMillis)

    fun daysRemainingInMonth(atMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = atMillis }
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val last = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (last - today).coerceAtLeast(0)
    }

    fun dayGroupLabel(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        return when {
            isSameDay(cal, today) -> "Today · " + SimpleDateFormat("MMM d", Locale.getDefault()).format(millis)
            isSameDay(cal, yesterday) -> "Yesterday"
            else -> SimpleDateFormat("EEE · MMM d", Locale.getDefault()).format(millis)
        }
    }

    fun timeLabel(millis: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(millis)

    fun dateLabel(millis: Long): String = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(millis)

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
