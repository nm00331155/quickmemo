package com.quickmemo.app.worker

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quickmemo.app.data.local.database.DatabaseMigrations
import com.quickmemo.app.data.local.database.QuickMemoDatabase
import java.util.concurrent.TimeUnit

class TrashCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = Room.databaseBuilder(
            applicationContext,
            QuickMemoDatabase::class.java,
            QuickMemoDatabase.DB_NAME,
        )
            .addMigrations(
                DatabaseMigrations.MIGRATION_1_2,
                DatabaseMigrations.MIGRATION_2_3,
                DatabaseMigrations.MIGRATION_3_4,
                DatabaseMigrations.MIGRATION_4_5,
                DatabaseMigrations.MIGRATION_5_6,
                DatabaseMigrations.MIGRATION_6_7,
                DatabaseMigrations.MIGRATION_7_8,
                DatabaseMigrations.MIGRATION_8_9,
            )
            .build()

        return kotlin.runCatching {
            val threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            db.memoDao().deleteExpiredDeletedMemos(threshold)
            db.close()
            Result.success()
        }.getOrElse {
            db.close()
            Result.retry()
        }
    }
}
