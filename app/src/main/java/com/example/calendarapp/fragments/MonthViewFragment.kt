package com.example.calendarapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.calendarapp.adapters.MonthAdapter
import com.example.calendarapp.databinding.FragmentMonthViewBinding
import com.example.calendarapp.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MonthViewFragment : Fragment() {
    private var _binding: FragmentMonthViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CalendarViewModel
    private lateinit var monthAdapter: MonthAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMonthViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CalendarViewModel::class.java]

        setupRecyclerView()
        observeViewModel()

        val currentMonth = YearMonth.now()
        updateMonthView(currentMonth)
    }

    private fun setupRecyclerView() {
        monthAdapter = MonthAdapter { date ->
            viewModel.selectDate(date)
        }

        binding.recyclerViewMonth.apply {
            layoutManager = GridLayoutManager(context, 7)
            adapter = monthAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            binding.textCurrentMonth.text = date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        }

        viewModel.events.observe(viewLifecycleOwner) { events ->
            monthAdapter.updateEvents(events)
        }
    }

    private fun updateMonthView(yearMonth: YearMonth) {
        val daysInMonth = generateDaysForMonth(yearMonth)
        monthAdapter.updateDays(daysInMonth)
    }

    private fun generateDaysForMonth(yearMonth: YearMonth): List<LocalDate?> {
        val daysInMonth = mutableListOf<LocalDate?>()
        val firstDay = yearMonth.atDay(1)
        val lastDay = yearMonth.atEndOfMonth()

        // Add empty cells for days before the first day of the month
        val dayOfWeek = firstDay.dayOfWeek.value % 7
        repeat(dayOfWeek) {
            daysInMonth.add(null)
        }

        // Add all days of the month
        var currentDay = firstDay
        while (!currentDay.isAfter(lastDay)) {
            daysInMonth.add(currentDay)
            currentDay = currentDay.plusDays(1)
        }

        return daysInMonth
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}