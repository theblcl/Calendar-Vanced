// Updated CalendarEvent.kt to include calendar info
package com.example.calendarapp.data

import java.time.LocalDateTime

data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String = "",
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val isAllDay: Boolean = false,
    val location: String = "",
    val recurrence: String? = null,
    val calendarId: String = "", // NEW: Which calendar this event belongs to
    val calendarName: String = "", // NEW: Human-readable calendar name
    val calendarColor: String = "#2196F3" // NEW: Calendar color for UI
)

// New data class for calendar information
data class CalendarInfo(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String = "",
    val color: String = "#2196F3",
    val url: String,
    val isEnabled: Boolean = true
)