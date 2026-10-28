package com.pennywiseai.tracker.backup.cloud

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pennywiseai.tracker.worker.CloudBackupWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Automatic cloud backups: periodic, on any network, not on low battery, bounded retries (see worker). */
@Singleton
class CloudBackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedule(frequency: CloudBackupFrequency) {
        val request = PeriodicWorkRequestBuilder<CloudBackupWorker>(frequency.days, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "cloud_backup_periodic"
        private const val BACKOFF_MINUTES = 30L
    }
}
