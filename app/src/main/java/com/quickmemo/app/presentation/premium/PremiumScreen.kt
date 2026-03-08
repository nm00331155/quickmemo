// File: app/src/main/java/com/quickmemo/app/presentation/premium/PremiumScreen.kt
package com.quickmemo.app.presentation.premium

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickmemo.app.billing.BillingManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    onOpenTodo: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PremiumViewModel = hiltViewModel(),
) {
    val billingState by viewModel.billingState.collectAsStateWithLifecycle()
    val purchaseState = billingState.purchaseState
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("プレミアム機能") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenTodo) {
                        Icon(
                            imageVector = Icons.Outlined.CheckBox,
                            contentDescription = "todo",
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "settings",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "🌟 QuickMemo Pro",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            items(
                listOf(
                    "広告の完全除去",
                    "翻訳機能",
                ),
            ) { feature ->
                Text(
                    text = "✓ $feature",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            item {
                IndividualPurchaseRow(
                    label = "広告除去のみ",
                    price = "¥300",
                    purchased = purchaseState.isAdFree,
                    onPurchase = {
                        activity?.let { viewModel.purchase(it, BillingManager.Products.REMOVE_ADS) }
                    },
                )
            }

            item {
                IndividualPurchaseRow(
                    label = "翻訳機能の解放",
                    price = "¥300",
                    purchased = purchaseState.hasTranslation,
                    onPurchase = {
                        activity?.let { viewModel.purchase(it, BillingManager.Products.UNLOCK_TRANSLATION) }
                    },
                )
            }

            val error = billingState.lastErrorMessage
            if (!error.isNullOrBlank()) {
                item {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    purchased: Boolean,
    onPurchase: () -> Unit,
    subLabel: String? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "$title   $price",
                style = MaterialTheme.typography.titleMedium,
            )
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onPurchase,
                enabled = !purchased,
            ) {
                Text(if (purchased) "✓ 購入済み" else "購入する")
            }
        }
    }
}

@Composable
private fun IndividualPurchaseRow(
    label: String,
    price: String,
    purchased: Boolean,
    onPurchase: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label  $price",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onPurchase,
            enabled = !purchased,
        ) {
            Text(if (purchased) "✓ 購入済み" else "購入")
        }
    }
}
