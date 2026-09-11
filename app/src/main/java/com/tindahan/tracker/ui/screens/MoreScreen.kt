package com.tindahan.tracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tindahan.tracker.R
import com.tindahan.tracker.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    vm: SettingsViewModel,
    onOpenDashboard: () -> Unit,
    onOpenCalculator: () -> Unit
) {
    val businessName by vm.businessName.collectAsState()
    val theme by vm.theme.collectAsState()
    val language by vm.language.collectAsState()
    val currency by vm.currency.collectAsState()
    val defaultLow by vm.defaultLowStock.collectAsState()

    var editBusiness by remember { mutableStateOf(false) }
    var businessDraft by remember(businessName) { mutableStateOf(businessName) }
    var showHelp by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val createBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = vm.buildBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    status = context.getString(R.string.saved)
                } catch (e: Exception) {
                    status = context.getString(R.string.error_generic)
                }
            }
        }
    }
    val openBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) showRestoreConfirm = uri
    }
    val createCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null && pendingExport != null) {
            val kind = pendingExport!!
            pendingExport = null
            scope.launch {
                try {
                    val (_, content) = vm.exportCsv(kind)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                    status = context.getString(R.string.saved)
                } catch (e: Exception) {
                    status = context.getString(R.string.error_generic)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                onClick = onOpenDashboard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.nav_dashboard), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("${stringResource(R.string.today_sales)} • ${stringResource(R.string.outstanding_utang)} • ${stringResource(R.string.inventory_value)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(
                onClick = onOpenCalculator,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.calculator), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.calculator_hint), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.business_name)) {
                if (editBusiness) {
                    OutlinedTextField(value = businessDraft, onValueChange = { businessDraft = it }, label = { Text(stringResource(R.string.business_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editBusiness = false }) { Text(stringResource(R.string.cancel)) }
                        Button(onClick = { vm.setBusinessName(businessDraft.trim()); editBusiness = false }) { Text(stringResource(R.string.save)) }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (businessName.isBlank()) "-" else businessName)
                        TextButton(onClick = { businessDraft = businessName; editBusiness = true }) { Text(stringResource(R.string.edit)) }
                    }
                }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.theme)) {
                ThemeDropdown(theme) { vm.setTheme(it) }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.language)) {
                LanguageDropdown(language) { vm.setLanguage(it) }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.currency)) {
                OutlinedTextField(value = currency, onValueChange = { if (it.length <= 3) vm.setCurrency(it) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            var draft by remember(defaultLow) { mutableStateOf(defaultLow.toString()) }
            SectionCard(title = stringResource(R.string.default_low_stock)) {
                OutlinedTextField(value = draft, onValueChange = { draft = it.filter { c -> c.isDigit() }.take(6) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Button(onClick = { draft.toIntOrNull()?.let { vm.setDefaultLowStock(it) } }, modifier = Modifier.height(48.dp)) { Text(stringResource(R.string.save)) }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.backup_restore)) {
                Button(onClick = { createBackupLauncher.launch("tinda-go-backup.json") }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(stringResource(R.string.backup_now)) }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { openBackupLauncher.launch(arrayOf("application/json", "*/*")) }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(stringResource(R.string.restore_backup)) }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.export_data)) {
                val kinds = listOf("products" to stringResource(R.string.export_products), "sales" to stringResource(R.string.export_sales), "utang" to stringResource(R.string.export_utang), "expenses" to stringResource(R.string.export_expenses))
                kinds.forEach { (kind, label) ->
                    TextButton(onClick = { pendingExport = kind; createCsvLauncher.launch("$kind.csv") }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(label) }
                }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.help)) {
                TextButton(onClick = { showHelp = true }) { Text(stringResource(R.string.help)) }
                TextButton(onClick = { showAbout = true }) { Text(stringResource(R.string.about)) }
            }
        }
        item {
            if (status != null) {
                Text(status!!, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(72.dp))
        }
    }

    if (showHelp) {
        AlertDialog(onDismissRequest = { showHelp = false }, title = { Text(stringResource(R.string.help)) }, text = { Text(stringResource(R.string.help_text)) }, confirmButton = { TextButton(onClick = { showHelp = false }) { Text(stringResource(R.string.save)) } })
    }
    if (showAbout) {
        AlertDialog(onDismissRequest = { showAbout = false }, title = { Text(stringResource(R.string.about)) }, text = { Text(stringResource(R.string.about_text)) }, confirmButton = { TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.save)) } })
    }
    val restoreUri = showRestoreConfirm
    if (restoreUri != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_message)) },
            confirmButton = {
                Button(onClick = {
                    showRestoreConfirm = null
                    scope.launch {
                        try {
                            val raw = context.contentResolver.openInputStream(restoreUri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                            val result = vm.restoreFromJson(raw)
                            status = if (result is com.tindahan.tracker.util.BackupUtils.ParseResult.Success) context.getString(R.string.saved) else context.getString(R.string.error_generic)
                        } catch (e: Exception) {
                            status = context.getString(R.string.error_generic)
                        }
                    }
                }) { Text(stringResource(R.string.restore)) }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDropdown(current: String, onPick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val dark = stringResource(R.string.theme_dark)
    val light = stringResource(R.string.theme_light)
    val sys = stringResource(R.string.theme_system)
    val label = when (current) { "light" -> light; "system" -> sys; else -> dark }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = label, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(dark) }, onClick = { onPick("dark"); expanded = false })
            DropdownMenuItem(text = { Text(light) }, onClick = { onPick("light"); expanded = false })
            DropdownMenuItem(text = { Text(sys) }, onClick = { onPick("system"); expanded = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(current: String, onPick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val en = stringResource(R.string.english)
    val tl = stringResource(R.string.filipino)
    val label = if (current == "tl") tl else en
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = label, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(en) }, onClick = { onPick("en"); expanded = false })
            DropdownMenuItem(text = { Text(tl) }, onClick = { onPick("tl"); expanded = false })
        }
    }
}
