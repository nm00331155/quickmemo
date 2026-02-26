package com.quickmemo.app.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quickmemo.app.BuildConfig
import com.quickmemo.app.data.datastore.settingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class AiUsageManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun canUse(isUnlimited: Boolean): Boolean {
        if (BuildConfig.DEBUG || isUnlimited) return true

        val usage = getUsage()
        return usage.count < FREE_LIMIT_PER_MONTH
    }

    suspend fun consumeOnSuccess(isUnlimited: Boolean) {
        if (BuildConfig.DEBUG || isUnlimited) return

        val current = getUsage()
        context.settingsDataStore.edit { preferences ->
            preferences[USAGE_YEAR_MONTH] = current.yearMonth
            preferences[USAGE_COUNT] = (current.count + 1).coerceAtLeast(0)
        }
    }

    suspend fun getUsage(): AiUsage {
        val now = YearMonth.now().toString()
        val prefs = context.settingsDataStore.data.first()
        val storedYearMonth = prefs[USAGE_YEAR_MONTH] ?: now
        val storedCount = prefs[USAGE_COUNT] ?: 0

        if (storedYearMonth != now) {
            context.settingsDataStore.edit { preferences ->
                preferences[USAGE_YEAR_MONTH] = now
                preferences[USAGE_COUNT] = 0
            }
            return AiUsage(now, 0)
        }

        return AiUsage(storedYearMonth, storedCount)
    }

    data class AiUsage(
        val yearMonth: String,
        val count: Int,
    )

    companion object {
        private const val FREE_LIMIT_PER_MONTH = 5
        private val USAGE_YEAR_MONTH = stringPreferencesKey("ai_usage_year_month")
        private val USAGE_COUNT = intPreferencesKey("ai_usage_count")
    }
}
