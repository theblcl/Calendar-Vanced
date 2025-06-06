package com.example.calendarapp.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.calendarapp.databinding.FragmentSettingsBinding
import com.example.calendarapp.utils.EPaperUtils

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val THEME_PREFS = "theme_settings"
        private const val KEY_THEME_MODE = "theme_mode"

        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEPaperToggle()
        setupThemeSelector()
    }

    private fun setupEPaperToggle() {
        // Set current state
        binding.switchEpaperMode.isChecked = EPaperUtils.isEPaperMode(requireContext())

        // Show device info
        val isEPaperDevice = EPaperUtils.detectEPaperDevice()
        binding.textEpaperStatus.text = if (isEPaperDevice) {
            "✓ E-paper device detected"
        } else {
            "📱 Regular device"
        }

        // Handle toggle
        binding.switchEpaperMode.setOnCheckedChangeListener { _, isChecked ->
            EPaperUtils.setEPaperMode(requireContext(), isChecked)

            // Show restart message
            binding.textRestartHint.visibility = View.VISIBLE
            binding.textRestartHint.text = "Restart app to apply e-paper optimizations"
        }
    }

    private fun setupThemeSelector() {
        // Get current theme mode
        val currentTheme = getCurrentThemeMode()
        updateThemeDisplay(currentTheme)

        // Handle theme selection click
        binding.layoutTheme.setOnClickListener {
            showThemeSelectionDialog()
        }
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf("Follow System", "Light Mode", "Dark Mode")
        val currentTheme = getCurrentThemeMode()

        AlertDialog.Builder(requireContext())
            .setTitle("Choose Theme")
            .setSingleChoiceItems(themes, currentTheme) { dialog, which ->
                setThemeMode(which)
                updateThemeDisplay(which)
                applyTheme(which)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getCurrentThemeMode(): Int {
        val prefs = requireContext().getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM)
    }

    private fun setThemeMode(mode: Int) {
        val prefs = requireContext().getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
    }

    private fun updateThemeDisplay(mode: Int) {
        val themeText = when (mode) {
            THEME_SYSTEM -> "Follow System"
            THEME_LIGHT -> "Light Mode"
            THEME_DARK -> "Dark Mode"
            else -> "Follow System"
        }
        binding.textTheme.text = themeText
    }

    private fun applyTheme(mode: Int) {
        val nightMode = when (mode) {
            THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}