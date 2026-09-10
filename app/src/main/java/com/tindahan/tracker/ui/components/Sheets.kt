package com.tindahan.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tindahan.tracker.R
import com.tindahan.tracker.data.local.entities.Expense
import com.tindahan.tracker.data.local.entities.Product
import com.tindahan.tracker.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductSheet(
    defaultThreshold: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, sellingCents: Long, costCents: Long?, qty: Int, thr: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selling by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("0") }
    var thr by remember { mutableStateOf(defaultThreshold.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidNumber = stringResource(R.string.invalid_number)
    val invalidAmount = stringResource(R.string.invalid_amount)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.add_product), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.product_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = selling, onValueChange = { selling = it }, label = { Text(stringResource(R.string.selling_price)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text(stringResource(R.string.cost_price)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text(stringResource(R.string.starting_quantity)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = thr, onValueChange = { thr = it }, label = { Text(stringResource(R.string.low_stock_threshold)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = {
                        if (name.isBlank()) { error = invalidNumber; return@Button }
                        val s = MoneyUtils.parseToCents(selling)
                        if (s == null) { error = invalidAmount; return@Button }
                        val c = if (cost.isBlank()) null else MoneyUtils.parseToCents(cost)
                        if (cost.isNotBlank() && c == null) { error = invalidAmount; return@Button }
                        val q = MoneyUtils.parseQuantity(qty)
                        val t = MoneyUtils.parseQuantity(thr)
                        if (q == null || t == null) { error = invalidNumber; return@Button }
                        onSave(name.trim(), s, c, q, t)
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text(stringResource(R.string.save)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductSheet(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (name: String, sellingCents: Long, costCents: Long?, qty: Int, thr: Int) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var selling by remember { mutableStateOf("%.2f".format(product.sellingPriceCents / 100.0)) }
    var cost by remember { mutableStateOf(product.costPriceCents?.let { "%.2f".format(it / 100.0) } ?: "") }
    var qty by remember { mutableStateOf(product.quantity.toString()) }
    var thr by remember { mutableStateOf(product.lowStockThreshold.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidNumber = stringResource(R.string.invalid_number)
    val invalidAmount = stringResource(R.string.invalid_amount)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.edit_product), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.product_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = selling, onValueChange = { selling = it }, label = { Text(stringResource(R.string.selling_price)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text(stringResource(R.string.cost_price)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text(stringResource(R.string.starting_quantity)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = thr, onValueChange = { thr = it }, label = { Text(stringResource(R.string.low_stock_threshold)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = {
                        if (name.isBlank()) { error = invalidNumber; return@Button }
                        val s = MoneyUtils.parseToCents(selling)
                        if (s == null) { error = invalidAmount; return@Button }
                        val c = if (cost.isBlank()) null else MoneyUtils.parseToCents(cost)
                        if (cost.isNotBlank() && c == null) { error = invalidAmount; return@Button }
                        val q = MoneyUtils.parseQuantity(qty)
                        val t = MoneyUtils.parseQuantity(thr)
                        if (q == null || t == null) { error = invalidNumber; return@Button }
                        onSave(name.trim(), s, c, q, t)
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text(stringResource(R.string.save)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUtangSheet(
    onDismiss: () -> Unit,
    onSave: (customer: String, desc: String, amountCents: Long, notes: String?) -> Unit
) {
    var customer by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidAmount = stringResource(R.string.invalid_amount)
    val required = stringResource(R.string.required)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.add_utang), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = customer, onValueChange = { customer = it }, label = { Text(stringResource(R.string.customer_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(stringResource(R.string.description)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(stringResource(R.string.amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.notes)) }, modifier = Modifier.fillMaxWidth())
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = {
                        if (customer.isBlank()) { error = required; return@Button }
                        val a = MoneyUtils.parseToCents(amount)
                        if (a == null || a <= 0) { error = invalidAmount; return@Button }
                        onSave(customer.trim(), desc.trim(), a, notes.ifBlank { null })
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text(stringResource(R.string.save)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    onDismiss: () -> Unit,
    onSave: (desc: String, amountCents: Long, category: String, notes: String?) -> Unit
) {
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidAmount = stringResource(R.string.invalid_amount)
    val required = stringResource(R.string.required)
    val categories = Expense.DEFAULT_CATEGORIES

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.add_expense), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(stringResource(R.string.description)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(stringResource(R.string.amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { category = it; expanded = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.notes)) }, modifier = Modifier.fillMaxWidth())
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = {
                        if (desc.isBlank()) { error = required; return@Button }
                        val a = MoneyUtils.parseToCents(amount)
                        if (a == null || a <= 0) { error = invalidAmount; return@Button }
                        onSave(desc.trim(), a, category, notes.ifBlank { null })
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text(stringResource(R.string.save)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
