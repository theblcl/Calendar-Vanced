package com.example.calendarapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calendarapp.adapters.WeekAdapter
import com.example.calendarapp.databinding.FragmentWeekViewBinding
import com.example.calendarapp.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

class WeekViewFragment : Fragment() {
    private var _binding: FragmentWeekViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CalendarViewModel
    private lateinit var weekAdapter: WeekAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeekViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CalendarViewModel::class.java]

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        weekAdapter = WeekAdapter()
        binding.recyclerViewWeek.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = weekAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            val weekDays = getWeekDays(date)
            val startDate = weekDays.first().format(DateTimeFormatter.ofPattern("MMM d"))
            val endDate = weekDays.last().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            binding.textCurrentWeek.text = "$startDate - $endDate"
            weekAdapter.updateWeek(weekDays)
        }

        viewModel.events.observe(viewLifecycleOwner) { events ->
            weekAdapter.updateEvents(events)
        }
    }

    private fun getWeekDays(date: LocalDate): List<LocalDate> {
        val weekFields = WeekFields.of(Locale.getDefault())
        val startOfWeek = date.with(weekFields.dayOfWeek(), 1)
        return (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}