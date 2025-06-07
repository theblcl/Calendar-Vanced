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

        println("=== AndroidCalendarRepository: getEvents() ===")
        println("Total events retrieved: ${events.size}")
        events.forEach { event ->
            println("Event: '${event.title}' -> Calendar: '${event.calendarName}' (Color: ${event.calendarColor})")
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
        println("Event timezone: ${event.timeZone}")
        println("Start time (local): ${event.startTime}")
        println("End time (local): ${event.endTime}")

        // Convert the event time from the selected timezone to UTC milliseconds
        val eventTimeZone = try {
            java.util.TimeZone.getTimeZone(event.timeZone)
        } catch (e: Exception) {
            println("Invalid timezone '${event.timeZone}', using system default")
            java.util.TimeZone.getDefault()
        }

        // Create ZoneId from the event's timezone
        val eventZoneId = eventTimeZone.toZoneId()

        // Convert LocalDateTime to ZonedDateTime in the event's timezone, then to UTC
        val startZoned = event.startTime.atZone(eventZoneId)
        val endZoned = event.endTime.atZone(eventZoneId)

        val startUtcMillis = startZoned.toInstant().toEpochMilli()
        val endUtcMillis = endZoned.toInstant().toEpochMilli()

        println("Start time (in ${event.timeZone}): $startZoned")
        println("End time (in ${event.timeZone}): $endZoned")
        println("Start UTC millis: $startUtcMillis")
        println("End UTC millis: $endUtcMillis")

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startUtcMillis)
            put(CalendarContract.Events.DTEND, endUtcMillis)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
            put(CalendarContract.Events.CALENDAR_ID, androidCalendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, event.timeZone) // Store the actual timezone
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
        println("Event timezone: ${event.timeZone}")

        // Convert the event time from the selected timezone to UTC milliseconds
        val eventTimeZone = try {
            java.util.TimeZone.getTimeZone(event.timeZone)
        } catch (e: Exception) {
            println("Invalid timezone '${event.timeZone}', using system default")
            java.util.TimeZone.getDefault()
        }

        // Create ZoneId from the event's timezone
        val eventZoneId = eventTimeZone.toZoneId()

        // Convert LocalDateTime to ZonedDateTime in the event's timezone, then to UTC
        val startZoned = event.startTime.atZone(eventZoneId)
        val endZoned = event.endTime.atZone(eventZoneId)

        val startUtcMillis = startZoned.toInstant().toEpochMilli()
        val endUtcMillis = endZoned.toInstant().toEpochMilli()

        println("Start time (in ${event.timeZone}): $startZoned")
        println("End time (in ${event.timeZone}): $endZoned")

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startUtcMillis)
            put(CalendarContract.Events.DTEND, endUtcMillis)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
            put(CalendarContract.Events.CALENDAR_ID, androidCalendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, event.timeZone) // Store the actual timezone
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

        // Define a diverse color palette - SAME as in getAssignedCalendarColor
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

                // CRITICAL: Use the SAME color assignment method for consistency
                val assignedColor = getAssignedCalendarColor(displayName ?: "Unknown Calendar")

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

                println("✅ Calendar '${displayName}' assigned color: $assignedColor (consistent color for nav + events)")
            }
        }

        println("=== AndroidCalendarRepository: getCalendars() ===")
        println("Total calendars retrieved: ${calendars.size}")
        calendars.forEach { calendar ->
            println("Calendar: '${calendar.displayName}' (ID: '${calendar.id}', Color: ${calendar.color})")
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

        // Get the event's timezone - important for proper display
        val eventTimeZone = try {
            // Try to get the timezone from the event data
            val projection = arrayOf(CalendarContract.Events.EVENT_TIMEZONE)
            val selection = "${CalendarContract.Events._ID} = ?"
            val selectionArgs = arrayOf(id)

            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { tzCursor ->
                if (tzCursor.moveToFirst()) {
                    val tz = tzCursor.getString(tzCursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_TIMEZONE))
                    if (!tz.isNullOrEmpty()) tz else TimeZone.getDefault().id
                } else {
                    TimeZone.getDefault().id
                }
            } ?: TimeZone.getDefault().id
        } catch (e: Exception) {
            println("Error getting event timezone: ${e.message}")
            TimeZone.getDefault().id
        }

        println("Event '$title': stored timezone = '$eventTimeZone'")

        // Convert UTC milliseconds back to LocalDateTime
        // The stored times are in UTC, but we need to display them in the event's timezone
        val eventZoneId = try {
            java.util.TimeZone.getTimeZone(eventTimeZone).toZoneId()
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }

        val startInstant = Instant.ofEpochMilli(startMillis)
        val endInstant = Instant.ofEpochMilli(endMillis)

        val startTime = LocalDateTime.ofInstant(startInstant, eventZoneId)
        val endTime = LocalDateTime.ofInstant(endInstant, eventZoneId)

        println("Event '$title': UTC start = $startInstant, local start in $eventTimeZone = $startTime")

        // CRITICAL: Get the assigned color for this calendar from our palette
        val assignedCalendarColor = getAssignedCalendarColor(calendarDisplayName)

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
            calendarColor = assignedCalendarColor, // Use our assigned color instead of Android's
            timeZone = eventTimeZone // Store the actual timezone
        )
    }

    private fun getAssignedCalendarColor(calendarDisplayName: String): String {
        // Use the same color assignment logic as in getCalendars()
        // CRITICAL: This ensures nav menu colors and event card colors match
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

        // Simple hash-based color assignment to ensure consistency
        val hash = calendarDisplayName.hashCode()
        val colorIndex = kotlin.math.abs(hash) % colorPalette.size
        val assignedColor = colorPalette[colorIndex]

        println("Color assignment for '$calendarDisplayName': hash=$hash, index=$colorIndex, color=$assignedColor")
        return assignedColor
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