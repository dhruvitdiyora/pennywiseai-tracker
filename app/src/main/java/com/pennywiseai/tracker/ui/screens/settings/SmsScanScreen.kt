package com.pennywiseai.tracker.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.icons.iconsax.Calendar
import com.pennywiseai.tracker.ui.icons.iconsax.Danger
import com.pennywiseai.tracker.ui.icons.iconsax.Iconsax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.orange_dark
import com.pennywiseai.tracker.ui.theme.orange_light
import com.pennywiseai.tracker.ui.theme.teal_dark
import com.pennywiseai.tracker.ui.theme.teal_light
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsScanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUnrecognizedSms: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val months by viewModel.smsScanMonths.collectAsStateWithLifecycle(initialValue = 3)
    val allTime by viewModel.smsScanAllTime.collectAsStateWithLifecycle(initialValue = false)
    val customDate by viewModel.smsScanCustomDate.collectAsStateWithLifecycle(initialValue = null)
    val useCustomDate by viewModel.smsScanUseCustomDate.collectAsStateWithLifecycle(initialValue = false)
    var showPeriodDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    val haze = remember { HazeState() }
    val subtitle = when {
        allTime -> "Scan all SMS messages"
        useCustomDate -> "Scan from ${customDate?.let(::formatScanDate) ?: "a custom start date"} to today"
        else -> "Scan last $months months"
    }
    Scaffold(topBar = {
        CustomTitleTopAppBar(title = "SMS & scanning", scrollBehaviorSmall = scroll,
            scrollBehaviorLarge = scroll, hazeState = haze, hasBackButton = true,
            navigationContent = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().hazeSource(haze).background(MaterialTheme.colorScheme.background)
            .overScrollVertical().verticalScroll(rememberScrollState()).padding(padding).padding(Dimensions.Padding.content),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.sm)) {
            SectionHeaderV2("Scanning")
            SettingsGroup {
                SettingsNavItem(Iconsax.Calendar, teal_light, teal_dark, "SMS Scan Period", subtitle,
                    { showPeriodDialog = true }, ListItemPosition.Top,
                    trailingText = if (allTime) "All time" else if (useCustomDate) "Custom" else "$months mo")
                SettingsNavItem(Iconsax.Danger, orange_light, orange_dark, "Unrecognized SMS",
                    "View and report unsupported bank messages", onNavigateToUnrecognizedSms, ListItemPosition.Bottom)
            }
        }
    }
    if (showPeriodDialog) AlertDialog(onDismissRequest = { showPeriodDialog = false }, title = { Text("SMS Scan Period") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            listOf(-1, -2, 1, 2, 3, 6, 12, 24).forEach { option ->
                val selected = when (option) {
                    -1 -> allTime
                    -2 -> useCustomDate && !allTime
                    else -> months == option && !allTime && !useCustomDate
                }
                androidx.compose.foundation.layout.Row(Modifier.selectable(selected) {
                    if (option == -1) viewModel.updateSmsScanAllTime(true) else if (option == -2) {
                        showPeriodDialog = false; showDatePicker = true
                    } else {
                        viewModel.updateSmsScanMonths(option); viewModel.updateSmsScanAllTime(false)
                    }
                    showPeriodDialog = false
                }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected, onClick = null)
                    Text(if (option == -1) "All Time" else if (option == -2) "Custom date" else if (option == 1) "1 month" else if (option == 24) "2 years" else "$option months")
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { showPeriodDialog = false }) { Text("Cancel") } })
    if (showDatePicker) {
        val today = java.time.LocalDate.now().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val initial = customDate ?: java.time.LocalDate.now().minusMonths(3).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val picker = rememberDatePickerState(initialSelectedDateMillis = initial, selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
        })
        DatePickerDialog(onDismissRequest = { showDatePicker = false; showPeriodDialog = true }, confirmButton = {
            TextButton(onClick = { picker.selectedDateMillis?.let(viewModel::updateSmsScanCustomDate); showDatePicker = false }) { Text("OK") }
        }, dismissButton = { TextButton(onClick = { showDatePicker = false; showPeriodDialog = true }) { Text("Cancel") } }) {
            DatePicker(picker)
        }
    }
}

private fun formatScanDate(millis: Long): String = java.time.Instant.ofEpochMilli(millis)
    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
