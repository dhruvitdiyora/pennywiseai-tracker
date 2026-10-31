package com.pennywiseai.tracker.ui.screens.settings.webhooks

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.pennywiseai.tracker.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.data.database.entity.*
import com.pennywiseai.tracker.data.webhook.WebhookHeader
import com.pennywiseai.tracker.data.webhook.WebhookSyncReason
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.theme.Spacing

@Composable
fun WebhooksScreen(onNavigateBack: () -> Unit, viewModel: WebhooksViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isPro by viewModel.isProEntitled.collectAsStateWithLifecycle()
    var showUpgrade by remember { mutableStateOf(false) }
    val listState = remember(state.editor != null, state.editor?.id, state.historyId) { LazyListState() }
    var deleteProfile by remember { mutableStateOf<WebhookProfileEntity?>(null) }
    val back: () -> Unit = {
        when {
            state.editor != null -> viewModel.closeEditor()
            state.historyId != null -> viewModel.closeHistory()
            else -> onNavigateBack()
        }
    }
    BackHandler(state.editor != null || state.historyId != null, onBack = back)
    val title = when {
        state.editor != null -> if (state.editor?.id == null) stringResource(R.string.webhooks_add_title) else stringResource(R.string.webhooks_edit_title)
        state.historyId != null -> stringResource(R.string.webhooks_history_title)
        else -> stringResource(R.string.webhooks_title)
    }
    PennyWiseScaffold(title = title, navigationIcon = {
        IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.webhooks_back)) }
    }) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.smd),
            contentPadding = PaddingValues(vertical = Spacing.md)
        ) {
            if (state.editor == null) {
                state.message?.let { message -> item { Text(message.asString(), style = MaterialTheme.typography.bodyMedium) } }
            }
            when {
                !isPro -> {
                    item { Text(stringResource(R.string.webhooks_pro_body)) }
                    item { Button(onClick = { showUpgrade = true }) { Text(stringResource(R.string.webhooks_unlock_pro)) } }
                }
                state.editor != null -> {
                    item { WebhookEditor(state.editor!!, state.intervalHours, viewModel::updateEditor) }
                    state.message?.let { message -> item { Text(message.asString(), color = MaterialTheme.colorScheme.error) } }
                    item {
                        Button(onClick = viewModel::save, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.webhooks_save))
                        }
                    }
                }
                state.historyId != null -> {
                    item {
                        Text(state.profiles.firstOrNull { it.id == state.historyId }?.name.orEmpty(),
                            style = MaterialTheme.typography.titleMedium)
                    }
                    if (state.logs.isEmpty()) item { Text(stringResource(R.string.webhooks_history_empty)) }
                    items(state.logs, key = { it.id }) { log ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Text(if (log.status == WebhookLogStatus.SUCCESS) stringResource(R.string.webhooks_log_delivered) else stringResource(R.string.webhooks_log_failed),
                                    color = if (log.status == WebhookLogStatus.SUCCESS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.titleSmall)
                                Text(log.message)
                                Text(stringResource(R.string.webhooks_log_meta, stringResource(log.syncReason.labelRes()), log.createdAt.toLocalDate().toString(), log.createdAt.toLocalTime().withNano(0).toString()),
                                    style = MaterialTheme.typography.bodySmall)
                                if (log.batchCount > 1) Text(pluralStringResource(R.plurals.webhooks_log_batches, log.batchCount, log.batchCount), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                else -> {
                    item {
                        Text(stringResource(R.string.webhooks_intro))
                    }
                    item { Button(onClick = viewModel::newProfile, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.webhooks_add_title)) } }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedTextField(state.interval, viewModel::setInterval, Modifier.weight(1f),
                                label = { Text(stringResource(R.string.webhooks_interval_label)) }, singleLine = true,
                                supportingText = { Text(stringResource(R.string.webhooks_interval_hint)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            TextButton(onClick = viewModel::saveInterval, enabled = !state.busy) { Text(stringResource(R.string.webhooks_apply)) }
                        }
                    }
                    item {
                        OutlinedButton(onClick = { viewModel.syncNow() }, enabled = !state.busy && state.profiles.any { it.enabled },
                            modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.webhooks_sync_all_now)) }
                    }
                    if (state.profiles.isEmpty()) item { Text(stringResource(R.string.webhooks_empty)) }
                    items(state.profiles, key = { it.id }) { profile ->
                        WebhookProfileCard(profile, !state.busy,
                            onEdit = { viewModel.edit(profile) },
                            onEnabled = { viewModel.setEnabled(profile.id, it) },
                            onTest = { viewModel.sendTest(profile.id) },
                            onSync = { viewModel.syncNow(profile.id) },
                            onHistory = { viewModel.showHistory(profile.id) },
                            onDelete = { deleteProfile = profile })
                    }
                }
            }
        }
    }
    if (showUpgrade) com.pennywiseai.tracker.presentation.paywall.UpgradeSheet(onDismiss = { showUpgrade = false })
    deleteProfile?.let { profile ->
        AlertDialog(onDismissRequest = { deleteProfile = null }, title = { Text(stringResource(R.string.webhooks_delete_title)) },
            text = { Text(stringResource(R.string.webhooks_delete_body, profile.name)) },
            confirmButton = { TextButton(onClick = { viewModel.delete(profile.id); deleteProfile = null }) { Text(stringResource(R.string.webhooks_delete)) } },
            dismissButton = { TextButton(onClick = { deleteProfile = null }) { Text(stringResource(R.string.webhooks_cancel)) } })
    }
}

