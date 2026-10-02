package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.location.LocationResult
import com.example.ui.GroceryTotals
import java.util.Locale

@Composable
fun TotalsBanner(
    totals: GroceryTotals,
    currentLocation: LocationResult?,
    onOpenBudgetDialog: () -> Unit,
    onOpenCurrencyDialog: () -> Unit,
    onOpenFamilySyncDialog: () -> Unit,
    onClearChecked: () -> Unit,
    onUncheckAll: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val sym = totals.currency.symbol

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("totals_banner_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (totals.isBudgetExceeded) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Title, Action Badges (Currency, Budget, Family), and Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (totals.isBudgetExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Cart",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Estimated Budget & Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (totals.isBudgetExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "$sym${String.format(Locale.US, "%.2f", totals.totalCost)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (totals.isBudgetExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Currency Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .clickable { onOpenCurrencyDialog() }
                        .testTag("currency_switch_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(totals.currency.flag, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = totals.currency.code,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Family Share Button
                IconButton(
                    onClick = onOpenFamilySyncDialog,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("family_sync_quick_btn")
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = "Family Sync",
                        tint = if (totals.isBudgetExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Options Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("cart_options_button")
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = if (totals.isBudgetExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Set Shopping Budget Limit") },
                            leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                            onClick = {
                                onOpenBudgetDialog()
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Change Currency (${totals.currency.symbol})") },
                            leadingIcon = { Icon(Icons.Default.Paid, contentDescription = null) },
                            onClick = {
                                onOpenCurrencyDialog()
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share / Sync With Family") },
                            leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                            onClick = {
                                onOpenFamilySyncDialog()
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Checked Items") },
                            onClick = {
                                onClearChecked()
                                menuExpanded = false
                            },
                            enabled = totals.checkedItemCount > 0
                        )
                        DropdownMenuItem(
                            text = { Text("Uncheck All Items") },
                            onClick = {
                                onUncheckAll()
                                menuExpanded = false
                            },
                            enabled = totals.checkedItemCount > 0
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Entire List") },
                            onClick = {
                                onClearAll()
                                menuExpanded = false
                            },
                            enabled = totals.totalItemCount > 0
                        )
                    }
                }
            }

            // Budget Limiter Meter
            if (totals.budgetLimit != null && totals.budgetLimit > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                val pct = (totals.budgetPercentage * 100).toInt()
                val budgetProgress = totals.budgetPercentage.coerceIn(0f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onOpenBudgetDialog() }
                            .testTag("budget_progress_label")
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (totals.isBudgetExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Budget: $sym${String.format(Locale.US, "%.0f", totals.budgetLimit)} ($pct% used)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (totals.isBudgetExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = if (totals.isBudgetExceeded) {
                            "⚠️ Exceeded by $sym${String.format(Locale.US, "%.2f", totals.totalCost - totals.budgetLimit)}"
                        } else {
                            "$sym${String.format(Locale.US, "%.2f", totals.budgetLimit - totals.totalCost)} left"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (totals.isBudgetExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { budgetProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when {
                        totals.isBudgetExceeded -> MaterialTheme.colorScheme.error
                        totals.budgetPercentage >= 0.8f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                // Quick add budget prompt
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenBudgetDialog() }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ Set a shopping budget limit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subtotals Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // In-Cart Subtotal
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "In-Cart (${totals.checkedItemCount})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$sym${String.format(Locale.US, "%.2f", totals.checkedCost)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Remaining Subtotal
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Remaining (${totals.totalItemCount - totals.checkedItemCount})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$sym${String.format(Locale.US, "%.2f", totals.remainingCost)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Current Location tag banner if detected
            AnimatedVisibility(visible = currentLocation != null) {
                currentLocation?.let { loc ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tagged near: ${loc.suggestedStoreName.ifBlank { loc.address }}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
