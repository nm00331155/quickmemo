package com.quickmemo.app.worker

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
        ).build()

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
