package com.pennywiseai.tracker.backup.cloud

/**
 * Every way a cloud backup operation can fail, as a closed set.
 *
 * Errors carry only this code, never a server message, URL, path, token or
 * file content, so they are safe to persist (last-error state) and to log.
 */
enum class CloudBackupError(val retryable: Boolean = false) {
    /** No provider is connected. */
    NOT_CONNECTED,

    /** This build has no Google Drive support (F-Droid) or Play services is missing. */
    PROVIDER_UNAVAILABLE,

    /** Google Drive isn't registered for this build (no OAuth client for package + SHA-1). */
    DRIVE_NOT_CONFIGURED,

    /** The provider needs the user to sign in / grant access again. */
    AUTH_REQUIRED,

    /** The server rejected the credentials. */
    AUTH_FAILED,

    /** The WebDAV address isn't a valid https:// URL. */
    INSECURE_URL,

    /** No backup passphrase is set on this device. */
    PASSPHRASE_NOT_SET,

    /** The passphrase is too short. */
    PASSPHRASE_TOO_SHORT,

    /** Android Keystore-backed storage can't be used; secrets are never stored in the clear instead. */
    SECURE_STORAGE_UNAVAILABLE,

    /** GCM authentication failed: wrong passphrase, or the file was altered or truncated. */
    WRONG_PASSPHRASE_OR_DAMAGED,

    /** The file isn't a PennyWise encrypted backup, or uses a newer envelope version. */
    UNSUPPORTED_FORMAT,

    /** The file exceeds the size budget. */
    TOO_LARGE,

    /** The provider has no space left. */
    STORAGE_FULL,

    /** The snapshot no longer exists on the provider. */
    NOT_FOUND,

    NETWORK(retryable = true),
    SERVER(retryable = true),

    /** Building the local backup failed. */
    EXPORT_FAILED,

    /** The decrypted backup couldn't be imported. */
    IMPORT_FAILED,

    UNKNOWN;

    companion object {
        fun fromName(name: String?): CloudBackupError? = entries.firstOrNull { it.name == name }
    }
}

/** A failure with a [CloudBackupError] code. The message is the code name only. */
class CloudBackupException(
    val error: CloudBackupError,
    cause: Throwable? = null,
) : Exception(error.name, cause)
