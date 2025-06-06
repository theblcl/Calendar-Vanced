package com.example.calendarapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarapp.data.CalendarEvent
import com.example.calendarapp.databinding.ItemMonthDayBinding
import java.time.LocalDate

class MonthAdapter(private val onDateClick: (LocalDate) -> Unit) : RecyclerView.Adapter<MonthAdapter.DayViewHolder>() {
    private var days = listOf<LocalDate?>()
    private var events = listOf<CalendarEvent>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemMonthDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    fun updateDays(newDays: List<LocalDate?>) {
        days = newDays
        notifyDataSetChanged()
    }

    fun updateEvents(newEvents: List<CalendarEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }

    inner class DayViewHolder(private val binding: ItemMonthDayBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(date: LocalDate?) {
            if (date != null) {
                binding.textDay.text = date.dayOfMonth.toString()
                binding.textDay.alpha = 1f

                // Check if there are events on this day
                val hasEvents = events.any { it.startTime.toLocalDate() == date }
                binding.eventIndicator.visibility = if (hasEvents) android.view.View.VISIBLE else android.view.View.GONE

                binding.root.setOnClickListener {
                    onDateClick(date)
                }
            } else {
                binding.textDay.text = ""
                binding.textDay.alpha = 0.3f
                binding.eventIndicator.visibility = android.view.View.GONE
                binding.root.setOnClickListener(null)
            }
        }
    }
}