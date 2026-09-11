package com.tindahan.tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import com.tindahan.tracker.R
import com.tindahan.tracker.data.local.entities.Note
import com.tindahan.tracker.ui.components.ConfirmDeleteDialog
import com.tindahan.tracker.ui.components.EmptyState
import com.tindahan.tracker.util.DateUtils
import com.tindahan.tracker.viewmodel.NotesViewModel

@Composable
fun NotesScreen(vm: NotesViewModel) {
    val items by vm.items.collectAsState()
    val query by vm.query.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Note?>(null) }
    var deleteTarget by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_note))
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Text(
                stringResource(R.string.notes_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                label = { Text(stringResource(R.string.search_notes)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            if (items.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.no_notes),
                    hint = stringResource(R.string.no_notes_hint),
                    actionLabel = stringResource(R.string.add_note),
                    onAction = { showAdd = true }
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    items(items, key = { it.id }, contentType = { "note" }) { n ->
                        NoteCard(
                            note = n,
                            onEdit = { editing = n },
                            onDelete = { deleteTarget = n }
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAdd) {
        NoteSheet(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { t, b -> vm.save(t, b, null) { showAdd = false } }
        )
    }
    val edit = editing
    if (edit != null) {
        NoteSheet(
            initial = edit,
            onDismiss = { editing = null },
            onSave = { t, b -> vm.save(t, b, edit) { editing = null } }
        )
    }
    if (deleteTarget != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_note_confirm),
            onConfirm = { vm.delete(deleteTarget!!); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    note.title.ifBlank { stringResource(R.string.note_untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.height(40.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_note))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.height(40.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (note.body.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(note.body, style = MaterialTheme.typography.bodyMedium, maxLines = 6)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                DateUtils.formatDateTime(note.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteSheet(
    initial: Note?,
    onDismiss: () -> Unit,
    onSave: (title: String, body: String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var body by remember { mutableStateOf(initial?.body ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val required = stringResource(R.string.note_required)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(20.dp).imePadding().verticalScroll(rememberScrollState())) {
            Text(
                if (initial == null) stringResource(R.string.add_note) else stringResource(R.string.edit_note),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.note_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text(stringResource(R.string.note_body)) },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = {
                        if (title.isBlank() && body.isBlank()) { error = required; return@Button }
                        onSave(title, body)
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text(stringResource(R.string.save)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
