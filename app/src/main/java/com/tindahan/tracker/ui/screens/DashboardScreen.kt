package com.tindahan.tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tindahan.tracker.R
import com.tindahan.tracker.ui.components.StatCard
import com.tindahan.tracker.util.MoneyUtils
import com.tindahan.tracker.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: DashboardViewModel,
    currency: String,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_dashboard)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatCard(label = stringResource(R.string.today_sales), value = MoneyUtils.formatCents(state.todaySales, currency), modifier = Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.primaryContainer)
            }
            item {
                StatCard(label = stringResource(R.string.today_expenses), value = MoneyUtils.formatCents(state.todayExpenses, currency), modifier = Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.secondaryContainer)
            }
            item {
                StatCard(label = stringResource(R.string.outstanding_utang), value = MoneyUtils.formatCents(state.outstandingUtang, currency), modifier = Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.tertiaryContainer)
            }
            item {
                StatCard(label = stringResource(R.string.inventory_value), value = MoneyUtils.formatCents(state.inventoryValue, currency), modifier = Modifier.fillMaxWidth())
            }
            item {
                val profit = state.estimatedProfitToday
                if (state.profitAvailable && profit != null) {
                    StatCard(
                        label = stringResource(R.string.estimated_profit),
                        value = MoneyUtils.formatCents(profit, currency),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    StatCard(
                        label = stringResource(R.string.estimated_profit),
                        value = stringResource(R.string.profit_unavailable),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
