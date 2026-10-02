package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.CurrencyConfig
import java.util.Locale

@Composable
fun BudgetDialog(
    currentBudget: Double?,
    currency: CurrencyConfig,
    onDismiss: () -> Unit,
    onSetBudget: (Double?) -> Unit
) {
    var budgetText by remember {
        mutableStateOf(if (currentBudget != null && currentBudget > 0) String.format(Locale.US, "%.0f", currentBudget) else "")
    }

    val presets = if (currency.code == "PHP") {
        listOf(1000.0, 2000.0, 3000.0, 5000.0, 10000.0)
    } else {
        listOf(25.0, 50.0, 75.0, 100.0, 200.0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Shopping Budget Limiter",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Set a spending limit for this grocery trip in ${currency.flag} ${currency.name}. SmartCart will warn you when approaching or exceeding your target.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text("Budget Limit") },
                    prefix = { Text(currency.symbol, fontWeight = FontWeight.Bold) },
                    placeholder = { Text(if (currency.code == "PHP") "2500" else "50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_input_field")
                )

                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = budgetText.toDoubleOrNull() == preset,
                            onClick = { budgetText = String.format(Locale.US, "%.0f", preset) },
                            label = { Text("${currency.symbol}${preset.toInt()}") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = budgetText.toDoubleOrNull()
                    onSetBudget(amount)
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Set Budget")
            }
        },
        dismissButton = {
            Row {
                if (currentBudget != null) {
                    TextButton(
                        onClick = { onSetBudget(null) },
                        modifier = Modifier.testTag("clear_budget_button")
                    ) {
                        Text("Remove Limit")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
