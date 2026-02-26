package com.quickmemo.app.domain.usecase

import com.quickmemo.app.domain.model.AppSettings
import com.quickmemo.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.settingsFlow
}
