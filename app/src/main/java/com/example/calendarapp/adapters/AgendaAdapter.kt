// Updated AgendaAdapter.kt - Generate dates starting from today instead of 6 months back
package com.example.calendarapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarapp.data.CalendarEvent
import com.example.calendarapp.databinding.ItemAgendaDateHeaderBinding
import com.example.calendarapp.databinding.ItemAgendaEventBinding
import com.example.calendarapp.databinding.ItemAgendaMonthHeaderBinding
import com.example.calendarapp.databinding.ItemAgendaWeekHeaderBinding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

class AgendaAdapter(private val onEventClick: (CalendarEvent) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_MONTH_HEADER = 0
        private const val TYPE_WEEK_HEADER = 1
        private const val TYPE_DATE_HEADER = 2
        private const val TYPE_EVENT = 3
    }

    private var items = listOf<AgendaItem>()

    sealed class AgendaItem {
        data class MonthHeader(val yearMonth: YearMonth) : AgendaItem()
        data class WeekHeader(val weekStart: LocalDate, val weekEnd: LocalDate) : AgendaItem()
        data class DateHeader(val date: LocalDate) : AgendaItem()
        data class Event(val event: CalendarEvent) : AgendaItem()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AgendaItem.MonthHeader -> TYPE_MONTH_HEADER
            is AgendaItem.WeekHeader -> TYPE_WEEK_HEADER
            is AgendaItem.DateHeader -> TYPE_DATE_HEADER
            is AgendaItem.Event -> TYPE_EVENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_MONTH_HEADER -> {
                val binding = ItemAgendaMonthHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                MonthHeaderViewHolder(binding)
            }
            TYPE_WEEK_HEADER -> {
                val binding = ItemAgendaWeekHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                WeekHeaderViewHolder(binding)
            }
            TYPE_DATE_HEADER -> {
                val binding = ItemAgendaDateHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DateHeaderViewHolder(binding)
            }
            TYPE_EVENT -> {
                val binding = ItemAgendaEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                EventViewHolder(binding, onEventClick)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AgendaItem.MonthHeader -> (holder as MonthHeaderViewHolder).bind(item.yearMonth)
            is AgendaItem.WeekHeader -> (holder as WeekHeaderViewHolder).bind(item.weekStart, item.weekEnd)
            is AgendaItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item.date)
            is AgendaItem.Event -> (holder as EventViewHolder).bind(item.event)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateEvents(events: List<CalendarEvent>) {
        val newItems = generateAgendaItems(events)
        items = newItems
        notifyDataSetChanged()

        // Debug: Print the date range we generated
        val today = LocalDate.now()
        println("=== Generated agenda items ===")
        println("Today is: $today")
        println("Total items: ${items.size}")

        // Find the first and last month headers
        val monthHeaders = items.filterIsInstance<AgendaItem.MonthHeader>()
        if (monthHeaders.isNotEmpty()) {
            println("First month: ${monthHeaders.first().yearMonth}")
            println("Last month: ${monthHeaders.last().yearMonth}")
        }

        // Check if today's week is present
        val todayPosition = findTodayPosition()
        println("Today found at position: $todayPosition")
        if (todayPosition < items.size) {
            println("Item at today's position: ${items[todayPosition]}")
        }
    }

    // Function to find today's position in the list
    fun findTodayPosition(): Int {
        val today = LocalDate.now()
        val todayYearMonth = YearMonth.from(today)
        println("Looking for today: $today in month: $todayYearMonth")

        var todayMonthStart = -1
        var todayWeekPosition = -1
        var todayDatePosition = -1

        // First pass: find today's month
        for (i in items.indices) {
            val item = items[i]
            if (item is AgendaItem.MonthHeader && item.yearMonth == todayYearMonth) {
                todayMonthStart = i
                println("Found today's month header at position $i: ${item.yearMonth}")
                break
            }
        }

        if (todayMonthStart == -1) {
            println("Today's month not found, returning 0")
            return 0
        }

        // Second pass: look for today's week and date within this month
        for (i in (todayMonthStart + 1) until items.size) {
            val item = items[i]

            when (item) {
                is AgendaItem.MonthHeader -> {
                    // Hit the next month, stop looking
                    break
                }
                is AgendaItem.WeekHeader -> {
                    if (!today.isBefore(item.weekStart) && !today.isAfter(item.weekEnd)) {
                        todayWeekPosition = i
                        println("Found today's week at position $i: ${item.weekStart} to ${item.weekEnd}")
                    }
                }
                is AgendaItem.DateHeader -> {
                    if (item.date == today) {
                        todayDatePosition = i
                        println("Found today's exact date at position $i: ${item.date}")
                        break // Found exact date, stop looking
                    }
                }
                is AgendaItem.Event -> {
                    // Skip events
                    continue
                }
            }
        }

        // Return the best position we found
        val finalPosition = when {
            todayDatePosition != -1 -> {
                // Instead of returning the date position, return the week position for more predictable scrolling
                println("Found today's date, but returning week position for better scrolling")
                if (todayWeekPosition != -1) todayWeekPosition else todayDatePosition
            }
            todayWeekPosition != -1 -> {
                println("Returning today's week position: $todayWeekPosition")
                todayWeekPosition
            }
            else -> {
                println("Fallback to month start: $todayMonthStart")
                todayMonthStart
            }
        }

        println("Final position chosen: $finalPosition")
        return finalPosition
    }

    private fun generateAgendaItems(events: List<CalendarEvent>): List<AgendaItem> {
        val items = mutableListOf<AgendaItem>()
        val today = LocalDate.now()

        // Sort all events by date
        val sortedEvents = events.sortedBy { it.startTime }

        // Group events by date
        val eventsByDate = sortedEvents.groupBy { it.startTime.toLocalDate() }

        // Determine the actual date range we need to cover based on events
        val earliestEventDate = sortedEvents.firstOrNull()?.startTime?.toLocalDate()
        val latestEventDate = sortedEvents.lastOrNull()?.startTime?.toLocalDate()

        // Create a generous range that covers all events plus buffer, ensuring today is always included
        val startDate = when {
            earliestEventDate != null && earliestEventDate.isBefore(today.minusMonths(12)) -> {
                earliestEventDate.withDayOfMonth(1).minusMonths(1)
            }
            else -> today.minusMonths(12).withDayOfMonth(1)
        }

        val endDate = when {
            latestEventDate != null && latestEventDate.isAfter(today.plusMonths(12)) -> {
                latestEventDate.withDayOfMonth(1).plusMonths(1)
            }
            else -> today.plusMonths(12).withDayOfMonth(1)
        }

        // Ensure today's month is always included even if there are no events
        val actualStartDate = if (startDate.isAfter(today.withDayOfMonth(1))) {
            today.minusMonths(1).withDayOfMonth(1)
        } else startDate

        val actualEndDate = if (endDate.isBefore(today.withDayOfMonth(1))) {
            today.plusMonths(1).withDayOfMonth(1)
        } else endDate

        println("Date range: $actualStartDate to $actualEndDate (today: $today)")

        var currentYearMonth = YearMonth.from(actualStartDate)
        val endYearMonth = YearMonth.from(actualEndDate)

        while (!currentYearMonth.isAfter(endYearMonth)) {
            // Add month header
            items.add(AgendaItem.MonthHeader(currentYearMonth))

            // Generate weeks for this month
            val monthWeeks = generateWeeksForMonth(currentYearMonth)

            for (weekRange in monthWeeks) {
                val weekStart = weekRange.first
                val weekEnd = weekRange.second

                // Always add week header (whether it has events or not)
                items.add(AgendaItem.WeekHeader(weekStart, weekEnd))

                // Get all dates in this week that have events
                val weekDatesWithEvents = eventsByDate.keys
                    .filter { date ->
                        !date.isBefore(weekStart) && !date.isAfter(weekEnd)
                    }
                    .sorted()

                // Add date headers and events for this week (only if there are events)
                for (date in weekDatesWithEvents) {
                    items.add(AgendaItem.DateHeader(date))

                    // Add all events for this date
                    eventsByDate[date]?.forEach { event ->
                        items.add(AgendaItem.Event(event))
                    }
                }
            }

            currentYearMonth = currentYearMonth.plusMonths(1)
        }

        return items
    }

    private fun generateWeeksForMonth(yearMonth: YearMonth): List<Pair<LocalDate, LocalDate>> {
        val weeks = mutableListOf<Pair<LocalDate, LocalDate>>()
        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth()

        // Start from the first week that contains any day of this month
        val weekFields = WeekFields.of(Locale.getDefault())
        var currentWeekStart = firstDayOfMonth.with(weekFields.dayOfWeek(), 1)

        while (!currentWeekStart.isAfter(lastDayOfMonth)) {
            val currentWeekEnd = currentWeekStart.plusDays(6)

            // Only include weeks that have at least one day in the current month
            if (!currentWeekEnd.isBefore(firstDayOfMonth) && !currentWeekStart.isAfter(lastDayOfMonth)) {
                weeks.add(Pair(currentWeekStart, currentWeekEnd))
            }

            currentWeekStart = currentWeekStart.plusWeeks(1)
        }

        return weeks
    }

    class MonthHeaderViewHolder(private val binding: ItemAgendaMonthHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(yearMonth: YearMonth) {
            binding.textMonthYear.text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        }
    }

    class WeekHeaderViewHolder(private val binding: ItemAgendaWeekHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(weekStart: LocalDate, weekEnd: LocalDate) {
            val startFormat = if (weekStart.month == weekEnd.month) {
                // Same month: "Jun 1 - 7"
                weekStart.format(DateTimeFormatter.ofPattern("MMM d"))
            } else {
                // Different months: "May 30 - Jun 5"
                weekStart.format(DateTimeFormatter.ofPattern("MMM d"))
            }

            val endFormat = weekEnd.format(DateTimeFormatter.ofPattern("MMM d"))
            binding.textWeekRange.text = "$startFormat - $endFormat"
        }
    }

    class DateHeaderViewHolder(private val binding: ItemAgendaDateHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(date: LocalDate) {
            // Only set the compact date display (day of week and number)
            binding.textDayNumber.text = date.dayOfMonth.toString()
            binding.textDayOfWeek.text = date.format(DateTimeFormatter.ofPattern("EEE")).uppercase()

            // Removed the binding.textDate.text assignment since we removed that TextView
            // This prevents the crash when the layout doesn't have textDate anymore
        }
    }

    class EventViewHolder(private val binding: ItemAgendaEventBinding, private val onEventClick: (CalendarEvent) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        private var currentEvent: CalendarEvent? = null

        init {
            // Set click listeners for both card and line layouts
            binding.eventCard.setOnClickListener {
                currentEvent?.let { onEventClick(it) }
            }
            binding.eventLine.setOnClickListener {
                currentEvent?.let { onEventClick(it) }
            }
        }

        fun bind(event: CalendarEvent) {
            currentEvent = event
            // Calculate event duration in hours
            val durationHours = java.time.Duration.between(event.startTime, event.endTime).toHours()

            if (event.isAllDay) {
                // Show as line item for all-day events
                binding.eventCard.visibility = View.GONE
                binding.eventLine.visibility = View.VISIBLE

                binding.textEventTitleLine.text = event.title

                if (event.description.isNotEmpty()) {
                    binding.textEventDescriptionLine.text = event.description
                    binding.textEventDescriptionLine.visibility = View.VISIBLE
                } else {
                    binding.textEventDescriptionLine.visibility = View.GONE
                }

                // Set calendar color indicator for line item
                try {
                    binding.colorIndicatorLine.setBackgroundColor(android.graphics.Color.parseColor(event.calendarColor))
                } catch (e: Exception) {
                    binding.colorIndicatorLine.setBackgroundColor(android.graphics.Color.parseColor("#4285F4"))
                }

            } else {
                // Show as card for timed events
                binding.eventCard.visibility = View.VISIBLE
                binding.eventLine.visibility = View.GONE

                binding.textEventTitle.text = event.title

                // Format time display
                val startTime = event.startTime.format(DateTimeFormatter.ofPattern("h:mm a"))
                val endTime = event.endTime.format(DateTimeFormatter.ofPattern("h:mm a"))
                binding.textEventTime.text = "$startTime - $endTime"

                // Format duration display
                binding.textEventDuration.text = when {
                    durationHours < 1 -> "${java.time.Duration.between(event.startTime, event.endTime).toMinutes()}m"
                    durationHours == 1L -> "1h"
                    else -> "${durationHours}h"
                }

                if (event.description.isNotEmpty()) {
                    binding.textEventDescription.text = event.description
                    binding.textEventDescription.visibility = View.VISIBLE
                } else {
                    binding.textEventDescription.visibility = View.GONE
                }

                // Set calendar color indicator for card
                try {
                    binding.colorIndicator.setBackgroundColor(android.graphics.Color.parseColor(event.calendarColor))
                } catch (e: Exception) {
                    binding.colorIndicator.setBackgroundColor(android.graphics.Color.parseColor("#4285F4"))
                }

                // Scale card height based on duration (max 8 hours)
                val scaledDuration = kotlin.math.min(durationHours, 8L)
                val baseHeight = 120 // Increased base minimum height in dp to show full time range
                val additionalHeight = (scaledDuration * 20).toInt() // 20dp per hour, more generous spacing
                val totalHeightDp = baseHeight + additionalHeight

                // Convert dp to pixels
                val density = binding.root.context.resources.displayMetrics.density
                val totalHeightPx = (totalHeightDp * density).toInt()

                // Apply the height to the card's content container
                val cardContent = binding.eventCard.getChildAt(0) as LinearLayout
                val layoutParams = cardContent.layoutParams
                layoutParams.height = totalHeightPx
                cardContent.layoutParams = layoutParams
            }
        }
    }
}