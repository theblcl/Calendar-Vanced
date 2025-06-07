// Complete cleaned MainActivity.kt
package com.example.calendarapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.calendarapp.databinding.ActivityMainBinding
import com.example.calendarapp.fragments.*
import com.example.calendarapp.utils.EPaperUtils
import com.example.calendarapp.viewmodel.CalendarViewModel
import com.example.calendarapp.viewmodel.CalendarViewModelFactory
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: CalendarViewModel
    private lateinit var toggle: ActionBarDrawerToggle

    companion object {
        private const val CALENDAR_PERMISSION_REQUEST_CODE = 1001
        private const val CALENDAR_MENU_ID_OFFSET = 1000

        // Menu IDs
        private const val VIEW_GROUP_ID = 100
        private const val NAV_AGENDA_ID = 101
        private const val NAV_DAY_ID = 102
        private const val NAV_THREE_DAY_ID = 103
        private const val NAV_WEEK_ID = 104
        private const val NAV_MONTH_ID = 105
        private const val NAV_SYNC_ID = 106
        private const val NAV_SETTINGS_ID = 107
    }

    private var lastCalendarSetupTime = 0L
    private var lastCalendarCount = 0

    private fun setupViewNavigation() {
        val menu = binding.navView.menu

        println("=== Setting up view navigation ===")

        // Clear any existing items first
        menu.clear()

        // Add view options (radio button behavior - only one selected)
        val viewGroup = menu.addSubMenu(0, VIEW_GROUP_ID, 0, "Views").also { submenu ->
            submenu.setGroupCheckable(0, true, true) // Single selection
        }

        // Add all view options
        viewGroup.add(0, NAV_AGENDA_ID, 0, "Agenda").apply {
            setIcon(R.drawable.ic_calendar_today)
            isCheckable = true
            isChecked = true // Default selection
        }

        viewGroup.add(0, NAV_DAY_ID, 1, "Day").apply {
            setIcon(R.drawable.ic_calendar_today)
            isCheckable = true
        }

        viewGroup.add(0, NAV_THREE_DAY_ID, 2, "3 Days").apply {
            setIcon(R.drawable.ic_calendar_today)
            isCheckable = true
        }

        viewGroup.add(0, NAV_WEEK_ID, 3, "Week").apply {
            setIcon(R.drawable.ic_calendar_today)
            isCheckable = true
        }

        viewGroup.add(0, NAV_MONTH_ID, 4, "Month").apply {
            setIcon(R.drawable.ic_calendar_today)
            isCheckable = true
        }

        // Add actions
        menu.add(0, NAV_SYNC_ID, 10, "Sync").apply {
            setIcon(android.R.drawable.ic_popup_sync)
            isCheckable = false
        }

        menu.add(0, NAV_SETTINGS_ID, 11, "Settings").apply {
            setIcon(android.R.drawable.ic_menu_preferences)
            isCheckable = false
        }

        println("View navigation setup complete")
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            NAV_AGENDA_ID -> {
                showFragment(AgendaViewFragment())
                supportActionBar?.title = "Agenda"
            }
            NAV_DAY_ID -> {
                showFragment(DayViewFragment())
                supportActionBar?.title = "Day"
            }
            NAV_THREE_DAY_ID -> {
                showFragment(ThreeDayViewFragment())
                supportActionBar?.title = "3 Days"
            }
            NAV_WEEK_ID -> {
                showFragment(WeekViewFragment())
                supportActionBar?.title = "Week"
            }
            NAV_MONTH_ID -> {
                showFragment(MonthViewFragment())
                supportActionBar?.title = "Month"
            }
            NAV_SYNC_ID -> {
                viewModel.syncCalendar()
                Toast.makeText(this, "Syncing calendar...", Toast.LENGTH_SHORT).show()
            }
            NAV_SETTINGS_ID -> {
                showFragment(SettingsFragment())
                supportActionBar?.title = "Settings"
            }
            else -> {
                // Handle calendar visibility toggles
                val calendarIndex = item.itemId - CALENDAR_MENU_ID_OFFSET
                if (calendarIndex >= 0) {
                    val calendars = viewModel.availableCalendars.value
                    if (calendars != null && calendarIndex < calendars.size) {
                        val calendar = calendars[calendarIndex]

                        // Toggle the calendar visibility
                        viewModel.toggleCalendarVisibility(calendar.displayName)

                        // Update the menu item check state and icon
                        item.isChecked = viewModel.isCalendarVisible(calendar.displayName)

                        // Update icon
                        if (item.isChecked) {
                            item.setIcon(R.drawable.checkbox_checked_epaper)
                        } else {
                            item.setIcon(R.drawable.checkbox_unchecked_epaper)
                        }

                        println("Toggled calendar: ${calendar.displayName} -> visible: ${item.isChecked}")

                        // Don't close drawer for calendar toggles
                        return true
                    }
                }
            }
        }

        // Close drawer for view changes
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply themes first
        applySelectedTheme()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check calendar permissions
        if (checkCalendarPermissions()) {
            initializeApp()
        } else {
            requestCalendarPermissions()
        }
    }

    private fun applySelectedTheme() {
        // First check if e-paper mode is enabled
        if (EPaperUtils.isEPaperMode(this)) {
            setTheme(R.style.Theme_CalendarApp_EPaper)
            return
        }

        // Apply the selected light/dark theme
        val themePrefs = getSharedPreferences("theme_settings", Context.MODE_PRIVATE)
        val themeMode = themePrefs.getInt("theme_mode", 0) // 0 = system, 1 = light, 2 = dark

        when (themeMode) {
            0 -> { // Follow system
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                setTheme(R.style.Theme_CalendarApp)
            }
            1 -> { // Light mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                setTheme(R.style.Theme_CalendarApp)
            }
            2 -> { // Dark mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                setTheme(R.style.Theme_CalendarApp)
            }
        }
    }

    private fun checkCalendarPermissions(): Boolean {
        val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
        val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
        return readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCalendarPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            ),
            CALENDAR_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CALENDAR_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                initializeApp()
            } else {
                Toast.makeText(
                    this,
                    "Calendar permissions are required to use this app",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun initializeApp() {
        // Initialize ViewModel
        val factory = CalendarViewModelFactory(this)
        viewModel = ViewModelProvider(this, factory)[CalendarViewModel::class.java]

        setupToolbar()
        setupNavigationDrawer()
        setupViewNavigation()
        setupFAB()
        observeCalendars()

        // Start with Agenda view
        showFragment(AgendaViewFragment())
        supportActionBar?.title = "Agenda"
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Agenda"

        binding.todayCalendarIcon.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment is AgendaViewFragment) {
                currentFragment.scrollToToday()
            }
        }

        updateTodayIcon()
    }

    private fun setupFAB() {
        binding.fabAddEvent.setOnClickListener {
            showEventDetail()
        }
    }

    private fun updateTodayIcon() {
        val today = java.time.LocalDate.now()
        binding.todayDateText.text = today.dayOfMonth.toString()
    }

    private fun setupNavigationDrawer() {
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)
    }

    private fun observeCalendars() {
        viewModel.availableCalendars.observe(this) { calendars ->
            setupCalendarCheckboxes(calendars)
        }
    }

    private fun setupCalendarCheckboxes(calendars: List<com.example.calendarapp.data.CalendarInfo>) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastCalendarSetupTime < 1000 && calendars.size == lastCalendarCount) {
            println("=== SKIPPING: setupCalendarCheckboxes called too soon (${currentTime - lastCalendarSetupTime}ms ago)")
            return
        }

        lastCalendarSetupTime = currentTime
        lastCalendarCount = calendars.size

        val menu = binding.navView.menu

        println("=== DEBUG: Setting up calendar checkboxes ===")
        println("Method called with ${calendars.size} calendars at time $currentTime")

        // Remove any existing calendar items
        var removedCount = 0
        for (i in menu.size() - 1 downTo 0) {
            val item = menu.getItem(i)
            if (item.itemId >= CALENDAR_MENU_ID_OFFSET || item.itemId == 999) {
                println("Removing existing item: ${item.title} (ID: ${item.itemId})")
                menu.removeItem(item.itemId)
                removedCount++
            }
        }
        println("Removed $removedCount existing calendar items")

        if (calendars.isEmpty()) {
            println("No calendars to add")
            return
        }

        val groupedCalendars = calendars.groupBy { calendar ->
            extractCalendarSource(calendar)
        }

        println("=== FINAL GROUPING ===")
        groupedCalendars.forEach { (source, cals) ->
            println("Group '$source': ${cals.size} calendars")
            cals.forEach { cal ->
                println("  - ${cal.displayName} (color: ${cal.color})")
            }
        }

        var itemIndex = 0
        var orderIndex = 50

        groupedCalendars.forEach { (source, calendarGroup) ->
            // Add group header
            val headerId = 999 - itemIndex
            val headerItem = menu.add(0, headerId, orderIndex++, "── $source ──")
            headerItem.isEnabled = false
            println("Added group header: '$source' (ID: $headerId)")

            // Add calendars in this group with colored text
            // Add calendars in this group with colored text matching event accent bars
            calendarGroup.forEach { calendar ->
                val menuItem = menu.add(
                    Menu.NONE,
                    CALENDAR_MENU_ID_OFFSET + itemIndex,
                    orderIndex++,
                    "  ${calendar.displayName}"
                )

                menuItem.apply {
                    isCheckable = true
                    isChecked = viewModel.isCalendarVisible(calendar.displayName)

                    // Create colored text span using THE EXACT SAME COLOR as event accent bars
                    val spannable = android.text.SpannableString("  ${calendar.displayName}")
                    try {
                        // This MUST be the same color that appears in event.calendarColor
                        val color = android.graphics.Color.parseColor(calendar.color)
                        val colorSpan = android.text.style.ForegroundColorSpan(color)
                        spannable.setSpan(colorSpan, 0, spannable.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        title = spannable

                        println("Navigation menu: '${calendar.displayName}' using color: ${calendar.color}")
                    } catch (e: Exception) {
                        println("❌ Failed to parse color '${calendar.color}' for calendar '${calendar.displayName}'")
                        title = "  ${calendar.displayName}"
                    }

                    if (isChecked) {
                        setIcon(R.drawable.checkbox_checked_epaper)
                    } else {
                        setIcon(R.drawable.checkbox_unchecked_epaper)
                    }
                }

                itemIndex++
            }
        }

        println("Calendar setup complete: $itemIndex calendars in ${groupedCalendars.size} groups")
    }

    private fun extractCalendarSource(calendar: com.example.calendarapp.data.CalendarInfo): String {
        println("=== DEBUG: Extracting source from account info ===")
        println("Calendar: '${calendar.displayName}'")
        println("Account name (stored in url): '${calendar.url}'")
        println("Description (accountType|ownerAccount|androidId): '${calendar.description}'")

        val accountName = calendar.url  // We stored account name in url field
        val descriptionParts = calendar.description.split("|")
        val accountType = if (descriptionParts.size > 0) descriptionParts[0] else ""
        val ownerAccount = if (descriptionParts.size > 1) descriptionParts[1] else ""

        println("Parsed - accountName: '$accountName', accountType: '$accountType', ownerAccount: '$ownerAccount'")

        val result = when {
            // Use account name if available and meaningful
            accountName.isNotEmpty() && accountName != "null" -> {
                when {
                    // If it's an email, use the part before @ but normalize it
                    accountName.contains("@") -> {
                        val emailPrefix = accountName.substringBefore("@").lowercase()
                        println("Using normalized email prefix: '$emailPrefix'")
                        emailPrefix.replaceFirstChar { it.titlecase() }
                    }
                    // If it's already a clean name, normalize it
                    else -> {
                        val normalized = accountName.lowercase()
                        println("Using normalized account name: '$normalized'")
                        normalized.replaceFirstChar { it.titlecase() }
                    }
                }
            }

            // Fallback to owner account with normalization
            ownerAccount.isNotEmpty() && ownerAccount != "null" -> {
                val result = if (ownerAccount.contains("@")) {
                    ownerAccount.substringBefore("@").lowercase()
                } else {
                    ownerAccount.lowercase()
                }
                println("Using normalized owner account: '$result'")
                result.replaceFirstChar { it.titlecase() }
            }

            // Use account type as fallback with normalization
            accountType.isNotEmpty() && accountType != "null" -> {
                val cleanType = when (accountType.lowercase()) {
                    "com.google" -> "Google"
                    "com.google.android.gm.exchange" -> "Exchange"
                    "local" -> "Local"
                    else -> accountType.substringAfterLast(".").lowercase().replaceFirstChar { it.titlecase() }
                }
                println("Using normalized account type: '$cleanType'")
                cleanType
            }

            // Final fallback
            else -> {
                println("Using default fallback")
                "Device Calendars"
            }
        }

        println("Final result: '$result'")
        return result
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun showEventDetail(eventId: String? = null) {
        val dialog = AndroidEventDetailFragment.newInstance(eventId)
        dialog.show(supportFragmentManager, "AndroidEventDetailFragment")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        updateTodayIcon()

        if (::viewModel.isInitialized) {
            viewModel.loadEvents()
            viewModel.loadCalendars()
        }
    }
}