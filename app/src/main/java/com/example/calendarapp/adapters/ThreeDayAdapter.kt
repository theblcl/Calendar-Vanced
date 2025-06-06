package com.example.calendarapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarapp.data.CalendarEvent
import com.example.calendarapp.databinding.ItemThreeDayBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ThreeDayAdapter : RecyclerView.Adapter<ThreeDayAdapter.ThreeDayViewHolder>() {
    private var days = listOf<LocalDate>()
    private var events = listOf<CalendarEvent>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreeDayViewHolder {
        val binding = ItemThreeDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThreeDayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThreeDayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    fun updateDays(newDays: List<LocalDate>) {
        days = newDays
        notifyDataSetChanged()
    }

    fun updateEvents(newEvents: List<CalendarEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }

    inner class ThreeDayViewHolder(private val binding: ItemThreeDayBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(date: LocalDate) {
            binding.textDate.text = date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))

            val dayEvents = events.filter { it.startTime.toLocalDate() == date }
            val eventAdapter = EventAdapter()
            eventAdapter.updateEvents(dayEvents)

            binding.recyclerViewDayEvents.apply {
                layoutManager = LinearLayoutManager(binding.root.context)
                adapter = eventAdapter
            }
        }
    }
}