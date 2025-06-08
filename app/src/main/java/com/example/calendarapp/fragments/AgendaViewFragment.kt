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
        setupPageNavigation()
        updatePageNavigationVisibility()
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

    private fun setupPageNavigation() {
        binding.buttonPageUp.setOnClickListener {
            pageUp()
        }

        binding.buttonPageDown.setOnClickListener {
            pageDown()
        }
    }

    private fun updatePageNavigationVisibility() {
        // Only show page navigation buttons when e-ink mode is enabled
        val isEPaperMode = com.example.calendarapp.utils.EPaperUtils.isEPaperMode(requireContext())

        // Access the buttons directly since they're in our layout
        if (isEPaperMode) {
            binding.buttonPageUp.visibility = android.view.View.VISIBLE
            binding.buttonPageDown.visibility = android.view.View.VISIBLE
            // Also show the parent container
            binding.buttonPageUp.parent?.let { parent ->
                (parent as? android.view.View)?.visibility = android.view.View.VISIBLE
            }
            println("E-ink mode enabled - showing page navigation buttons")
        } else {
            binding.buttonPageUp.visibility = android.view.View.GONE
            binding.buttonPageDown.visibility = android.view.View.GONE
            // Also hide the parent container
            binding.buttonPageUp.parent?.let { parent ->
                (parent as? android.view.View)?.visibility = android.view.View.GONE
            }
            println("E-ink mode disabled - hiding page navigation buttons")
        }
    }

    private fun pageUp() {
        val recyclerView = binding.recyclerViewAgenda

        // Calculate 75% of the visible area height
        val visibleHeight = recyclerView.height - recyclerView.paddingTop - recyclerView.paddingBottom
        val scrollDistance = (visibleHeight * 0.75).toInt()

        // Jump scroll up by 75% of screen height (no animation for e-ink)
        recyclerView.scrollBy(0, -scrollDistance)

        println("Page Up: jumped up by ${scrollDistance}px (75% of ${visibleHeight}px)")
    }

    private fun pageDown() {
        val recyclerView = binding.recyclerViewAgenda

        // Calculate 75% of the visible area height
        val visibleHeight = recyclerView.height - recyclerView.paddingTop - recyclerView.paddingBottom
        val scrollDistance = (visibleHeight * 0.75).toInt()

        // Jump scroll down by 75% of screen height (no animation for e-ink)
        recyclerView.scrollBy(0, scrollDistance)

        println("Page Down: jumped down by ${scrollDistance}px (75% of ${visibleHeight}px)")
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