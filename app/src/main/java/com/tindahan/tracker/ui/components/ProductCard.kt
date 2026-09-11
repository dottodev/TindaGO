package com.tindahan.tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tindahan.tracker.R
import com.tindahan.tracker.data.local.entities.Product
import com.tindahan.tracker.data.local.entities.StockState
import com.tindahan.tracker.util.MoneyUtils

@Composable
fun StockStatusChip(state: StockState, modifier: Modifier = Modifier) {
    val label = when (state) {
        StockState.IN_STOCK -> stringResource(R.string.in_stock)
        StockState.LOW_STOCK -> stringResource(R.string.low_stock)
        StockState.OUT_OF_STOCK -> stringResource(R.string.out_of_stock)
    }
    val icon = when (state) {
        StockState.IN_STOCK -> Icons.Default.CheckCircle
        StockState.LOW_STOCK -> Icons.Default.Warning
        StockState.OUT_OF_STOCK -> Icons.Default.Block
    }
    // Icon + text so status never relies on color alone.
    val container = when (state) {
        StockState.IN_STOCK -> MaterialTheme.colorScheme.primaryContainer
        StockState.LOW_STOCK -> MaterialTheme.colorScheme.tertiaryContainer
        StockState.OUT_OF_STOCK -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (state) {
        StockState.IN_STOCK -> MaterialTheme.colorScheme.onPrimaryContainer
        StockState.LOW_STOCK -> MaterialTheme.colorScheme.onTertiaryContainer
        StockState.OUT_OF_STOCK -> MaterialTheme.colorScheme.onErrorContainer
    }
    AssistChip(
        onClick = {},
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        colors = AssistChipDefaults.assistChipColors(containerColor = container, labelColor = content),
        modifier = modifier
    )
}

@Composable
fun ProductCard(
    product: Product,
    currency: String,
    sellLabel: String,
    restockLabel: String,
    onSell: () -> Unit,
    onRestock: () -> Unit,
    onCustomSell: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ProductImage(imagePath = product.imagePath, size = 56.dp, targetPx = 192)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${product.quantity} • ${MoneyUtils.formatCents(product.sellingPriceCents, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StockStatusChip(state = product.stockState)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Button(
                    onClick = onSell,
                    enabled = !product.isOutOfStock,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text(sellLabel, fontWeight = FontWeight.Bold) }
                androidx.compose.material3.Button(
                    onClick = onRestock,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text(restockLabel, fontWeight = FontWeight.Bold) }
                IconButton(
                    onClick = onCustomSell,
                    enabled = !product.isOutOfStock,
                    modifier = Modifier.height(48.dp).width(48.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.custom_sale))
                }
            }
        }
    }
}
