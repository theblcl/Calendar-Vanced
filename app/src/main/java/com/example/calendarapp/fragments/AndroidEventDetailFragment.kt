package com.example.calendarapp.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.calendarapp.data.CalendarEvent
import com.example.calendarapp.data.CalendarInfo
import com.example.calendarapp.databinding.FragmentEventDetailBinding
import com.example.calendarapp.viewmodel.CalendarViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class AndroidEventDetailFragment : DialogFragment() {
    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CalendarViewModel

    private var editingEvent: CalendarEvent? = null
    private var selectedStartDateTime = LocalDateTime.now()
    private var selectedEndDateTime = LocalDateTime.now().plusHours(1)
    private var isAllDay = false
    private var availableCalendars = listOf<CalendarInfo>()
    private var selectedCalendar: CalendarInfo? = null
    private var selectedTimeZone: TimeZone = TimeZone.getDefault()

    private val availableTimeZones = listOf(
        TimeZone.getTimeZone("America/New_York"),
        TimeZone.getTimeZone("America/Chicago"),
        TimeZone.getTimeZone("America/Denver"),
        TimeZone.getTimeZone("America/Los_Angeles"),
        TimeZone.getTimeZone("America/Phoenix"),
        TimeZone.getTimeZone("America/Anchorage"),
        TimeZone.getTimeZone("Pacific/Honolulu"),
        TimeZone.getTimeZone("Europe/London"),
        TimeZone.getTimeZone("Europe/Paris"),
        TimeZone.getTimeZone("Europe/Berlin"),
        TimeZone.getTimeZone("Europe/Rome"),
        TimeZone.getTimeZone("Europe/Madrid"),
        TimeZone.getTimeZone("Asia/Tokyo"),
        TimeZone.getTimeZone("Asia/Shanghai"),
        TimeZone.getTimeZone("Asia/Seoul"),
        TimeZone.getTimeZone("Asia/Mumbai"),
        TimeZone.getTimeZone("Australia/Sydney"),
        TimeZone.getTimeZone("Australia/Melbourne"),
        TimeZone.getTimeZone("UTC")
    ).plus(TimeZone.getDefault()) // Add device timezone
        .distinctBy { it.id } // Remove duplicates
        .sortedBy { it.getDisplayName(false, TimeZone.SHORT) }

    companion object {
        private const val ARG_EVENT_ID = "event_id"

        fun newInstance(eventId: String? = null): AndroidEventDetailFragment {
            val fragment = AndroidEventDetailFragment()
            val args = Bundle()
            if (eventId != null) {
                args.putString(ARG_EVENT_ID, eventId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)

        // Make the dialog full screen
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CalendarViewModel::class.java]

        setupUI()
        setupTimezoneDropdown()
        setupClickListeners()

        // Load calendars FIRST, then load event in the callback
        loadCalendarsAndEvent()
    }

    private fun setupUI() {
        // Make dialog full screen
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Remove dialog window background and decorations for true full screen
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setupTimezoneDropdown() {
        val timezoneNames = availableTimeZones.map { timezone ->
            val shortName = timezone.getDisplayName(false, TimeZone.SHORT)
            val longName = timezone.getDisplayName(false, TimeZone.LONG)
            if (shortName == longName) {
                shortName
            } else {
                "$shortName - $longName"
            }
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, timezoneNames)
        binding.spinnerTimezone.setAdapter(adapter)

        // Set default to device timezone
        selectedTimeZone = TimeZone.getDefault()
        val defaultIndex = availableTimeZones.indexOfFirst { it.id == selectedTimeZone.id }
        if (defaultIndex >= 0) {
            binding.spinnerTimezone.setText(timezoneNames[defaultIndex], false)
        }

        binding.spinnerTimezone.setOnItemClickListener { _, _, position, _ ->
            selectedTimeZone = availableTimeZones[position]
            updateTimeDisplay()
        }
    }

    private fun loadCalendarsAndEvent() {
        lifecycleScope.launch {
            try {
                // Load calendars first
                availableCalendars = viewModel.getAvailableCalendars()

                println("=== Available Calendars for Selection ===")
                availableCalendars.forEachIndexed { index, calendar ->
                    println("Calendar $index:")
                    println("  ID: '${calendar.id}'")
                    println("  Name: '${calendar.name}'")
                    println("  DisplayName: '${calendar.displayName}'")
                }

                setupCalendarDropdown()

                // THEN load and match the event
                loadEvent()
                updateTimeDisplay()

            } catch (e: Exception) {
                println("Error loading calendars: ${e.message}")
                Toast.makeText(context, "Error loading calendars: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCalendarDropdown() {
        val calendarNames = availableCalendars.map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, calendarNames)
        binding.spinnerCalendar.setAdapter(adapter)

        // Set default selection
        if (availableCalendars.isNotEmpty()) {
            selectedCalendar = availableCalendars.first()
            binding.spinnerCalendar.setText(selectedCalendar?.displayName ?: "", false)
        }

        binding.spinnerCalendar.setOnItemClickListener { _, _, position, _ ->
            selectedCalendar = availableCalendars[position]
        }
    }

    private fun loadEvent() {
        val eventId = arguments?.getString(ARG_EVENT_ID)
        if (eventId != null) {
            // Find the event to edit
            viewModel.events.value?.find { it.id == eventId }?.let { event ->
                editingEvent = event
                binding.editTextTitle.setText(event.title)
                binding.editTextDescription.setText(event.description)
                binding.editTextLocation.setText(event.location)

                selectedStartDateTime = event.startTime
                selectedEndDateTime = event.endTime
                isAllDay = event.isAllDay

                // Set timezone from event
                try {
                    selectedTimeZone = TimeZone.getTimeZone(event.timeZone)
                    val timezoneIndex = availableTimeZones.indexOfFirst { it.id == event.timeZone }
                    if (timezoneIndex >= 0) {
                        val timezoneNames = availableTimeZones.map { timezone ->
                            val shortName = timezone.getDisplayName(false, TimeZone.SHORT)
                            val longName = timezone.getDisplayName(false, TimeZone.LONG)
                            if (shortName == longName) shortName else "$shortName - $longName"
                        }
                        binding.spinnerTimezone.setText(timezoneNames[timezoneIndex], false)
                    }
                } catch (e: Exception) {
                    selectedTimeZone = TimeZone.getDefault()
                }

                binding.switchAllDay.isChecked = isAllDay
                binding.textDialogTitle.text = "Edit Event"
                binding.buttonDelete.visibility = View.VISIBLE

                // NOW calendar matching will work because calendars are loaded
                println("=== DEBUG: Event Calendar Matching ===")
                println("Event calendarId: '${event.calendarId}'")
                println("Event calendarName: '${event.calendarName}'")
                println("Available calendars count: ${availableCalendars.size}")

                val eventCalendar = availableCalendars.find { calendar ->
                    val displayNameMatch = calendar.displayName == event.calendarName
                    println("Testing '${calendar.displayName}' == '${event.calendarName}': $displayNameMatch")
                    displayNameMatch
                }

                if (eventCalendar != null) {
                    selectedCalendar = eventCalendar
                    binding.spinnerCalendar.setText(eventCalendar.displayName, false)
                    println("✅ Successfully matched calendar: '${eventCalendar.displayName}'")
                } else {
                    println("❌ No exact match found")
                    if (availableCalendars.isNotEmpty()) {
                        selectedCalendar = availableCalendars.first()
                        binding.spinnerCalendar.setText(selectedCalendar?.displayName ?: "", false)
                        println("Using first available: '${selectedCalendar?.displayName}'")
                    }
                }
            }
        } else {
            // Creating new event
            binding.textDialogTitle.text = "New Event"
            binding.buttonDelete.visibility = View.GONE

            // Set default time to next hour
            val now = LocalDateTime.now()
            selectedStartDateTime = now.withMinute(0).withSecond(0).withNano(0).plusHours(1)
            selectedEndDateTime = selectedStartDateTime.plusHours(1)
        }
    }

    private fun setupClickListeners() {
        // Close button (X)
        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.buttonSave.setOnClickListener {
            saveEvent()
        }

        binding.buttonDelete.setOnClickListener {
            deleteEvent()
        }

        binding.switchAllDay.setOnCheckedChangeListener { _, isChecked ->
            isAllDay = isChecked
            updateTimeDisplay()
        }

        binding.layoutStartDate.setOnClickListener {
            showDatePicker(true)
        }

        binding.layoutStartTime.setOnClickListener {
            if (!isAllDay) {
                showTimePicker(true)
            }
        }

        binding.layoutEndDate.setOnClickListener {
            showDatePicker(false)
        }

        binding.layoutEndTime.setOnClickListener {
            if (!isAllDay) {
                showTimePicker(false)
            }
        }
    }

    private fun updateTimeDisplay() {
        // Get timezone abbreviation
        val timezoneInfo = selectedTimeZone.getDisplayName(false, TimeZone.SHORT)

        // Update start date/time
        binding.textStartDate.text = selectedStartDateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        binding.textStartTime.text = if (isAllDay) {
            "All day"
        } else {
            "${selectedStartDateTime.format(DateTimeFormatter.ofPattern("h:mm a"))} $timezoneInfo"
        }

        // Update end date/time
        binding.textEndDate.text = selectedEndDateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        binding.textEndTime.text = if (isAllDay) {
            "All day"
        } else {
            "${selectedEndDateTime.format(DateTimeFormatter.ofPattern("h:mm a"))} $timezoneInfo"
        }

        // Show/hide time layouts
        binding.layoutStartTime.visibility = if (isAllDay) View.GONE else View.VISIBLE
        binding.layoutEndTime.visibility = if (isAllDay) View.GONE else View.VISIBLE
    }

    private fun showDatePicker(isStart: Boolean) {
        val currentDate = if (isStart) selectedStartDateTime.toLocalDate() else selectedEndDateTime.toLocalDate()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val newDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (isStart) {
                    selectedStartDateTime = selectedStartDateTime.with(newDate)
                    // Adjust end time if needed
                    if (selectedEndDateTime.toLocalDate().isBefore(newDate)) {
                        selectedEndDateTime = selectedEndDateTime.with(newDate)
                    }
                } else {
                    selectedEndDateTime = selectedEndDateTime.with(newDate)
                    // Adjust start time if needed
                    if (selectedStartDateTime.toLocalDate().isAfter(newDate)) {
                        selectedStartDateTime = selectedStartDateTime.with(newDate)
                    }
                }
                updateTimeDisplay()
            },
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        ).show()
    }

    private fun showTimePicker(isStart: Boolean) {
        val currentTime = if (isStart) selectedStartDateTime.toLocalTime() else selectedEndDateTime.toLocalTime()

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val newTime = LocalTime.of(hourOfDay, minute)
                if (isStart) {
                    selectedStartDateTime = selectedStartDateTime.with(newTime)
                    // Adjust end time if it's now before start time
                    if (selectedEndDateTime.isBefore(selectedStartDateTime)) {
                        selectedEndDateTime = selectedStartDateTime.plusHours(1)
                    }
                } else {
                    selectedEndDateTime = selectedEndDateTime.with(newTime)
                    // Adjust start time if it's now after end time
                    if (selectedStartDateTime.isAfter(selectedEndDateTime)) {
                        selectedStartDateTime = selectedEndDateTime.minusHours(1)
                    }
                }
                updateTimeDisplay()
            },
            currentTime.hour,
            currentTime.minute,
            false
        ).show()
    }

    private fun saveEvent() {
        val title = binding.editTextTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCalendar == null) {
            Toast.makeText(context, "Please select a calendar", Toast.LENGTH_SHORT).show()
            return
        }

        val event = CalendarEvent(
            id = editingEvent?.id ?: UUID.randomUUID().toString(),
            title = title,
            description = binding.editTextDescription.text.toString().trim(),
            startTime = selectedStartDateTime,
            endTime = selectedEndDateTime,
            isAllDay = isAllDay,
            location = binding.editTextLocation.text.toString().trim(),
            calendarId = selectedCalendar!!.id,
            calendarName = selectedCalendar!!.displayName,
            calendarColor = selectedCalendar!!.color,
            timeZone = selectedTimeZone.id
        )

        if (editingEvent != null) {
            viewModel.updateEvent(event)
        } else {
            viewModel.addEvent(event)
        }

        Toast.makeText(context, "Event saved", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    private fun deleteEvent() {
        editingEvent?.let { event ->
            viewModel.deleteEvent(event.id)
            Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}