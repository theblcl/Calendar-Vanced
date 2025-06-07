// Updated AgendaViewFragment.kt with smooth scroll to today and integration with new adapter methods
package com.example.calendarapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calendarapp.MainActivity
import com.example.calendarapp.adapters.AgendaAdapter
import com.example.calendarapp.databinding.FragmentAgendaViewBinding
import com.example.calendarapp.viewmodel.CalendarViewModel

class AgendaViewFragment : Fragment() {
    private var _binding: FragmentAgendaViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CalendarViewModel
    private lateinit var agendaAdapter: AgendaAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAgendaViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CalendarViewModel::class.java]

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        agendaAdapter = AgendaAdapter { event ->
            // Handle event click
            (activity as? MainActivity)?.showEventDetail(event.id)
        }
        binding.recyclerViewAgenda.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = agendaAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.events.observe(viewLifecycleOwner) { events ->
            // Always show all events with dynamic range
            agendaAdapter.updateEvents(events)

            // Smooth scroll to today on first load
            scrollToTodayOnLoad()
        }
    }

    fun scrollToToday() {
        _binding?.let { binding ->
            binding.recyclerViewAgenda.post {
                if (_binding == null) return@post

                val todayPosition = agendaAdapter.findTodayPosition()
                println("Scrolling to position: $todayPosition")

                val layoutManager = binding.recyclerViewAgenda.layoutManager as LinearLayoutManager

                // Use scrollToPositionWithOffset to place today at the top
                layoutManager.scrollToPositionWithOffset(todayPosition, 0)
            }
        }
    }

    private fun scrollToTodayOnLoad() {
        _binding?.let { binding ->
            binding.recyclerViewAgenda.post {
                if (_binding == null) return@post

                val todayPosition = agendaAdapter.findTodayPosition()
                // Use regular scrollToPosition for initial load to avoid jarring movement
                binding.recyclerViewAgenda.scrollToPosition(todayPosition)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}