package com.quickmemo.app.worker

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quickmemo.app.data.datastore.settingsDataStore
import com.quickmemo.app.data.local.database.QuickMemoDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

class AppBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val preferences = applicationContext.settingsDataStore.data.first()
            val enabled = preferences[booleanPreferencesKey(KEY_APP_BACKUP_ENABLED)] ?: true
            if (!enabled) {
                return Result.success()
            }

            val maxGenerations = (preferences[intPreferencesKey(KEY_APP_BACKUP_MAX_GEN)] ?: 10)
                .coerceIn(1, 10)

            val dbFile = applicationContext.getDatabasePath(QuickMemoDatabase.DB_NAME)
            if (!dbFile.exists()) {
                return Result.success()
            }

            val backupDir = File(applicationContext.filesDir, BACKUP_DIR_NAME)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val database = QuickMemoDatabase.getInstance(applicationContext)
            database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(backupDir, "quickmemo_backup_${timestamp}.db")
            dbFile.copyTo(backupFile, overwrite = true)

            val walFile = File(dbFile.path + "-wal")
            if (walFile.exists()) {
                walFile.copyTo(File(backupFile.path + "-wal"), overwrite = true)
            }

            val shmFile = File(dbFile.path + "-shm")
            if (shmFile.exists()) {
                shmFile.copyTo(File(backupFile.path + "-shm"), overwrite = true)
            }

            pruneOldBackups(backupDir = backupDir, maxGenerations = maxGenerations)
            updateLastBackupDateTime()

            Log.d(TAG, "App backup created: ${backupFile.name}")
            Result.success()
        }.getOrElse { throwable ->
            Log.e(TAG, "App backup failed", throwable)
            Result.retry()
        }
    }

    private suspend fun updateLastBackupDateTime() {
        val displayTime = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date())
        applicationContext.settingsDataStore.edit { preferences ->
            preferences[stringPreferencesKey(KEY_LAST_BACKUP_DATETIME)] = displayTime
        }
    }

    private fun pruneOldBackups(backupDir: File, maxGenerations: Int) {
        val backups = backupDir
            .listFiles { file ->
                file.isFile &&
                    file.name.startsWith("quickmemo_backup_") &&
                    file.name.endsWith(".db")
            }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        if (backups.size <= maxGenerations) return

        backups.drop(maxGenerations).forEach { file ->
            runCatching { file.delete() }
            runCatching { File(file.path + "-wal").delete() }
            runCatching { File(file.path + "-shm").delete() }
        }
    }

    companion object {
        private const val TAG = "QM_APP_BACKUP"
        private const val BACKUP_DIR_NAME = "app_backups"
        private const val KEY_APP_BACKUP_ENABLED = "app_backup_enabled"
        private const val KEY_APP_BACKUP_MAX_GEN = "app_backup_max_gen"
        private const val KEY_LAST_BACKUP_DATETIME = "last_backup_datetime"
    }
}
