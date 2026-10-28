package com.pennywiseai.tracker.backup.cloud

import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton

/** F-Droid: no Google Play services, so no Google Drive. WebDAV remains available. */
@Singleton
class UnavailableGoogleDriveFeature @Inject constructor() : GoogleDriveFeature {
    override val isAvailable = false

    override suspend fun connect(): DriveConnectResult =
        DriveConnectResult.Failed(CloudBackupError.PROVIDER_UNAVAILABLE)

    override suspend fun completeConnect(data: Intent?): DriveConnectResult =
        DriveConnectResult.Failed(CloudBackupError.PROVIDER_UNAVAILABLE)

    override fun provider(): CloudStorageProvider =
        throw CloudBackupException(CloudBackupError.PROVIDER_UNAVAILABLE)

    override suspend fun disconnect() = Unit
}
