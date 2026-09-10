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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.tindahan.tracker.R
import com.tindahan.tracker.ui.components.ConfirmDeleteDialog
import com.tindahan.tracker.ui.components.EditProductSheet
import com.tindahan.tracker.ui.components.SellRestockRow
import com.tindahan.tracker.util.DateUtils
import com.tindahan.tracker.util.MoneyUtils
import com.tindahan.tracker.viewmodel.ProductDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    vm: ProductDetailsViewModel,
    currency: String,
    onBack: () -> Unit
) {
    val product by vm.product.collectAsState()
    val movements by vm.movements.collectAsState()
    val sales by vm.sales.collectAsState()
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val p = product

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_product_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showEdit = true }, enabled = p != null) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_product))
                    }
                    IconButton(onClick = { showDelete = true }, enabled = p != null) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_product), tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { pad ->
        if (p == null) {
            Column(Modifier.fillMaxSize().padding(pad).padding(24.dp)) {
                Text(stringResource(R.string.error_generic))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(p.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("${stringResource(R.string.current_stock)}: ${p.quantity}")
                            Text("${stringResource(R.string.selling_price)}: ${MoneyUtils.formatCents(p.sellingPriceCents, currency)}")
                            Text("${stringResource(R.string.cost_price)}: ${p.costPriceCents?.let { MoneyUtils.formatCents(it, currency) } ?: "-"}")
                            Text("${stringResource(R.string.inventory_value)}: ${MoneyUtils.formatCents(p.inventoryValueCents, currency)}")
                            val profit = p.profitPerItemCents
                            Text(
                                if (profit != null) "${stringResource(R.string.estimated_profit_per_item)}: ${MoneyUtils.formatCents(profit, currency)}"
                                else stringResource(R.string.profit_unavailable)
                            )
                            Text("${stringResource(R.string.low_stock_threshold)}: ${p.lowStockThreshold}")
                            if (p.isLowStock) {
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(R.string.low_stock), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))
                            SellRestockRow(
                                onSell = vm::sell,
                                onRestock = vm::restock,
                                sellLabel = stringResource(R.string.sell),
                                restockLabel = stringResource(R.string.restock),
                                sellEnabled = p.quantity > 0
                            )
                        }
                    }
                }
                item {
                    Text(stringResource(R.string.recent_transactions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(movements.take(30), key = { it.id }) { m ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("${m.type} (${m.delta})", fontWeight = FontWeight.Bold)
                                Text(DateUtils.formatDateTime(m.timestamp), style = MaterialTheme.typography.labelSmall)
                            }
                            Text("${m.quantityAfter}")
                        }
                    }
                }
                items(sales.take(30), key = { "s${it.id}" }) { s ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${s.quantity} × ${MoneyUtils.formatCents(s.unitPriceCents, currency)}")
                            Text(MoneyUtils.formatCents(s.totalCents, currency), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showEdit && p != null) {
        EditProductSheet(
            product = p,
            onDismiss = { showEdit = false },
            onSave = { name, selling, cost, qty, thr ->
                vm.saveEdit(name, selling, cost, qty, thr) { showEdit = false }
            }
        )
    }
    if (showDelete) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_product_confirm),
            message = stringResource(R.string.delete_product_message),
            onConfirm = { showDelete = false; vm.delete(onBack) },
            onDismiss = { showDelete = false }
        )
    }
}
