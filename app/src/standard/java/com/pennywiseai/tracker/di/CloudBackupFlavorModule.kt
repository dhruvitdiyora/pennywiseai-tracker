package com.pennywiseai.tracker.di

import com.pennywiseai.tracker.backup.cloud.GoogleDriveFeature
import com.pennywiseai.tracker.backup.cloud.PlayGoogleDriveFeature
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Standard flavor: Google Drive through Play services. F-Droid binds an unavailable stub. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CloudBackupFlavorModule {
    @Binds
    abstract fun bindGoogleDriveFeature(impl: PlayGoogleDriveFeature): GoogleDriveFeature
}