@Composable
private fun WebhookProfileCard(
    profile: WebhookProfileEntity, enabled: Boolean, onEdit: () -> Unit,
    onEnabled: (Boolean) -> Unit, onTest: () -> Unit, onSync: () -> Unit,
    onHistory: () -> Unit, onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(profile.name, style = MaterialTheme.typography.titleMedium)
                    Text(runCatching { java.net.URI(profile.url).host }.getOrNull().orEmpty(),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${profile.currency} · ${stringResource(profile.rangePreset.labelRes())}", style = MaterialTheme.typography.bodySmall)
                }
                Switch(profile.enabled, onEnabled, enabled = enabled)
            }
            Text(if (profile.enabled) stringResource(R.string.webhooks_auto_sync_on) else stringResource(R.string.webhooks_auto_sync_off), style = MaterialTheme.typography.bodySmall)
            profile.lastError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            profile.lastSyncedAt?.let { Text(stringResource(R.string.webhooks_last_sync, it.toLocalDate().toString(), it.toLocalTime().withNano(0).toString()), style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                TextButton(onClick = onEdit, enabled = enabled) { Text(stringResource(R.string.webhooks_edit)) }
                TextButton(onClick = onTest, enabled = enabled) { Text(stringResource(R.string.webhooks_test)) }
                TextButton(onClick = onSync, enabled = enabled && profile.enabled) { Text(stringResource(R.string.webhooks_sync)) }
                TextButton(onClick = onHistory) { Text(stringResource(R.string.webhooks_history)) }
                IconButton(onClick = onDelete, enabled = enabled) { Icon(Icons.Default.Delete, stringResource(R.string.webhooks_delete_named, profile.name)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WebhookEditor(editor: WebhookEditorState, interval: Int, update: ((WebhookEditorState) -> WebhookEditorState) -> Unit) {
    var rangeMenu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.smd)) {
        OutlinedTextField(editor.name, { value -> update { it.copy(name = value) } }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.webhooks_field_name)) }, singleLine = true)
        OutlinedTextField(editor.url, { value -> update { it.copy(url = value) } }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.webhooks_field_url)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
        Text(if (editor.enabled) pluralStringResource(R.plurals.webhooks_editor_auto_notice, interval, interval)
            else stringResource(R.string.webhooks_editor_manual_notice),
            style = MaterialTheme.typography.bodyMedium)
        if (editor.url.trim().startsWith("http://", true)) {
            Text(stringResource(R.string.webhooks_http_warning), color = MaterialTheme.colorScheme.error)
        }
        OutlinedTextField(editor.currency, { value -> update { it.copy(currency = value.uppercase()) } }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.webhooks_field_currency)) }, singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.webhooks_enable_auto), Modifier.weight(1f))
            Switch(editor.enabled, { value -> update { it.copy(enabled = value) } })
        }
        Text(stringResource(R.string.webhooks_data_to_send), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            WebhookDataType.entries.forEach { type ->
                FilterChip(type in editor.types, onClick = { update {
                    it.copy(types = if (type in it.types) it.types - type else it.types + type)
                } }, label = { Text(stringResource(type.labelRes())) })
            }
        }
        Box {
            OutlinedButton(onClick = { rangeMenu = true }) { Text(stringResource(R.string.webhooks_range, stringResource(editor.range.labelRes()))) }
            DropdownMenu(rangeMenu, onDismissRequest = { rangeMenu = false }) {
                WebhookRangePreset.entries.forEach { range ->
                    DropdownMenuItem(text = { Text(stringResource(range.labelRes())) }, onClick = {
                        update { it.copy(range = range) }; rangeMenu = false
                    })
                }
            }
        }
        Text(stringResource(R.string.webhooks_range_note),
            style = MaterialTheme.typography.bodySmall)
        if (editor.range == WebhookRangePreset.CUSTOM) {
            OutlinedTextField(editor.start, { value -> update { it.copy(start = value) } }, Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.webhooks_field_start)) }, singleLine = true)
            OutlinedTextField(editor.end, { value -> update { it.copy(end = value) } }, Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.webhooks_field_end)) }, singleLine = true)
        }
        Text(stringResource(R.string.webhooks_headers_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.webhooks_headers_note),
            style = MaterialTheme.typography.bodySmall)
        editor.headers.forEachIndexed { index, header ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(header.key, { value -> update { it.copy(headers = it.headers.toMutableList().apply {
                    this[index] = this[index].copy(key = value)
                }) } }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.webhooks_header_name)) }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(header.value, { value -> update { it.copy(headers = it.headers.toMutableList().apply {
                        this[index] = this[index].copy(value = value)
                    }) } }, Modifier.weight(1f), label = { Text(stringResource(R.string.webhooks_header_value)) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    IconButton(onClick = { update { it.copy(headers = it.headers.filterIndexed { i, _ -> i != index }) } }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.webhooks_remove_header))
                    }
                }
            }
        }
        OutlinedButton(onClick = { update { it.copy(headers = it.headers + WebhookHeader("", "")) } }) { Text(stringResource(R.string.webhooks_add_header)) }
    }
}

