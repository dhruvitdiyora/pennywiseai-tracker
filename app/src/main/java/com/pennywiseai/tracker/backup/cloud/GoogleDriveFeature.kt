package com.pennywiseai.tracker.backup.cloud

import android.app.PendingIntent
import android.content.Intent

/**
 * Google Drive support, bound per flavor: the standard build implements it
 * with Google Play services (`drive.appdata` scope only); the F-Droid build
 * binds a stub with [isAvailable] = false and pulls in no Google library.
 */
interface GoogleDriveFeature {
    val isAvailable: Boolean

    /** Starts authorization. May need the user to pick an account / consent first. */
    suspend fun connect(): DriveConnectResult

    /** Completes [DriveConnectResult.NeedsConsent] with the activity result data. */
    suspend fun completeConnect(data: Intent?): DriveConnectResult

    /** Storage backed by the app-private appDataFolder of the connected account. */
    fun provider(): CloudStorageProvider

    /** Revokes the grant (best effort) and forgets the account. */
    suspend fun disconnect()
}

sealed interface DriveConnectResult {
    data class Connected(val accountName: String?) : DriveConnectResult
    class NeedsConsent(val pendingIntent: PendingIntent) : DriveConnectResult
    data class Failed(val error: CloudBackupError) : DriveConnectResult
}
