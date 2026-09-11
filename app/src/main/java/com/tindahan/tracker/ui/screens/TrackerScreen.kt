package com.tindahan.tracker.ui.screens

import android.content.Intent
import androidx.compose.animation.Crossfade
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tindahan.tracker.R
import com.tindahan.tracker.ui.components.AddExpenseSheet
import com.tindahan.tracker.ui.components.AddUtangSheet
import com.tindahan.tracker.ui.components.ConfirmDeleteDialog
import com.tindahan.tracker.ui.components.EmptyState
import com.tindahan.tracker.ui.components.ExpenseCard
import com.tindahan.tracker.ui.components.StatCard
import com.tindahan.tracker.ui.components.UtangCard
import com.tindahan.tracker.util.MoneyUtils
import com.tindahan.tracker.viewmodel.ExpenseViewModel
import com.tindahan.tracker.viewmodel.UtangViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    utangVm: UtangViewModel,
    expenseVm: ExpenseViewModel,
    currency: String,
    businessName: String
) {
    var tab by remember { mutableIntStateOf(0) }
    val utangTab = stringResource(R.string.tab_utang)
    val expensesTab = stringResource(R.string.tab_expenses)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(selected = tab == 0, onClick = { tab = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                Text(utangTab)
            }
            SegmentedButton(selected = tab == 1, onClick = { tab = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                Text(expensesTab)
            }
        }
        Spacer(Modifier.height(12.dp))
        Crossfade(targetState = tab, label = "tracker-tab") { t ->
            if (t == 0) {
                UtangTab(utangVm, currency, businessName)
            } else {
                ExpensesTab(expenseVm, currency)
            }
        }
    }
}

@Composable
private fun UtangTab(vm: UtangViewModel, currency: String, businessName: String) {    val items by vm.items.collectAsState()
    val query by vm.query.collectAsState()
    val unpaid by vm.unpaidTotal.collectAsState()
    val paid by vm.paidTotal.collectAsState()
    val total by vm.grandTotal.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(label = stringResource(R.string.unpaid_utang), value = MoneyUtils.formatCents(unpaid, currency), modifier = Modifier.weight(1f), container = MaterialTheme.colorScheme.tertiaryContainer)
            StatCard(label = stringResource(R.string.paid_utang), value = MoneyUtils.formatCents(paid, currency), modifier = Modifier.weight(1f), container = MaterialTheme.colorScheme.primaryContainer)
        }
        StatCard(label = stringResource(R.string.total_utang), value = MoneyUtils.formatCents(total, currency))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Button(
                onClick = { showAdd = true },
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("+ ${stringResource(R.string.add_utang)}") }
            IconButton(onClick = {
                val unpaidItems = items.filter { !it.isPaid }.map { it.customerName to it.amountCents }
                val text = MoneyUtils.buildUtangShareText(businessName.ifBlank { null }, unpaidItems, currency)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, null))
            }, modifier = Modifier.height(48.dp)) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_utang_list))
            }
        }
        OutlinedTextField(value = query, onValueChange = vm::setQuery, label = { Text(stringResource(R.string.search_customer)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (items.isEmpty()) {
            EmptyState(title = stringResource(R.string.no_unpaid_utang), hint = stringResource(R.string.no_utang_hint))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(items, key = { it.id }) { u ->
                    UtangCard(
                        item = u,
                        currency = currency,
                        onMarkPaid = { vm.setPaid(u.id, true) },
                        onDelete = { deleteId = u.id }
                    )
                }
            }
        }
    }
    if (showAdd) {
        AddUtangSheet(
            onDismiss = { showAdd = false },
            onSave = { c, d, a, n ->
                vm.add(c, d, a, System.currentTimeMillis(), null, n) { showAdd = false }
            }
        )
    }
    if (deleteId != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_utang_confirm),
            onConfirm = { vm.delete(deleteId!!); deleteId = null },
            onDismiss = { deleteId = null }
        )
    }
}

@Composable
private fun ExpensesTab(vm: ExpenseViewModel, currency: String) {
    val items by vm.items.collectAsState()
    val query by vm.query.collectAsState()
    val today by vm.todayTotal.collectAsState()
    val month by vm.monthTotal.collectAsState()
    val total by vm.grandTotal.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(label = stringResource(R.string.today_expenses), value = MoneyUtils.formatCents(today, currency), modifier = Modifier.weight(1f), container = MaterialTheme.colorScheme.secondaryContainer)
            StatCard(label = stringResource(R.string.month_expenses), value = MoneyUtils.formatCents(month, currency), modifier = Modifier.weight(1f), container = MaterialTheme.colorScheme.tertiaryContainer)
        }
        StatCard(label = stringResource(R.string.total_expenses), value = MoneyUtils.formatCents(total, currency))
        androidx.compose.material3.Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("+ ${stringResource(R.string.add_expense)}")
        }
        OutlinedTextField(value = query, onValueChange = vm::setQuery, label = { Text(stringResource(R.string.search_expenses)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (items.isEmpty()) {
            EmptyState(title = stringResource(R.string.no_expenses), hint = stringResource(R.string.no_expenses_hint))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(items, key = { it.id }) { e ->
                    ExpenseCard(item = e, currency = currency, onDelete = { deleteId = e.id })
                }
            }
        }
    }
    if (showAdd) {
        AddExpenseSheet(
            onDismiss = { showAdd = false },
            onSave = { d, a, c, n ->
                vm.add(d, a, System.currentTimeMillis(), c, n) { showAdd = false }
            }
        )
    }
    if (deleteId != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_expense_confirm),
            onConfirm = { vm.delete(deleteId!!); deleteId = null },
            onDismiss = { deleteId = null }
        )
    }
}
