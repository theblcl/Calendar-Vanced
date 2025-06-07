package com.example.calendarapp.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarapp.data.CalendarEvent
import com.example.calendarapp.data.CalendarInfo
import com.example.calendarapp.data.AndroidCalendarRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

class CalendarViewModel(private val context: Context) : ViewModel() {
    private val repository = AndroidCalendarRepository(context)

    private val _events = MutableLiveData<List<CalendarEvent>>()
    val events: LiveData<List<CalendarEvent>> = _events

    private val _allEvents = MutableLiveData<List<CalendarEvent>>()
    private val _visibleCalendars = MutableLiveData<Set<String>>()
    val visibleCalendars: LiveData<Set<String>> = _visibleCalendars

    private val _availableCalendars = MutableLiveData<List<CalendarInfo>>()
    val availableCalendars: LiveData<List<CalendarInfo>> = _availableCalendars

    private val _selectedDate = MutableLiveData<LocalDate>()
    val selectedDate: LiveData<LocalDate> = _selectedDate

    private val _syncStatus = MutableLiveData<String>()
    val syncStatus: LiveData<String> = _syncStatus

    private var isInitialized = false

    init {
        _selectedDate.value = LocalDate.now()
        // Start with empty set initially - this is important
        _visibleCalendars.value = emptySet()

        // Load data in the correct order
        loadCalendars()
        loadEvents()
    }

    fun loadCalendars() {
        viewModelScope.launch {
            try {
                val calendars = repository.getCalendars()
                _availableCalendars.value = calendars

                // IMPORTANT: Only set all calendars visible on first load
                if (!isInitialized) {
                    val allCalendarNames = calendars.map { it.displayName }.toSet()
                    _visibleCalendars.value = allCalendarNames
                    isInitialized = true

                    println("=== Initial Calendar Load ===")
                    println("Loaded ${calendars.size} calendars, all initially visible")
                    allCalendarNames.forEach { name ->
                        println("  ✅ '$name' - visible")
                    }

                    // Apply the filter after setting visible calendars
                    filterEvents()
                } else {
                    // On subsequent loads, preserve existing visibility state
                    println("=== Subsequent Calendar Load - Preserving Visibility State ===")
                    val currentVisible = _visibleCalendars.value ?: emptySet()
                    println("Current visible calendars: $currentVisible")
                    filterEvents()
                }

            } catch (e: Exception) {
                println("Error loading calendars: ${e.message}")
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun loadEvents() {
        viewModelScope.launch {
            try {
                val eventList = repository.getEvents()
                _allEvents.value = eventList

                println("=== Events Loaded ===")
                println("Total events loaded: ${eventList.size}")

                // Debug: Print each event and its calendar association
                eventList.forEach { event ->
                    println("Event: '${event.title}' -> Calendar: '${event.calendarName}' (ID: '${event.calendarId}')")
                }

                filterEvents() // Apply calendar visibility filter
                _syncStatus.value = "Events loaded from Android Calendar"
            } catch (e: Exception) {
                _syncStatus.value = "Error loading events: ${e.message}"
                println("Error loading events: ${e.message}")
            }
        }
    }

    fun toggleCalendarVisibility(calendarName: String) {
        val currentVisible = _visibleCalendars.value ?: emptySet()
        val wasVisible = currentVisible.contains(calendarName)
        val newVisible = if (wasVisible) {
            currentVisible - calendarName
        } else {
            currentVisible + calendarName
        }

        println("=== Toggling Calendar Visibility ===")
        println("Calendar: '$calendarName'")
        println("Was visible: $wasVisible")
        println("Now visible: ${newVisible.contains(calendarName)}")
        println("Before: $currentVisible")
        println("After: $newVisible")

        // Update the visibility state - this will trigger observers
        _visibleCalendars.value = newVisible

        // Force immediate filtering
        filterEvents()
    }

    fun isCalendarVisible(calendarName: String): Boolean {
        val visible = _visibleCalendars.value?.contains(calendarName) ?: false
        return visible
    }

    private fun filterEvents() {
        val allEvents = _allEvents.value ?: emptyList()
        val visibleCalendarNames = _visibleCalendars.value ?: emptySet()

        println("=== Event Filtering ===")
        println("Total events: ${allEvents.size}")
        println("Visible calendars: $visibleCalendarNames")

        // Debug: Show all unique calendar names in events
        val eventCalendarNames = allEvents.map { it.calendarName }.distinct()
        println("Calendar names found in events: $eventCalendarNames")

        val filteredEvents = if (visibleCalendarNames.isEmpty()) {
            // If no calendars are selected, show all events (fallback behavior)
            println("No calendars selected - showing all events")
            allEvents
        } else {
            // Only show events from visible calendars
            val filtered = allEvents.filter { event ->
                val isVisible = visibleCalendarNames.contains(event.calendarName)
                if (!isVisible) {
                    println("  ❌ Hiding event '${event.title}' from calendar '${event.calendarName}'")
                } else {
                    println("  ✅ Showing event '${event.title}' from calendar '${event.calendarName}'")
                }
                isVisible
            }
            filtered
        }

        println("Filtered events: ${filteredEvents.size} out of ${allEvents.size}")

        // Update the events LiveData - this should trigger UI updates
        _events.value = filteredEvents
    }

    fun syncCalendar() {
        viewModelScope.launch {
            try {
                _syncStatus.value = "Syncing with Android Calendar..."
                repository.syncWithCalDAV() // This just does nothing for Android provider
                loadEvents()
                _syncStatus.value = "Sync completed"
            } catch (e: Exception) {
                _syncStatus.value = "Sync failed: ${e.message}"
            }
        }
    }

    fun addEvent(event: CalendarEvent) {
        viewModelScope.launch {
            try {
                repository.addEvent(event)
                loadEvents()
                _syncStatus.value = "Event created in Android Calendar"
            } catch (e: Exception) {
                _syncStatus.value = "Failed to create event: ${e.message}"
            }
        }
    }

    fun updateEvent(event: CalendarEvent) {
        viewModelScope.launch {
            try {
                repository.updateEvent(event)
                loadEvents()
                _syncStatus.value = "Event updated in Android Calendar"
            } catch (e: Exception) {
                _syncStatus.value = "Failed to update event: ${e.message}"
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            try {
                repository.deleteEvent(eventId)
                loadEvents()
                _syncStatus.value = "Event deleted from Android Calendar"
            } catch (e: Exception) {
                _syncStatus.value = "Failed to delete event: ${e.message}"
            }
        }
    }

    suspend fun getAvailableCalendars(): List<CalendarInfo> {
        return repository.getCalendars()
    }
}