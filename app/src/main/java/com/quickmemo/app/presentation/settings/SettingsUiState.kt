package com.quickmemo.app.presentation.settings

import com.quickmemo.app.ai.AiEngineManager
import com.quickmemo.app.ai.ModelManager
import com.quickmemo.app.billing.BillingState
import com.quickmemo.app.domain.model.AppSettings

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val billingState: BillingState = BillingState(),
    val appVersion: String = "1.0.0",
    val todoTabNames: List<String> = listOf("Todo 1", "Todo 2", "Todo 3"),
    val aiEnginePreferences: AiEngineManager.AiEnginePreferences = AiEngineManager.AiEnginePreferences(),
    val aiModelStatus: ModelManager.ModelStatus = ModelManager.ModelStatus.NOT_DOWNLOADED,
    val aiModelProgress: Float = 0f,
)
