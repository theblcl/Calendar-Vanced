package com.example.calendarapp.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

class AndroidCalendarRepository(private val context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver

    suspend fun getEvents(): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val events = mutableListOf<CalendarEvent>()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
            CalendarContract.Events.CALENDAR_COLOR
        )

        val selection = "${CalendarContract.Events.DELETED} != 1"
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                try {
                    val event = parseEventFromCursor(cursor)
                    events.add(event)
                } catch (e: Exception) {
                    println("Error parsing event: ${e.message}")
                }
            }
        }

        events
    }

    suspend fun addEvent(event: CalendarEvent) = withContext(Dispatchers.IO) {
        // Find the Android calendar ID based on the calendar name only
        val androidCalendarId = findCalendarIdByName(event.calendarName)

        println("=== Creating Android Calendar Event ===")
        println("Event: ${event.title}")
        println("Target Calendar Name: '${event.calendarName}'")
        println("Found Android Calendar ID: $androidCalendarId")

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, event.startTime.toEpochSecond(ZoneOffset.UTC) * 1000)
            put(CalendarContract.Events.DTEND, event.endTime.toEpochSecond(ZoneOffset.UTC) * 1000)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
            put(CalendarContract.Events.CALENDAR_ID, androidCalendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.ALL_DAY, if (event.isAllDay) 1 else 0)
        }

        val result = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        println("Event creation result: $result")
    }

    suspend fun updateEvent(event: CalendarEvent) = withContext(Dispatchers.IO) {
        // Find the Android calendar ID based on the calendar name only
        val androidCalendarId = findCalendarIdByName(event.calendarName)

        println("=== Updating Android Calendar Event ===")
        println("Event: ${event.title}")
        println("Event ID: ${event.id}")
        println("Target Calendar Name: '${event.calendarName}'")
        println("Found Android Calendar ID: $androidCalendarId")

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, event.startTime.toEpochSecond(ZoneOffset.UTC) * 1000)
            put(CalendarContract.Events.DTEND, event.endTime.toEpochSecond(ZoneOffset.UTC) * 1000)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
            put(CalendarContract.Events.CALENDAR_ID, androidCalendarId)
            put(CalendarContract.Events.ALL_DAY, if (event.isAllDay) 1 else 0)
        }

        val eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id.toLong())
        val result = contentResolver.update(eventUri, values, null, null)
        println("Event update result: $result rows affected")
    }

    suspend fun deleteEvent(eventId: String) = withContext(Dispatchers.IO) {
        println("=== Deleting Android Calendar Event ===")
        println("Event ID: $eventId")

        val eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId.toLong())
        val result = contentResolver.delete(eventUri, null, null)
        println("Event deletion result: $result rows affected")
    }

    suspend fun getCalendars(): List<CalendarInfo> = withContext(Dispatchers.IO) {
        val calendars = mutableListOf<CalendarInfo>()

        // Define a diverse color palette
        val colorPalette = listOf(
            "#E53E3E", // Red
            "#3182CE", // Blue
            "#38A169", // Green
            "#D69E2E", // Orange
            "#805AD5", // Purple
            "#E53E3E", // Pink
            "#319795", // Teal
            "#DD6B20", // Orange-red
            "#9F7AEA", // Light purple
            "#4FD1C7", // Aqua
            "#F56565", // Light red
            "#4299E1", // Light blue
            "#68D391", // Light green
            "#ED8936"  // Amber
        )

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.NAME,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.SYNC_EVENTS,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.OWNER_ACCOUNT
        )

        val selection = "${CalendarContract.Calendars.VISIBLE} = 1"

        contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            null,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )?.use { cursor ->
            var colorIndex = 0
            while (cursor.moveToNext()) {
                val androidId = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.NAME))
                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                val originalColor = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR))
                val visible = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.VISIBLE)) == 1
                val syncEvents = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.SYNC_EVENTS)) == 1

                val accountName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)) ?: ""
                val accountType = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)) ?: ""
                val ownerAccount = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.OWNER_ACCOUNT)) ?: ""

                // Use our diverse color palette instead of Android's colors
                val assignedColor = colorPalette[colorIndex % colorPalette.size]
                colorIndex++

                calendars.add(
                    CalendarInfo(
                        id = displayName ?: "Unknown Calendar",
                        name = name ?: "",
                        displayName = displayName ?: "Unknown Calendar",
                        color = assignedColor, // This color will be used in BOTH navigation and event cards
                        url = accountName,
                        isEnabled = visible && syncEvents,
                        description = "$accountType|$ownerAccount|$androidId"
                    )
                )

                println("✅ Calendar '${displayName}' assigned color: $assignedColor (will be used in both navigation and event cards)")
            }
        }

        calendars
    }

    suspend fun syncWithCalDAV() = withContext(Dispatchers.IO) {
        // For Android Calendar Provider, sync is handled by the system
        println("Sync with Android Calendar completed - handled by system")
    }

    private fun parseEventFromCursor(cursor: Cursor): CalendarEvent {
        val id = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events._ID))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)) ?: ""
        val description = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)) ?: ""
        val location = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)) ?: ""

        val startMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
        val endMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
        val allDay = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)) == 1

        val androidCalendarId = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID))
        val calendarDisplayName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_DISPLAY_NAME)) ?: ""
        val calendarColor = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_COLOR))

        val startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startMillis), ZoneId.systemDefault())
        val endTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endMillis), ZoneId.systemDefault())

        return CalendarEvent(
            id = id,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            isAllDay = allDay,
            location = location,
            calendarId = calendarDisplayName, // Use display name as calendar ID
            calendarName = calendarDisplayName,
            calendarColor = String.format("#%06X", 0xFFFFFF and calendarColor)
        )
    }

    private fun findCalendarIdByName(calendarName: String): String {
        println("=== Finding Calendar by Name ===")
        println("Looking for calendar with name: '$calendarName'")

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )

        val selection = "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(calendarName)

        contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val androidId = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                val foundName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                println("Found matching calendar: AndroidID=$androidId, Name='$foundName'")
                return androidId
            }
        }

        // If no exact match found, fall back to first available calendar
        println("No calendar found with name '$calendarName', using default calendar")
        return getDefaultCalendarId()
    }

    private fun getDefaultCalendarId(): String {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.SYNC_EVENTS} = 1"

        contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val androidId = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                println("Using default calendar: AndroidID=$androidId, Name='$name'")
                return androidId
            }
        }

        throw IllegalStateException("No writable calendar found")
    }
}