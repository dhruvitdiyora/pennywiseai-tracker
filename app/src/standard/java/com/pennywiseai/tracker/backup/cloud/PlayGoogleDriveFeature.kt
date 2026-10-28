package com.pennywiseai.tracker.backup.cloud

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.pennywiseai.tracker.di.CloudBackupHttpClient
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google Drive through the Play services Authorization API, asking only for
 * `drive.appdata` (the app's own hidden folder; no access to the user's files).
 *
 * PennyWise stores no OAuth token: Play services caches the grant and hands
 * out a short-lived access token on each [token] call. Only the account name
 * is kept (sealed in [CloudSecretStore]) so [disconnect] can revoke the grant.
 *
 * There is no client id or secret in the app. Google matches the request to
 * an Android OAuth client registered for this package name + signing SHA-1
 * (see docs/cloud-backup.md); without one, authorization fails with
 * DEVELOPER_ERROR, reported as DRIVE_NOT_CONFIGURED.
 */
@Singleton
class PlayGoogleDriveFeature @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secrets: CloudSecretStore,
    @CloudBackupHttpClient private val httpClient: HttpClient,
) : GoogleDriveFeature, DriveTokenSource {

    override val isAvailable = true

    private val scope = Scope(GoogleDriveProvider.SCOPE_DRIVE_APPDATA)
    private val authClient get() = Identity.getAuthorizationClient(context)
    private val request: AuthorizationRequest
        get() = AuthorizationRequest.builder().setRequestedScopes(listOf(scope)).build()

    override suspend fun connect(): DriveConnectResult = try {
        handle(authClient.authorize(request).await())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DriveConnectResult.Failed(map(e))
    }

    override suspend fun completeConnect(data: Intent?): DriveConnectResult = try {
        handle(authClient.getAuthorizationResultFromIntent(data))
    } catch (e: Exception) {
        DriveConnectResult.Failed(map(e))
    }

    private fun handle(result: AuthorizationResult): DriveConnectResult {
        val pending = result.pendingIntent
        if (result.hasResolution() && pending != null) return DriveConnectResult.NeedsConsent(pending)
        if (result.accessToken.isNullOrEmpty() || scope.scopeUri !in result.grantedScopes) {
            return DriveConnectResult.Failed(CloudBackupError.AUTH_REQUIRED)
        }
        val account = result.toGoogleSignInAccount()?.email
        if (account != null) {
            // Needed only to revoke on disconnect. If secure storage is unavailable we
            // skip it (never store it in the clear); disconnect then clears the token instead.
            runCatching { secrets.put(CloudSecretStore.DRIVE_ACCOUNT, account) }
        }
        return DriveConnectResult.Connected(account)
    }

    override fun provider(): CloudStorageProvider = GoogleDriveProvider(this, httpClient)

    override suspend fun token(): String {
        val result = try {
            authClient.authorize(request).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw CloudBackupException(map(e), e)
        }
        if (result.hasResolution()) throw CloudBackupException(CloudBackupError.AUTH_REQUIRED)
        return result.accessToken?.takeIf { it.isNotEmpty() }
            ?: throw CloudBackupException(CloudBackupError.AUTH_REQUIRED)
    }

    override suspend fun invalidate(token: String) {
        runCatching { authClient.clearToken(ClearTokenRequest.builder().setToken(token).build()).await() }
    }

    override suspend fun disconnect() {
        val account = runCatching { secrets.get(CloudSecretStore.DRIVE_ACCOUNT) }.getOrNull()
        try {
            if (account != null) {
                authClient.revokeAccess(
                    RevokeAccessRequest.builder()
                        .setAccount(Account(account, "com.google"))
                        .setScopes(listOf(scope))
                        .build()
                ).await()
            } else {
                // No account on file: at least drop the cached token.
                val result = authClient.authorize(request).await()
                result.accessToken?.let { invalidate(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Drive revoke failed: ${map(e)}")
        } finally {
            secrets.remove(CloudSecretStore.DRIVE_ACCOUNT)
        }
    }

    private fun map(e: Exception): CloudBackupError = when (e) {
        is CloudBackupException -> e.error
        is ApiException -> when (e.statusCode) {
            CommonStatusCodes.DEVELOPER_ERROR -> CloudBackupError.DRIVE_NOT_CONFIGURED
            CommonStatusCodes.NETWORK_ERROR, CommonStatusCodes.TIMEOUT -> CloudBackupError.NETWORK
            CommonStatusCodes.CANCELED, CommonStatusCodes.SIGN_IN_REQUIRED -> CloudBackupError.AUTH_REQUIRED
            CommonStatusCodes.API_NOT_CONNECTED, CommonStatusCodes.INTERNAL_ERROR -> CloudBackupError.PROVIDER_UNAVAILABLE
            else -> CloudBackupError.UNKNOWN
        }
        else -> CloudBackupError.UNKNOWN
    }

    private companion object {
        const val TAG = "CloudBackupDrive"
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
