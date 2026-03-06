package com.quickmemo.app.presentation.settings

import com.quickmemo.app.billing.BillingState
import com.quickmemo.app.domain.model.AppSettings

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val billingState: BillingState = BillingState(),
    val appVersion: String = "1.0.0",
    val todoTabNames: List<String> = listOf("Todo 1", "Todo 2", "Todo 3"),
)
