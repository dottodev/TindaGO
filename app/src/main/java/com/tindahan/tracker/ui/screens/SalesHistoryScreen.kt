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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tindahan.tracker.R
import com.tindahan.tracker.data.repository.TindahanRepository
import com.tindahan.tracker.ui.components.EmptyState
import com.tindahan.tracker.ui.components.StatCard
import com.tindahan.tracker.util.DateUtils
import com.tindahan.tracker.util.MoneyUtils
import com.tindahan.tracker.viewmodel.SalesHistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    repo: TindahanRepository,
    currency: String,
    onBack: () -> Unit
) {
    val vm: SalesHistoryViewModel = viewModel(factory = SalesHistoryViewModel.Factory(repo))
    val sales by vm.sales.collectAsState()
    val total by vm.total.collectAsState()
    val filter by vm.filter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sales_history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == DateUtils.SalesFilter.TODAY, onClick = { vm.setFilter(DateUtils.SalesFilter.TODAY) }, label = { Text(stringResource(R.string.filter_today)) })
                FilterChip(selected = filter == DateUtils.SalesFilter.YESTERDAY, onClick = { vm.setFilter(DateUtils.SalesFilter.YESTERDAY) }, label = { Text(stringResource(R.string.filter_yesterday)) })
                FilterChip(selected = filter == DateUtils.SalesFilter.WEEK, onClick = { vm.setFilter(DateUtils.SalesFilter.WEEK) }, label = { Text(stringResource(R.string.filter_week)) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == DateUtils.SalesFilter.MONTH, onClick = { vm.setFilter(DateUtils.SalesFilter.MONTH) }, label = { Text(stringResource(R.string.filter_month)) })
                FilterChip(selected = filter == DateUtils.SalesFilter.ALL, onClick = { vm.setFilter(DateUtils.SalesFilter.ALL) }, label = { Text(stringResource(R.string.filter_all)) })
            }
            StatCard(label = stringResource(R.string.today_sales), value = MoneyUtils.formatCents(total, currency), modifier = Modifier.fillMaxWidth())
            if (sales.isEmpty()) {
                EmptyState(title = stringResource(R.string.no_sales), hint = "")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sales, key = { it.id }) { s ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(s.productName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(MoneyUtils.formatCents(s.totalCents, currency), fontWeight = FontWeight.Bold)
                                }
                                Text("${s.quantity} × ${MoneyUtils.formatCents(s.unitPriceCents, currency)} • ${DateUtils.formatDateTime(s.timestamp)}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
