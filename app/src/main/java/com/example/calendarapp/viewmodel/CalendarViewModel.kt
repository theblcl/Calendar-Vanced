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

    init {
        _selectedDate.value = LocalDate.now()
        _visibleCalendars.value = emptySet()
        loadCalendars()
        loadEvents()
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun loadEvents() {
        viewModelScope.launch {
            try {
                val eventList = repository.getEvents()
                _allEvents.value = eventList
                filterEvents() // Apply calendar visibility filter
                _syncStatus.value = "Events loaded from Android Calendar"
            } catch (e: Exception) {
                _syncStatus.value = "Error loading events: ${e.message}"
            }
        }
    }

    fun loadCalendars() {
        viewModelScope.launch {
            try {
                val calendars = repository.getCalendars()
                _availableCalendars.value = calendars

                // Initially show all calendars
                val allCalendarNames = calendars.map { it.displayName }.toSet()
                _visibleCalendars.value = allCalendarNames
                filterEvents()
            } catch (e: Exception) {
                println("Error loading calendars: ${e.message}")
            }
        }
    }

    fun toggleCalendarVisibility(calendarName: String) {
        val currentVisible = _visibleCalendars.value ?: emptySet()
        val newVisible = if (currentVisible.contains(calendarName)) {
            currentVisible - calendarName
        } else {
            currentVisible + calendarName
        }
        _visibleCalendars.value = newVisible
        filterEvents()

        println("Calendar '$calendarName' visibility toggled. Visible calendars: $newVisible")
    }

    fun isCalendarVisible(calendarName: String): Boolean {
        return _visibleCalendars.value?.contains(calendarName) ?: false
    }

    private fun filterEvents() {
        val allEvents = _allEvents.value ?: emptyList()
        val visibleCalendarNames = _visibleCalendars.value ?: emptySet()

        val filteredEvents = if (visibleCalendarNames.isEmpty()) {
            // If no calendars are selected, show all events
            allEvents
        } else {
            // Only show events from visible calendars
            allEvents.filter { event ->
                visibleCalendarNames.contains(event.calendarName)
            }
        }

        _events.value = filteredEvents
        println("Filtered events: ${filteredEvents.size} out of ${allEvents.size} total events")
    }

    fun syncCalendar() {
        viewModelScope.launch {
            try {
                _syncStatus.value = "Syncing with Android Calendar..."
                repository.syncWithCalDAV()
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