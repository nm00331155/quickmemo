package com.quickmemo.app.calendar

import android.content.Intent
import android.provider.CalendarContract

object CalendarHelper {
    fun createCalendarIntent(
        title: String,
        description: String = "",
        startTimeMillis: Long? = null,
        endTimeMillis: Long? = null,
    ): Intent {
        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            startTimeMillis?.let {
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it)
            }
            endTimeMillis?.let {
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it)
            }
        }
    }
}
