package com.pennywiseai.tracker.di

import com.pennywiseai.tracker.backup.cloud.GoogleDriveFeature
import com.pennywiseai.tracker.backup.cloud.UnavailableGoogleDriveFeature
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** F-Droid: Google Drive is unavailable; the standard flavor binds the Play services implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CloudBackupFlavorModule {
    @Binds
    abstract fun bindGoogleDriveFeature(impl: UnavailableGoogleDriveFeature): GoogleDriveFeature
}
