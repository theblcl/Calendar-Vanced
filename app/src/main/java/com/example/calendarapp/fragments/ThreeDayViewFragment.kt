package com.example.calendarapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calendarapp.adapters.ThreeDayAdapter
import com.example.calendarapp.databinding.FragmentThreeDayViewBinding
import com.example.calendarapp.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ThreeDayViewFragment : Fragment() {
    private var _binding: FragmentThreeDayViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CalendarViewModel
    private lateinit var threeDayAdapter: ThreeDayAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentThreeDayViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CalendarViewModel::class.java]

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        threeDayAdapter = ThreeDayAdapter()
        binding.recyclerViewThreeDay.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = threeDayAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            val threeDays = getThreeDays(date)
            val startDate = threeDays.first().format(DateTimeFormatter.ofPattern("MMM d"))
            val endDate = threeDays.last().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            binding.textCurrentRange.text = "$startDate - $endDate"
            threeDayAdapter.updateDays(threeDays)
        }

        viewModel.events.observe(viewLifecycleOwner) { events ->
            threeDayAdapter.updateEvents(events)
        }
    }

    private fun getThreeDays(date: LocalDate): List<LocalDate> {
        return listOf(date, date.plusDays(1), date.plusDays(2))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}