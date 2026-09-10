package com.tindahan.tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tindahan.tracker.R
import com.tindahan.tracker.data.local.entities.Product
import com.tindahan.tracker.ui.components.AddProductSheet
import com.tindahan.tracker.ui.components.EmptyState
import com.tindahan.tracker.ui.components.ProductCard
import com.tindahan.tracker.ui.components.SellSheet
import com.tindahan.tracker.ui.components.StatCard
import com.tindahan.tracker.util.MoneyUtils
import com.tindahan.tracker.viewmodel.StockViewModel

@Composable
fun StockScreen(
    vm: StockViewModel,
    currency: String,
    defaultThreshold: Int,
    onOpenProduct: (Long) -> Unit,
    onOpenSalesHistory: () -> Unit
) {
    val products by vm.products.collectAsState()
    val query by vm.query.collectAsState()
    val invValue by vm.inventoryValue.collectAsState()
    val count by vm.productCount.collectAsState()
    val items by vm.totalItems.collectAsState()
    val todaySales by vm.todaySales.collectAsState()
    val message by vm.message.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var sellTarget by remember { mutableStateOf<Product?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val soldTemplate = stringResource(R.string.sold_message)
    val restockedTemplate = stringResource(R.string.restocked_message)
    val sellLabel = stringResource(R.string.sell)
    val restockLabel = stringResource(R.string.restock)

    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message!!)
            vm.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_product))
            }
        }
    ) { pad ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(pad)
        ) {
            val wide = maxWidth > 600.dp
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(label = stringResource(R.string.inventory_value), value = MoneyUtils.formatCents(invValue, currency), modifier = Modifier.weight(1f))
                    StatCard(label = stringResource(R.string.today_sales), value = MoneyUtils.formatCents(todaySales, currency), modifier = Modifier.weight(1f))
                    if (wide) {
                        StatCard(label = stringResource(R.string.total_products), value = count.toString(), modifier = Modifier.weight(1f))
                        StatCard(label = stringResource(R.string.total_items), value = items.toString(), modifier = Modifier.weight(1f))
                    }
                }
                if (!wide) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard(label = stringResource(R.string.total_products), value = count.toString(), modifier = Modifier.weight(1f))
                        StatCard(label = stringResource(R.string.total_items), value = items.toString(), modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onOpenSalesHistory) { Text(stringResource(R.string.sales_history)) }
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    label = { Text(stringResource(R.string.search_products)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                if (products.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.no_products),
                        hint = stringResource(R.string.no_products_hint),
                        actionLabel = stringResource(R.string.add_product),
                        onAction = { showAdd = true }
                    )
                } else if (wide) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(products, key = { it.id }) { p ->
                            ProductCard(
                                product = p,
                                currency = currency,
                                sellLabel = "-1 $sellLabel",
                                restockLabel = "+1 $restockLabel",
                                onSell = { vm.sell(p.id, soldTemplate) },
                                onRestock = { vm.restock(p.id, restockedTemplate) },
                                onCustomSell = { sellTarget = p },
                                onOpen = { onOpenProduct(p.id) }
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(products, key = { it.id }) { p ->
                            ProductCard(
                                product = p,
                                currency = currency,
                                sellLabel = "-1 $sellLabel",
                                restockLabel = "+1 $restockLabel",
                                onSell = { vm.sell(p.id, soldTemplate) },
                                onRestock = { vm.restock(p.id, restockedTemplate) },
                                onCustomSell = { sellTarget = p },
                                onOpen = { onOpenProduct(p.id) }
                            )
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddProductSheet(
            defaultThreshold = defaultThreshold,
            onDismiss = { showAdd = false },
            onSave = { name, selling, cost, qty, thr, image, notes ->
                vm.addProduct(name, selling, cost, qty, thr, image, notes) { showAdd = false }
            }
        )
    }

    val sellP = sellTarget
    if (sellP != null) {
        SellSheet(
            productName = sellP.name,
            unitPriceCents = sellP.sellingPriceCents,
            stock = sellP.quantity,
            currency = currency,
            onDismiss = { sellTarget = null },
            onConfirm = { qty, disc, label, note ->
                vm.sellCustom(sellP.id, sellP.name, qty, disc, label, note, soldTemplate) { sellTarget = null }
            }
        )
    }
}
