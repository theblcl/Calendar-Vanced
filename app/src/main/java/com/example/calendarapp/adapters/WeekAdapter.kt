package com.example.calendarapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarapp.data.CalendarEvent
import com.example.calendarapp.databinding.ItemWeekDayBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WeekAdapter : RecyclerView.Adapter<WeekAdapter.WeekDayViewHolder>() {
    private var weekDays = listOf<LocalDate>()
    private var events = listOf<CalendarEvent>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekDayViewHolder {
        val binding = ItemWeekDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WeekDayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeekDayViewHolder, position: Int) {
        holder.bind(weekDays[position])
    }

    override fun getItemCount(): Int = weekDays.size

    fun updateWeek(newWeekDays: List<LocalDate>) {
        weekDays = newWeekDays
        notifyDataSetChanged()
    }

    fun updateEvents(newEvents: List<CalendarEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }

    inner class WeekDayViewHolder(private val binding: ItemWeekDayBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(date: LocalDate) {
            binding.textDayName.text = date.format(DateTimeFormatter.ofPattern("EEE"))
            binding.textDayNumber.text = date.dayOfMonth.toString()

            val dayEvents = events.filter { it.startTime.toLocalDate() == date }
            binding.textEventCount.text = "${dayEvents.size} events"
        }
    }
}