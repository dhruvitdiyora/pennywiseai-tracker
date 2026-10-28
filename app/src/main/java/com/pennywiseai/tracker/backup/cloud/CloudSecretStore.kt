package com.pennywiseai.tracker.backup.cloud

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Seals/opens small secrets. Implementations throw on any failure; they never pass data through. */
interface SecretCipher {
    fun seal(plain: ByteArray, associatedData: ByteArray): ByteArray
    fun open(sealed: ByteArray, associatedData: ByteArray): ByteArray
}

/** Where sealed (already encrypted, Base64) values are kept. Never sees plaintext. */
interface SealedValueStore {
    fun read(key: String): String?
    /** @return false if the write did not persist. */
    fun write(key: String, value: String): Boolean
    fun remove(keys: Collection<String>): Boolean
}

/**
 * Secure storage for cloud credentials and the backup passphrase.
 *
 * Fails closed: if the Keystore cipher can't seal or open a value, the call
 * throws [CloudBackupException] with SECURE_STORAGE_UNAVAILABLE. There is no
 * fallback to plain preferences, and a failed seal writes nothing. A sealed
 * value is bound to its key name (GCM associated data), so blobs can't be
 * swapped between entries.
 */
class CloudSecretStore(
    private val cipher: SecretCipher,
    private val store: SealedValueStore,
) {
    fun put(key: String, value: String) {
        val sealed = try {
            cipher.seal(value.toByteArray(Charsets.UTF_8), key.toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            throw CloudBackupException(CloudBackupError.SECURE_STORAGE_UNAVAILABLE, e)
        }
        if (!store.write(key, Base64.getEncoder().encodeToString(sealed))) {
            throw CloudBackupException(CloudBackupError.SECURE_STORAGE_UNAVAILABLE)
        }
    }

    /**
     * @return the value, or null if none is stored.
     * @throws CloudBackupException SECURE_STORAGE_UNAVAILABLE when a value exists but can't be
     * opened (Keystore unavailable, or the key was lost, e.g. after a device transfer).
     */
    fun get(key: String): String? {
        val encoded = store.read(key) ?: return null
        return try {
            val plain = cipher.open(Base64.getDecoder().decode(encoded), key.toByteArray(Charsets.UTF_8))
            String(plain, Charsets.UTF_8)
        } catch (e: Exception) {
            throw CloudBackupException(CloudBackupError.SECURE_STORAGE_UNAVAILABLE, e)
        }
    }

    fun contains(key: String): Boolean = store.read(key) != null

    fun remove(vararg keys: String) {
        store.remove(keys.toList())
    }

    companion object {
        const val WEBDAV_URL = "webdav_url"
        const val WEBDAV_USERNAME = "webdav_username"
        const val WEBDAV_PASSWORD = "webdav_password"
        const val DRIVE_ACCOUNT = "drive_account"
        const val PASSPHRASE = "backup_passphrase"
    }
}

/** AES-256-GCM with a non-exportable Android Keystore key. Output: iv(12) || ciphertext+tag. */
class AndroidKeystoreSecretCipher(private val alias: String = ALIAS) : SecretCipher {

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    override fun seal(plain: ByteArray, associatedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key())
            updateAAD(associatedData)
        }
        val out = cipher.doFinal(plain)
        val iv = cipher.iv
        require(iv.size == IV_LENGTH)
        return ByteBuffer.allocate(iv.size + out.size).put(iv).put(out).array()
    }

    override fun open(sealed: ByteArray, associatedData: ByteArray): ByteArray {
        require(sealed.size > IV_LENGTH)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, sealed, 0, IV_LENGTH))
            updateAAD(associatedData)
        }
        return cipher.doFinal(sealed, IV_LENGTH, sealed.size - IV_LENGTH)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "pennywise_cloud_backup_secrets"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
    }
}

/** Sealed values in a dedicated preferences file, excluded from Android backup / device transfer. */
class SharedPreferencesSealedValueStore(private val prefs: SharedPreferences) : SealedValueStore {
    override fun read(key: String): String? = prefs.getString(key, null)
    override fun write(key: String, value: String): Boolean = prefs.edit().putString(key, value).commit()
    override fun remove(keys: Collection<String>): Boolean =
        prefs.edit().apply { keys.forEach(::remove) }.commit()

    companion object {
        /** Keep in sync with res/xml/data_extraction_rules.xml and backup_rules.xml. */
        const val FILE_NAME = "cloud_backup_secrets"
    }
}
