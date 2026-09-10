package com.tindahan.tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tindahan.tracker.R

@Composable
fun OnboardingScreen(
    onFinish: (businessName: String?) -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    var business by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (page) {
            0 -> {
                Text(stringResource(R.string.onboarding_title_1), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.onboarding_desc_1))
            }
            1 -> {
                Text(stringResource(R.string.onboarding_title_2), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.onboarding_desc_2))
            }
            else -> {
                Text(stringResource(R.string.onboarding_title_3), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.onboarding_desc_3))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.business_name_prompt), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = business,
                    onValueChange = { business = it },
                    label = { Text(stringResource(R.string.business_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { onFinish(null) }, modifier = Modifier.weight(1f).height(48.dp)) {
                Text(stringResource(R.string.onboarding_skip))
            }
            Button(
                onClick = {
                    if (page < 2) page++ else onFinish(business.ifBlank { null })
                },
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(
                    when (page) {
                        0, 1 -> stringResource(R.string.onboarding_next)
                        else -> stringResource(R.string.onboarding_get_started)
                    }
                )
            }
        }
    }
}
