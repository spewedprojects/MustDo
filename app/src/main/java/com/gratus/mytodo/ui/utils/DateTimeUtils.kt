package com.gratus.mytodo.ui.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Thread-safe utility helper for handling all date and time formatting patterns used in the app.
 */
object DateTimeUtils {
    
    private val dbFormatter = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    private val mainHeaderFormatter = ThreadLocal.withInitial { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    private val homeDateLabelFormatter = ThreadLocal.withInitial { SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()) }
    private val historyGroupFormatter = ThreadLocal.withInitial { SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()) }
    private val statsLabelFormatter = ThreadLocal.withInitial { SimpleDateFormat("MM-dd", Locale.getDefault()) }
    private val addDialogDayFormatter = ThreadLocal.withInitial { SimpleDateFormat("EEE dd", Locale.getDefault()) }
    private val alarmTimeFormatter = ThreadLocal.withInitial { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    private val alarmDateFormatter = ThreadLocal.withInitial { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
    private val monthYearFormatter = ThreadLocal.withInitial { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    private val db get() = dbFormatter.get()!!
    private val mainHeader get() = mainHeaderFormatter.get()!!
    private val homeDateLabel get() = homeDateLabelFormatter.get()!!
    private val historyGroup get() = historyGroupFormatter.get()!!
    private val statsLabel get() = statsLabelFormatter.get()!!
    private val addDialogDay get() = addDialogDayFormatter.get()!!
    private val alarmTime get() = alarmTimeFormatter.get()!!
    private val alarmDate get() = alarmDateFormatter.get()!!
    private val monthYear get() = monthYearFormatter.get()!!

    fun formatDbDate(calendar: Calendar): String = db.format(calendar.time)
    fun formatDbDate(timeMillis: Long): String = db.format(Date(timeMillis))
    fun parseDbDate(dateStr: String): Date? = try { db.parse(dateStr) } catch (e: Exception) { null }

    fun formatMainHeader(calendar: Calendar): String = mainHeader.format(calendar.time)
    fun formatMainHeader(date: Date): String = mainHeader.format(date)
    
    fun formatHomeDateLabel(calendar: Calendar): String = homeDateLabel.format(calendar.time)
    fun formatHomeDateLabel(date: Date): String = homeDateLabel.format(date)
    
    fun formatHistoryGroup(date: Date): String = historyGroup.format(date)
    
    fun formatStatsLabel(calendar: Calendar): String = statsLabel.format(calendar.time)
    fun formatStatsLabel(date: Date): String = statsLabel.format(date)
    
    fun formatAddDialogDay(calendar: Calendar): String = addDialogDay.format(calendar.time)
    fun formatAddDialogDay(date: Date): String = addDialogDay.format(date)
    
    fun formatAlarmTime(context: android.content.Context?, timeMillis: Long): String {
        val is24 = try {
            context?.let { android.text.format.DateFormat.is24HourFormat(it) } ?: false
        } catch (e: Exception) {
            false
        }
        val pattern = if (is24) "HH:mm" else "hh:mm a"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }

    fun formatAlarmTime(timeMillis: Long): String = formatAlarmTime(null, timeMillis)

    fun formatAlarmDate(context: android.content.Context?, timeMillis: Long): String {
        val is24 = try {
            context?.let { android.text.format.DateFormat.is24HourFormat(it) } ?: false
        } catch (e: Exception) {
            false
        }
        val pattern = if (is24) "MMM dd, HH:mm" else "MMM dd, hh:mm a"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }

    fun formatAlarmDate(timeMillis: Long): String = formatAlarmDate(null, timeMillis)

    fun formatMonthYear(date: Date): String = monthYear.format(date)

    fun daysBetween(cal1: Calendar, cal2: Calendar): Int {
        val date1 = cal1.clone() as Calendar
        val date2 = cal2.clone() as Calendar
        
        date1.set(Calendar.HOUR_OF_DAY, 0)
        date1.set(Calendar.MINUTE, 0)
        date1.set(Calendar.SECOND, 0)
        date1.set(Calendar.MILLISECOND, 0)
        
        date2.set(Calendar.HOUR_OF_DAY, 0)
        date2.set(Calendar.MINUTE, 0)
        date2.set(Calendar.SECOND, 0)
        date2.set(Calendar.MILLISECOND, 0)
        
        val diff = date2.timeInMillis - date1.timeInMillis
        return (diff / (24 * 60 * 60 * 1000)).toInt()
    }

    fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
