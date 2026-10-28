package com.pennywiseai.tracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pennywiseai.tracker.backup.cloud.CloudBackupException
import com.pennywiseai.tracker.backup.cloud.CloudBackupManager
import com.pennywiseai.tracker.backup.cloud.CloudBackupSettings
import com.pennywiseai.tracker.backup.cloud.CloudProviderType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * One automatic cloud backup. Network and server errors retry with
 * exponential backoff, at most [MAX_ATTEMPTS] times per period; everything
 * else (sign-in needed, no passphrase, …) stops at once and shows on the
 * Cloud backup screen until the user fixes it.
 */
@HiltWorker
class CloudBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val manager: CloudBackupManager,
    private val settings: CloudBackupSettings,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val config = settings.current()
        if (!config.autoBackupEnabled || config.provider == CloudProviderType.NONE) return Result.success()
        return try {
            manager.backupNow()
            Result.success()
        } catch (e: CloudBackupException) {
            if (shouldRetry(e.error.retryable, runAttemptCount)) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val MAX_ATTEMPTS = 3

        internal fun shouldRetry(retryable: Boolean, runAttemptCount: Int): Boolean =
            retryable && runAttemptCount + 1 < MAX_ATTEMPTS
    }
}