@StringRes
private fun WebhookRangePreset.labelRes(): Int = when (this) {
    WebhookRangePreset.SINCE_LAST_SUCCESS -> R.string.webhooks_range_since_last_success
    WebhookRangePreset.TODAY -> R.string.webhooks_range_today
    WebhookRangePreset.CURRENT_WEEK -> R.string.webhooks_range_current_week
    WebhookRangePreset.CURRENT_MONTH -> R.string.webhooks_range_current_month
    WebhookRangePreset.PREVIOUS_MONTH -> R.string.webhooks_range_previous_month
    WebhookRangePreset.LAST_30_DAYS -> R.string.webhooks_range_last_30_days
    WebhookRangePreset.CUSTOM -> R.string.webhooks_range_custom
}

@StringRes
private fun WebhookDataType.labelRes(): Int = when (this) {
    WebhookDataType.SUMMARY -> R.string.webhooks_type_summary
    WebhookDataType.TRANSACTIONS -> R.string.webhooks_type_transactions
    WebhookDataType.BUDGETS -> R.string.webhooks_type_budgets
    WebhookDataType.ACCOUNTS -> R.string.webhooks_type_accounts
    WebhookDataType.SUBSCRIPTIONS -> R.string.webhooks_type_subscriptions
}

@StringRes
private fun WebhookSyncReason.labelRes(): Int = when (this) {
    WebhookSyncReason.MANUAL -> R.string.webhooks_reason_manual
    WebhookSyncReason.INTERVAL -> R.string.webhooks_reason_interval
    WebhookSyncReason.SCHEDULED -> R.string.webhooks_reason_scheduled
    WebhookSyncReason.TEST -> R.string.webhooks_reason_test
}
