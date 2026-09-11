package com.tindahan.tracker.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.tindahan.tracker.R
import com.tindahan.tracker.data.local.entities.Expense
import com.tindahan.tracker.data.local.entities.Product
import com.tindahan.tracker.util.DiscountType
import com.tindahan.tracker.util.Discounts
import com.tindahan.tracker.util.ImageStore
import com.tindahan.tracker.util.MoneyUtils
import java.io.File

/**
 * Optional product photo row. Gallery uses the system photo picker and the
 * camera is delegated via intent, so no storage/camera permissions are needed.
 * Picked images are resized and staged immediately; the sheet owner cleans up
 * unstaged files on cancel.
 */
@Composable
fun ImagePickerRow(
    imageName: String?,
    onImageStaged: (String?) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val saved = ImageStore.saveFromUri(context, uri)
        if (saved == null) onError() else onImageStaged(saved)
    }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok: Boolean ->
        if (!ok) return@rememberLauncherForActivityResult
        val uri = cameraUri
        if (uri == null) {
            onError()
            return@rememberLauncherForActivityResult
        }
        val saved = ImageStore.saveFromUri(context, uri)
        if (saved == null) onError() else onImageStaged(saved)
    }

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ProductImage(imagePath = imageName, size = 72.dp, targetPx = 256)
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.gallery))
                }
                OutlinedButton(
                    onClick = {
                        try {
                            val tmp = File(context.cacheDir, "camera_tmp.jpg")
                            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", tmp)
                            cameraUri = uri
                            camera.launch(uri)
                        } catch (e: Exception) {
                            onError()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.camera))
                }
            }
            if (imageName != null) {
                TextButton(onClick = { onImageStaged(null) }, modifier = Modifier.height(40.dp)) {
                    Text(stringResource(R.string.remove_photo), color = MaterialTheme.colorScheme.error)
                }
            } else {
                Text(
                    stringResource(R.string.add_photo_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Tracks a staged image file. Call [StagedImage.commit] on save and
 * [StagedImage.cancel] when the sheet is dismissed without saving so staged
 * files don't leak. Explicit cancel (instead of dispose cleanup) survives
 * screen rotation.
 */
@Composable
fun rememberStagedImage(initial: String?): StagedImage {
    val context = LocalContext.current
    var imageName by remember { mutableStateOf(initial) }
    var committed by remember { mutableStateOf(false) }
    return remember {
        object : StagedImage {
            override val current: String? get() = imageName
            override fun stage(staged: String?) {
                val prev = imageName
                imageName = staged
                // Delete replaced staged files right away (never the original).
                if (prev != null && prev != initial && prev != staged) ImageStore.delete(context, prev)
                if (staged == null && prev != null && prev != initial) ImageStore.delete(context, prev)
            }
            override fun commit() { committed = true }
            override fun cancel() {
                if (!committed && imageName != null && imageName != initial) {
                    ImageStore.delete(context, imageName)
                }
            }
        }
    }
}

interface StagedImage {
    val current: String?
    fun stage(staged: String?)
    fun commit()
    fun cancel()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductSheet(
    defaultThreshold: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, sellingCents: Long, costCents: Long?, qty: Int, thr: Int, image: String?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selling by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("0") }
    var thr by remember { mutableStateOf(defaultThreshold.toString()) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var photoError by remember { mutableStateOf(false) }
    val staged = rememberStagedImage(null)
    val invalidNumber = stringResource(R.string.invalid_number)
    val invalidAmount = stringResource(R.string.invalid_amount)
    val dismiss = { staged.cancel(); onDismiss() }

    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(20.dp).imePadding().verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.add_product), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            ImagePickerRow(
                imageName = staged.current,
                onImageStaged = { staged.stage(it); photoError = false },
                onError = { photoError = true }
            )
            if (photoError) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.photo_error), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
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
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.notes)) }, modifier = Modifier.fillMaxWidth())
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = dismiss, modifier = Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.cancel)) }
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
                        staged.commit()
                        onSave(name.trim(), s, c, q, t, staged.current, notes.ifBlank { null })
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
    onSave: (name: String, sellingCents: Long, costCents: Long?, qty: Int, thr: Int, image: String?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var selling by remember { mutableStateOf("%.2f".format(product.sellingPriceCents / 100.0)) }
    var cost by remember { mutableStateOf(product.costPriceCents?.let { "%.2f".format(it / 100.0) } ?: "") }
    var qty by remember { mutableStateOf(product.quantity.toString()) }
    var thr by remember { mutableStateOf(product.lowStockThreshold.toString()) }
    var notes by remember { mutableStateOf(product.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var photoError by remember { mutableStateOf(false) }
    val staged = rememberStagedImage(product.imagePath)
    val invalidNumber = stringResource(R.string.invalid_number)
    val invalidAmount = stringResource(R.string.invalid_amount)
    val dismiss = { staged.cancel(); onDismiss() }

    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(20.dp).imePadding().verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.edit_product), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            ImagePickerRow(
                imageName = staged.current,
                onImageStaged = { staged.stage(it); photoError = false },
                onError = { photoError = true }
            )
            if (photoError) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.photo_error), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
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
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.notes)) }, modifier = Modifier.fillMaxWidth())
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = dismiss, modifier = Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.cancel)) }
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
                        staged.commit()
                        onSave(name.trim(), s, c, q, t, staged.current, notes.ifBlank { null })
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
fun SellSheet(
    productName: String,
    unitPriceCents: Long,
    stock: Int,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (qty: Int, discountCents: Long, discountLabel: String?, note: String?) -> Unit
) {
    var qtyText by remember { mutableStateOf("1") }
    var discountTypeIdx by remember { mutableIntStateOf(0) }
    var discountValue by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidNumber = stringResource(R.string.invalid_number)

    val qty = qtyText.toIntOrNull()?.coerceIn(1, maxOf(stock, 1)) ?: 1
    val type = when (discountTypeIdx) {
        1 -> DiscountType.PERCENT
        2 -> DiscountType.FIXED
        else -> DiscountType.NONE
    }
    val parsedDiscount = discountValue.replace(",", ".").toDoubleOrNull() ?: 0.0
    val calc = Discounts.calculate(unitPriceCents * qty.toLong(), type, parsedDiscount)
    val label = Discounts.label(type, parsedDiscount, currency)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(20.dp).imePadding().verticalScroll(rememberScrollState())) {
            Text(productName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "${MoneyUtils.formatCents(unitPriceCents, currency)} • ${stringResource(R.string.current_stock)}: $stock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.quantity), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { qtyText = (qty - 1).coerceAtLeast(1).toString() },
                    modifier = Modifier.height(48.dp).width(48.dp)
                ) { Icon(Icons.Default.Remove, contentDescription = null) }
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter { c -> c.isDigit() }.take(6) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { qtyText = (qty + 1).coerceAtMost(stock).toString() },
                    modifier = Modifier.height(48.dp).width(48.dp)
                ) { Icon(Icons.Default.Add, contentDescription = null) }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.discount), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(selected = discountTypeIdx == 0, onClick = { discountTypeIdx = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 3)) {
                    Text(stringResource(R.string.discount_none))
                }
                SegmentedButton(selected = discountTypeIdx == 1, onClick = { discountTypeIdx = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 3)) {
                    Text(stringResource(R.string.discount_percent))
                }
                SegmentedButton(selected = discountTypeIdx == 2, onClick = { discountTypeIdx = 2 }, shape = SegmentedButtonDefaults.itemShape(2, 3)) {
                    Text(stringResource(R.string.discount_fixed))
                }
            }
            if (type != DiscountType.NONE) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = discountValue,
                    onValueChange = { discountValue = it },
                    label = { Text(if (type == DiscountType.PERCENT) stringResource(R.string.discount_percent) else stringResource(R.string.discount_fixed)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.notes)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.subtotal))
                Text(MoneyUtils.formatCents(calc.subtotalCents, currency))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.discount))
                Text("-" + MoneyUtils.formatCents(calc.discountCents, currency))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.total), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(MoneyUtils.formatCents(calc.totalCents, currency), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
                        val q = qtyText.toIntOrNull()
                        if (q == null || q < 1 || q > stock) { error = invalidNumber; return@Button }
                        onConfirm(q, calc.discountCents, label, note.ifBlank { null })
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text(stringResource(R.string.record_sale)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockSheet(
    productName: String,
    stock: Int,
    onDismiss: () -> Unit,
    onConfirm: (qty: Int) -> Unit
) {
    var qtyText by remember { mutableStateOf("1") }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidNumber = stringResource(R.string.invalid_number)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(20.dp).imePadding().verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.restock_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "$productName • ${stringResource(R.string.current_stock)}: $stock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = qtyText,
                onValueChange = { qtyText = it.filter { c -> c.isDigit() }.take(6) },
                label = { Text(stringResource(R.string.quantity)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
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
                        val q = qtyText.toIntOrNull()
                        if (q == null || q < 1) { error = invalidNumber; return@Button }
                        onConfirm(q)
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
        Column(Modifier.padding(20.dp).imePadding().verticalScroll(rememberScrollState())) {
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
        Column(Modifier.padding(20.dp).imePadding().verticalScroll(rememberScrollState())) {
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
