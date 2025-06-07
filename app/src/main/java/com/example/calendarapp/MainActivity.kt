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
    private var calendarsLoaded = false
    private var isSettingUpCalendars = false // Prevent concurrent setup

    // Track calendar menu items separately
    private val calendarMenuItems = mutableMapOf<String, MenuItem>()

    private fun setupViewNavigation() {
        val menu = binding.navView.menu

        println("=== Setting up view navigation ===")

        // Clear any existing items first
        menu.clear()

        // Add view options in a separate group that allows single selection
        val viewGroup = menu.addSubMenu(0, VIEW_GROUP_ID, 0, "Views").also { submenu ->
            submenu.setGroupCheckable(0, true, true) // Single selection for views only
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

        // Add actions in a separate non-checkable group
        menu.add(Menu.NONE, NAV_SYNC_ID, 10, "Sync").apply {
            setIcon(android.R.drawable.ic_popup_sync)
            isCheckable = false
        }

        menu.add(Menu.NONE, NAV_SETTINGS_ID, 11, "Settings").apply {
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
                return true // Don't close drawer
            }
            NAV_SETTINGS_ID -> {
                showFragment(SettingsFragment())
                supportActionBar?.title = "Settings"
            }
            else -> {
                // Handle calendar visibility toggles
                val calendarName = calendarMenuItems.entries.find { it.value == item }?.key
                if (calendarName != null) {
                    println("=== Calendar Toggle Debug ===")
                    println("Tapped menu item with ID: ${item.itemId}")
                    println("Mapped to calendar: '$calendarName'")
                    println("Current visibility: ${viewModel.isCalendarVisible(calendarName)}")

                    // Toggle the calendar visibility in ViewModel
                    viewModel.toggleCalendarVisibility(calendarName)

                    // Return true to keep drawer open for calendar toggles
                    return true
                } else {
                    println("Could not find calendar for menu item ID: ${item.itemId}")
                    return true
                }
            }
        }

        // Close drawer for view changes only
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun updateCalendarCheckbox(calendarName: String, isVisible: Boolean) {
        val menuItem = calendarMenuItems[calendarName]
        if (menuItem != null) {
            // Update the checkbox icon only
            if (isVisible) {
                menuItem.setIcon(R.drawable.checkbox_checked_epaper)
            } else {
                menuItem.setIcon(R.drawable.checkbox_unchecked_epaper)
            }
            println("Updated checkbox for '$calendarName': $isVisible")
        }
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
        // Observe calendar visibility changes to update checkboxes
        viewModel.visibleCalendars.observe(this) { visibleCalendars ->
            if (calendarsLoaded) {
                println("=== Visibility changed, updating checkboxes ===")
                println("Visible calendars: $visibleCalendars")
                updateAllCalendarCheckboxes(visibleCalendars)
            }
        }

        // Observe available calendars to set up the menu
        viewModel.availableCalendars.observe(this) { calendars ->
            setupCalendarCheckboxes(calendars)
            calendarsLoaded = true
        }

        // Observe events to see when they change
        viewModel.events.observe(this) { events ->
            println("=== Events updated: ${events.size} events visible ===")
        }
    }

    private fun updateAllCalendarCheckboxes(visibleCalendars: Set<String>) {
        println("=== updateAllCalendarCheckboxes called ===")
        println("Visible calendars: $visibleCalendars")
        println("Calendar menu items count: ${calendarMenuItems.size}")

        calendarMenuItems.forEach { (calendarName, menuItem) ->
            val isVisible = visibleCalendars.contains(calendarName)

            // Use modern, friendly system icons
            if (isVisible) {
                menuItem.setIcon(android.R.drawable.ic_menu_send) // Calendar/agenda icon for enabled
                println("✅ Set AGENDA ICON for '$calendarName'")
            } else {
                menuItem.setIcon(android.R.drawable.ic_menu_close_clear_cancel) // X icon for disabled
                println("❌ Set X ICON for '$calendarName'")
            }
        }

        // Force the navigation view to refresh
        binding.navView.invalidate()
    }

    private fun setupCalendarCheckboxes(calendars: List<com.example.calendarapp.data.CalendarInfo>) {
        // Prevent concurrent setup calls
        if (isSettingUpCalendars) {
            println("=== SKIPPING: Calendar setup already in progress")
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCalendarSetupTime < 500) { // Reduced from 1000ms to 500ms
            println("=== SKIPPING: setupCalendarCheckboxes called too soon")
            return
        }

        isSettingUpCalendars = true
        lastCalendarSetupTime = currentTime

        val menu = binding.navView.menu

        println("=== DEBUG: Setting up calendar checkboxes ===")
        println("Method called with ${calendars.size} calendars")

        // Clear existing calendar items and map MORE THOROUGHLY
        calendarMenuItems.clear()

        // Remove all items that are not core navigation items
        val itemsToRemove = mutableListOf<MenuItem>()
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val itemId = item.itemId

            // Keep only core navigation items (Views, Sync, Settings)
            if (itemId != VIEW_GROUP_ID &&
                itemId != NAV_AGENDA_ID && itemId != NAV_DAY_ID &&
                itemId != NAV_THREE_DAY_ID && itemId != NAV_WEEK_ID &&
                itemId != NAV_MONTH_ID && itemId != NAV_SYNC_ID &&
                itemId != NAV_SETTINGS_ID) {
                itemsToRemove.add(item)
            }
        }

        itemsToRemove.forEach { item ->
            menu.removeItem(item.itemId)
            println("Removed menu item: '${item.title}' (ID: ${item.itemId})")
        }

        if (calendars.isEmpty()) {
            println("No calendars to add")
            isSettingUpCalendars = false
            return
        }

        // Group calendars by source
        val groupedCalendars = calendars.groupBy { calendar ->
            extractCalendarSource(calendar)
        }

        var orderIndex = 50
        var headerIdCounter = 9000 // Use high numbers for headers to avoid conflicts

        groupedCalendars.forEach { (source, calendarGroup) ->
            // Add group header (not clickable) with unique ID
            val headerId = headerIdCounter++
            menu.add(Menu.NONE, headerId, orderIndex++, "── $source ──").apply {
                isEnabled = false
                isCheckable = false
                println("Added header: '── $source ──' with ID: $headerId")
            }

            // Add calendars in this group
            calendarGroup.forEach { calendar ->
                // Use a unique ID based on calendar name hash to avoid conflicts
                val menuItemId = CALENDAR_MENU_ID_OFFSET + Math.abs(calendar.displayName.hashCode())
                val menuItem = menu.add(
                    Menu.NONE, // NO GROUP - This prevents the highlighting issue
                    menuItemId,
                    orderIndex++,
                    "  ${calendar.displayName}"
                )

                menuItem.apply {
                    // CRITICAL: Do not make calendar items checkable in the traditional sense
                    // We'll handle the visual state manually with icons
                    isCheckable = false

                    // Check current visibility from ViewModel
                    val isVisible = viewModel.isCalendarVisible(calendar.displayName)

                    // Use modern, friendly system icons
                    if (isVisible) {
                        setIcon(android.R.drawable.ic_menu_agenda) // Calendar/agenda icon for enabled
                        println("Set AGENDA ICON for '${calendar.displayName}'")
                    } else {
                        setIcon(android.R.drawable.ic_menu_close_clear_cancel) // X icon for disabled
                        println("Set X ICON for '${calendar.displayName}'")
                    }

                    println("Calendar '${calendar.displayName}': visible=$isVisible, menuItemId=$menuItemId")

                    // Apply calendar color to text
                    val spannable = android.text.SpannableString("  ${calendar.displayName}")
                    try {
                        val color = android.graphics.Color.parseColor(calendar.color)
                        val colorSpan = android.text.style.ForegroundColorSpan(color)
                        spannable.setSpan(colorSpan, 0, spannable.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        title = spannable
                    } catch (e: Exception) {
                        title = "  ${calendar.displayName}"
                    }
                }

                // Store in our map for easy updates - KEY IS CALENDAR NAME
                calendarMenuItems[calendar.displayName] = menuItem
                println("Added calendar: '${calendar.displayName}' -> Menu ID: $menuItemId")
            }
        }

        println("Calendar setup complete: ${calendarMenuItems.size} calendars")
        println("Final calendar menu mapping:")
        calendarMenuItems.forEach { (name, item) ->
            println("  '$name' -> Menu ID: ${item.itemId}")
        }

        isSettingUpCalendars = false
    }

    private fun extractCalendarSource(calendar: com.example.calendarapp.data.CalendarInfo): String {
        val accountName = calendar.url  // We stored account name in url field
        val descriptionParts = calendar.description.split("|")
        val accountType = if (descriptionParts.size > 0) descriptionParts[0] else ""
        val ownerAccount = if (descriptionParts.size > 1) descriptionParts[1] else ""

        return when {
            // Use account name if available and meaningful
            accountName.isNotEmpty() && accountName != "null" -> {
                when {
                    // If it's an email, use the part before @ but normalize it
                    accountName.contains("@") -> {
                        accountName.substringBefore("@").lowercase().replaceFirstChar { it.titlecase() }
                    }
                    // If it's already a clean name, normalize it
                    else -> {
                        accountName.lowercase().replaceFirstChar { it.titlecase() }
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
                result.replaceFirstChar { it.titlecase() }
            }

            // Use account type as fallback with normalization
            accountType.isNotEmpty() && accountType != "null" -> {
                when (accountType.lowercase()) {
                    "com.google" -> "Google"
                    "com.google.android.gm.exchange" -> "Exchange"
                    "local" -> "Local"
                    else -> accountType.substringAfterLast(".").lowercase().replaceFirstChar { it.titlecase() }
                }
            }

            // Final fallback
            else -> "Device Calendars"
        }
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

    override fun onResume() {
        super.onResume()
        updateTodayIcon()

        if (::viewModel.isInitialized) {
            viewModel.loadEvents()
            viewModel.loadCalendars()
        }
    }
}