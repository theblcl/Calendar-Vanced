package com.example.calendarapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.*

class CalendarRepository {
    private val events = mutableListOf<CalendarEvent>()
    private val calDavSync = CalDAVSync()

    init {
        // Add some sample events
        events.addAll(getSampleEvents())
    }

    suspend fun getEvents(): List<CalendarEvent> = withContext(Dispatchers.IO) {
        events.toList()
    }

    suspend fun addEvent(event: CalendarEvent) = withContext(Dispatchers.IO) {
        events.add(event)
        // Sync to CalDAV server
        try {
            calDavSync.createEvent(event)
        } catch (e: Exception) {
            println("Failed to sync new event to CalDAV: ${e.message}")
            // Keep the event locally even if sync fails
        }
    }

    suspend fun updateEvent(event: CalendarEvent) = withContext(Dispatchers.IO) {
        val index = events.indexOfFirst { it.id == event.id }
        if (index != -1) {
            events[index] = event
            // Sync to CalDAV server
            try {
                calDavSync.updateEvent(event)
            } catch (e: Exception) {
                println("Failed to sync updated event to CalDAV: ${e.message}")
                // Keep the local update even if sync fails
            }
        }
    }

    suspend fun deleteEvent(eventId: String) = withContext(Dispatchers.IO) {
        val eventToRemove = events.find { it.id == eventId }
        if (eventToRemove != null) {
            events.remove(eventToRemove)
            // Sync to CalDAV server
            try {
                calDavSync.deleteEvent(eventId)
            } catch (e: Exception) {
                println("Failed to sync deleted event to CalDAV: ${e.message}")
                // Keep the local deletion even if sync fails
            }
        }
    }

    suspend fun syncWithCalDAV() = withContext(Dispatchers.IO) {
        val remoteEvents = calDavSync.fetchEvents()
        events.clear()
        events.addAll(remoteEvents)
    }

    private fun getSampleEvents(): List<CalendarEvent> {
        val now = LocalDateTime.now()
        return listOf(
            CalendarEvent(
                id = UUID.randomUUID().toString(),
                title = "Team Meeting",
                description = "Weekly team standup",
                startTime = now.plusDays(1).withHour(10).withMinute(0),
                endTime = now.plusDays(1).withHour(11).withMinute(0),
                calendarColor = "#FF6B35"
            ),
            CalendarEvent(
                id = UUID.randomUUID().toString(),
                title = "Project Deadline",
                description = "App development milestone",
                startTime = now.plusDays(3).withHour(9).withMinute(0),
                endTime = now.plusDays(3).withHour(17).withMinute(0),
                calendarColor = "#4ECDC4"
            )
        )
    }
}