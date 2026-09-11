package com.tindahan.tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tindahan.tracker.R
import com.tindahan.tracker.util.Calculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf(Calculator.State()) }
    fun press(key: Calculator.Key) {
        state = Calculator.reduce(state, key)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calculator)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    state.display,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().padding(20.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            val rows: List<List<Pair<String, Calculator.Key>>> = listOf(
                listOf("C" to Calculator.Key.Clear, "⌫" to Calculator.Key.Back, "%" to Calculator.Key.Percent, "÷" to Calculator.Key.Op('÷')),
                listOf("7" to Calculator.Key.Digit('7'), "8" to Calculator.Key.Digit('8'), "9" to Calculator.Key.Digit('9'), "×" to Calculator.Key.Op('×')),
                listOf("4" to Calculator.Key.Digit('4'), "5" to Calculator.Key.Digit('5'), "6" to Calculator.Key.Digit('6'), "−" to Calculator.Key.Op('-')),
                listOf("1" to Calculator.Key.Digit('1'), "2" to Calculator.Key.Digit('2'), "3" to Calculator.Key.Digit('3'), "+" to Calculator.Key.Op('+')),
                listOf("±" to Calculator.Key.Negate, "0" to Calculator.Key.Digit('0'), "." to Calculator.Key.Dot, "=" to Calculator.Key.Equals)
            )
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                    row.forEach { (label, key) ->
                        val isOp = key is Calculator.Key.Op || key == Calculator.Key.Equals
                        Button(
                            onClick = { press(key) },
                            colors = if (isOp) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f).fillMaxSize()
                        ) {
                            Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
