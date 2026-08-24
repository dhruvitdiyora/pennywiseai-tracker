package com.pennywiseai.tracker.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pennywiseai.tracker.MainActivity
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.backup.folder.FolderBackupWriter
import com.pennywiseai.tracker.data.backup.BackupExporter
import com.pennywiseai.tracker.data.backup.ExportBytesResult
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScheduledFolderBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupExporter: BackupExporter,
    private val folderBackupWriter: FolderBackupWriter,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!userPreferencesRepository.isScheduledFolderBackupEnabled()) {
            return Result.success()
        }

        val treeUri = userPreferencesRepository.getScheduledFolderBackupTreeUri()
        if (treeUri.isNullOrBlank()) {
            Log.w(TAG, "Scheduled folder backup enabled but no folder selected")
            return recordFailureAndRetry("No backup folder is selected")
        }

        if (!folderBackupWriter.canWriteToFolder(treeUri)) {
            Log.w(TAG, "Cannot write to scheduled backup folder")
            return recordFailureAndRetry("The backup folder is no longer accessible")
        }

        return when (val exportResult = backupExporter.exportBackupBytes()) {
            is ExportBytesResult.Success -> {
                when (val writeResult = folderBackupWriter.writeBackup(treeUri, exportResult.bytes)) {
                    is FolderBackupWriter.Result.Success -> {
                        userPreferencesRepository.setScheduledFolderBackupLastTimestamp(
                            System.currentTimeMillis()
                        )
                        dismissFailureNotification()
                        Log.i(TAG, "Scheduled folder backup completed")
                        Result.success()
                    }
                    is FolderBackupWriter.Result.Failure -> {
                        Log.e(TAG, "Scheduled folder backup write failed: ${writeResult.message}")
                        recordFailureAndRetry(writeResult.message)
                    }
                }
            }
            is ExportBytesResult.Error -> {
                Log.e(TAG, "Scheduled folder backup export failed: ${exportResult.message}")
                recordFailureAndRetry(exportResult.message)
            }
        }
    }

    private suspend fun recordFailureAndRetry(message: String): Result {
        val failureCount = userPreferencesRepository.recordScheduledFolderBackupFailure(
            message = message,
            timestamp = System.currentTimeMillis()
        )
        if (failureCount >= FAILURES_BEFORE_NOTIFICATION) {
            showFailureNotification()
        }
        return Result.retry()
    }

    private fun showFailureNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.automatic_backup_notification_channel),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val settingsIntent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_SETTINGS, true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.automatic_backup_notification_title))
            .setContentText(applicationContext.getString(R.string.automatic_backup_notification_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun dismissFailureNotification() {
        applicationContext.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    companion object {
        private const val TAG = "ScheduledFolderBackup"
        private const val CHANNEL_ID = "automatic_backup_failures"
        private const val NOTIFICATION_ID = 3_201
        private const val FAILURES_BEFORE_NOTIFICATION = 3
    }
}
