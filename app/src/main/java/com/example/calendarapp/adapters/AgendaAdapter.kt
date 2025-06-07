// Updated AgendaAdapter.kt - Combines date headers with their events
package com.example.calendarapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarapp.data.CalendarEvent
import com.example.calendarapp.databinding.ItemAgendaMonthHeaderBinding
import com.example.calendarapp.databinding.ItemAgendaWeekHeaderBinding
import com.example.calendarapp.databinding.ItemAgendaDayWithEventsBinding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*
import androidx.core.content.res.ResourcesCompat

class AgendaAdapter(private val onEventClick: (CalendarEvent) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_MONTH_HEADER = 0
        private const val TYPE_WEEK_HEADER = 1
        private const val TYPE_DAY_WITH_EVENTS = 2
    }

    private var items = listOf<AgendaItem>()

    sealed class AgendaItem {
        data class MonthHeader(val yearMonth: YearMonth) : AgendaItem()
        data class WeekHeader(val weekStart: LocalDate, val weekEnd: LocalDate) : AgendaItem()
        data class DayWithEvents(val date: LocalDate, val events: List<CalendarEvent>) : AgendaItem()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AgendaItem.MonthHeader -> TYPE_MONTH_HEADER
            is AgendaItem.WeekHeader -> TYPE_WEEK_HEADER
            is AgendaItem.DayWithEvents -> TYPE_DAY_WITH_EVENTS
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
            TYPE_DAY_WITH_EVENTS -> {
                val binding = ItemAgendaDayWithEventsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DayWithEventsViewHolder(binding, onEventClick)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AgendaItem.MonthHeader -> (holder as MonthHeaderViewHolder).bind(item.yearMonth)
            is AgendaItem.WeekHeader -> (holder as WeekHeaderViewHolder).bind(item.weekStart, item.weekEnd)
            is AgendaItem.DayWithEvents -> (holder as DayWithEventsViewHolder).bind(item.date, item.events)
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
    }

    // Function to find today's position in the list
    fun findTodayPosition(): Int {
        val today = LocalDate.now()
        println("Looking for today: $today")

        // Look for today's DayWithEvents item
        for (i in items.indices) {
            val item = items[i]
            if (item is AgendaItem.DayWithEvents && item.date == today) {
                println("Found today's date at position $i: ${item.date}")
                return i
            }
        }

        // Fallback: look for today's week
        for (i in items.indices) {
            val item = items[i]
            if (item is AgendaItem.WeekHeader) {
                if (!today.isBefore(item.weekStart) && !today.isAfter(item.weekEnd)) {
                    println("Found today's week at position $i")
                    return i
                }
            }
        }

        println("Today not found, returning position 0")
        return 0
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

                // Always add week header
                items.add(AgendaItem.WeekHeader(weekStart, weekEnd))

                // Get all dates in this week that have events
                val weekDatesWithEvents = eventsByDate.keys
                    .filter { date ->
                        !date.isBefore(weekStart) && !date.isAfter(weekEnd)
                    }
                    .sorted()

                // Add DayWithEvents items for each date with events
                for (date in weekDatesWithEvents) {
                    val dayEvents = eventsByDate[date] ?: emptyList()
                    items.add(AgendaItem.DayWithEvents(date, dayEvents))
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

    class DayWithEventsViewHolder(private val binding: ItemAgendaDayWithEventsBinding, private val onEventClick: (CalendarEvent) -> Unit) : RecyclerView.ViewHolder(binding.root) {

        fun bind(date: LocalDate, events: List<CalendarEvent>) {
            // Set the date display
            binding.textDayOfWeek.text = date.format(DateTimeFormatter.ofPattern("EEE")).uppercase()
            binding.textDayNumber.text = date.dayOfMonth.toString()

            // Set orange colors
            binding.textDayOfWeek.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            binding.textDayNumber.setTextColor(android.graphics.Color.parseColor("#FF9800"))

            // Clear previous events
            binding.eventsContainer.removeAllViews()

            // Add each event to the container
            events.forEach { event ->
                val eventView = createEventView(event)
                binding.eventsContainer.addView(eventView)
            }
        }

        private fun createEventView(event: CalendarEvent): View {
            val context = binding.root.context

            // Create a container for the event
            val eventContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16 // More spacing between events
                }
            }

            // Create a CardView with enhanced material design
            val cardView = androidx.cardview.widget.CardView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                // Enhanced material design properties
                radius = 16f // Larger radius for modern look
                cardElevation = 4f // More elevation for depth

                // Theme-appropriate background colors
                val isDarkMode = (context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES

                setCardBackgroundColor(
                    if (isDarkMode) {
                        android.graphics.Color.parseColor("#2B2B2B") // Dark gray for dark mode
                    } else {
                        android.graphics.Color.parseColor("#FFFFFF") // Pure white for light mode
                    }
                )

                // Enhanced material ripple effect
                isClickable = true
                isFocusable = true
            }

            // Create a horizontal container for colored line + content
            val cardContent = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Create enhanced colored line indicator with proper drawable
            val colorIndicator = android.view.View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    8, // Slightly wider for better visibility
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    marginEnd = 18 // More space between line and text
                }

                // Use the event's calendar color
                val calendarColor = try {
                    android.graphics.Color.parseColor(event.calendarColor)
                } catch (e: Exception) {
                    android.graphics.Color.parseColor("#3182CE") // Default blue from our palette
                }

                // Create a rounded rectangle drawable programmatically
                val drawable = android.graphics.drawable.GradientDrawable().apply {
                    setColor(calendarColor)
                    cornerRadius = 4f // Rounded corners
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                }

                background = drawable

                println("Event '${event.title}' using accent color: ${event.calendarColor}")
            }

            // Create the event text with enhanced styling
            val eventText = android.widget.TextView(context).apply {
                text = "${event.title}\n${event.startTime.format(DateTimeFormatter.ofPattern("h:mm a"))} - ${event.endTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
                textSize = 15f // Slightly larger text
                setPadding(0, 24, 24, 24) // More generous padding

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )

                // Text color based on theme
                val isDarkMode = (context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES

                setTextColor(
                    if (isDarkMode) {
                        android.graphics.Color.parseColor("#E6E1E5") // Light text for dark mode
                    } else {
                        android.graphics.Color.parseColor("#1C1B1F") // Dark text for light mode
                    }
                )

                // Enhanced line spacing for better readability
                setLineSpacing(10f, 1.3f)

                // Add subtle shadow for depth (light mode only)
                if (!isDarkMode) {
                    setShadowLayer(1f, 0f, 1f, android.graphics.Color.parseColor("#10000000"))
                }

                setOnClickListener { onEventClick(event) }
            }

            // Assemble the card
            cardContent.addView(colorIndicator)
            cardContent.addView(eventText)
            cardView.addView(cardContent)
            eventContainer.addView(cardView)

            return eventContainer
        }
    }
}