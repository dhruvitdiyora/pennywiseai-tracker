package com.pennywiseai.tracker.di

import android.content.Context
import com.pennywiseai.tracker.backup.cloud.AndroidKeystoreSecretCipher
import com.pennywiseai.tracker.backup.cloud.CloudBackupCrypto
import com.pennywiseai.tracker.backup.cloud.CloudSecretStore
import com.pennywiseai.tracker.backup.cloud.SharedPreferencesSealedValueStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import javax.inject.Qualifier
import javax.inject.Singleton

/** The HTTP client cloud providers use: no redirects (credentials stay on the host the user chose). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CloudBackupHttpClient

@Module
@InstallIn(SingletonComponent::class)
object CloudBackupModule {

    @Provides
    @Singleton
    fun provideCloudSecretStore(@ApplicationContext context: Context): CloudSecretStore = CloudSecretStore(
        cipher = AndroidKeystoreSecretCipher(),
        store = SharedPreferencesSealedValueStore(
            context.getSharedPreferences(SharedPreferencesSealedValueStore.FILE_NAME, Context.MODE_PRIVATE)
        ),
    )

    @Provides
    @Singleton
    fun provideCloudBackupCrypto(): CloudBackupCrypto = CloudBackupCrypto()

    @Provides
    @Singleton
    @CloudBackupHttpClient
    fun provideCloudHttpClient(): HttpClient = HttpClient(OkHttp) {
        followRedirects = false
        expectSuccess = false
        engine {
            config {
                followRedirects(false)
                followSslRedirects(false)
            }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 60_000
            requestTimeoutMillis = 15 * 60_000
        }
    }
}
