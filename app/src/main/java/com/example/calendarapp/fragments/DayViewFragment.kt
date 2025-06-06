package com.example.calendarapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calendarapp.adapters.EventAdapter
import com.example.calendarapp.databinding.FragmentDayViewBinding
import com.example.calendarapp.viewmodel.CalendarViewModel
import java.time.format.DateTimeFormatter

class DayViewFragment : Fragment() {
    private var _binding: FragmentDayViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CalendarViewModel
    private lateinit var eventAdapter: EventAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDayViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CalendarViewModel::class.java]

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter()
        binding.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            binding.textCurrentDate.text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
        }

        viewModel.events.observe(viewLifecycleOwner) { events ->
            val selectedDate = viewModel.selectedDate.value
            if (selectedDate != null) {
                val dayEvents = events.filter { event ->
                    event.startTime.toLocalDate() == selectedDate
                }
                eventAdapter.updateEvents(dayEvents)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}