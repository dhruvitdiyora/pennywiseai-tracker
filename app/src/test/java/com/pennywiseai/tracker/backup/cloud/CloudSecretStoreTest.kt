package com.pennywiseai.tracker.backup.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CloudSecretStoreTest {

    /** Software AES-GCM standing in for the Keystore key, same output layout. */
    private class SoftwareCipher : SecretCipher {
        private val key = SecretKeySpec(ByteArray(32).also { SecureRandom().nextBytes(it) }, "AES")
        override fun seal(plain: ByteArray, associatedData: ByteArray): ByteArray {
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val c = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv)); updateAAD(associatedData)
            }
            return iv + c.doFinal(plain)
        }
        override fun open(sealed: ByteArray, associatedData: ByteArray): ByteArray {
            val c = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, sealed, 0, 12)); updateAAD(associatedData)
            }
            return c.doFinal(sealed, 12, sealed.size - 12)
        }
    }

    /** A Keystore that is unavailable: every operation throws. */
    private class BrokenCipher : SecretCipher {
        override fun seal(plain: ByteArray, associatedData: ByteArray): ByteArray = throw IllegalStateException("keystore down")
        override fun open(sealed: ByteArray, associatedData: ByteArray): ByteArray = throw IllegalStateException("keystore down")
    }

    private class MapStore(var failWrites: Boolean = false) : SealedValueStore {
        val map = mutableMapOf<String, String>()
        override fun read(key: String) = map[key]
        override fun write(key: String, value: String): Boolean {
            if (failWrites) return false
            map[key] = value
            return true
        }
        override fun remove(keys: Collection<String>): Boolean { keys.forEach(map::remove); return true }
    }

    private fun assertUnavailable(block: () -> Unit) {
        val e = assertThrows(CloudBackupException::class.java) { block() }
        assertEquals(CloudBackupError.SECURE_STORAGE_UNAVAILABLE, e.error)
    }

    @Test
    fun putThenGet_roundTrips_andStoresOnlyCiphertext() {
        val backing = MapStore()
        val store = CloudSecretStore(SoftwareCipher(), backing)
        store.put(CloudSecretStore.WEBDAV_PASSWORD, "hunter2-app-password")
        assertEquals("hunter2-app-password", store.get(CloudSecretStore.WEBDAV_PASSWORD))
        val raw = backing.map.getValue(CloudSecretStore.WEBDAV_PASSWORD)
        assertFalse(raw.contains("hunter2"))
        assertFalse(String(java.util.Base64.getDecoder().decode(raw), Charsets.ISO_8859_1).contains("hunter2"))
    }

    @Test
    fun sealFailure_failsClosed_andWritesNothing() {
        val backing = MapStore()
        val store = CloudSecretStore(BrokenCipher(), backing)
        assertUnavailable { store.put(CloudSecretStore.PASSPHRASE, "a passphrase that must not leak") }
        assertTrue("nothing may be written when sealing fails", backing.map.isEmpty())
    }

    @Test
    fun openFailure_throws_insteadOfReturningStoredBytes() {
        val backing = MapStore()
        CloudSecretStore(SoftwareCipher(), backing).put(CloudSecretStore.PASSPHRASE, "long enough passphrase")
        // Same blobs, but the key is gone (e.g. after a device transfer).
        val afterKeyLoss = CloudSecretStore(SoftwareCipher(), backing)
        assertUnavailable { afterKeyLoss.get(CloudSecretStore.PASSPHRASE) }
        assertUnavailable { CloudSecretStore(BrokenCipher(), backing).get(CloudSecretStore.PASSPHRASE) }
    }

    @Test
    fun failedPersist_isReported() {
        val store = CloudSecretStore(SoftwareCipher(), MapStore(failWrites = true))
        assertUnavailable { store.put(CloudSecretStore.WEBDAV_USERNAME, "someone") }
    }

    @Test
    fun sealedValue_isBoundToItsKey() {
        val backing = MapStore()
        val store = CloudSecretStore(SoftwareCipher(), backing)
        store.put(CloudSecretStore.WEBDAV_PASSWORD, "server password")
        backing.map[CloudSecretStore.PASSPHRASE] = backing.map.getValue(CloudSecretStore.WEBDAV_PASSWORD)
        assertUnavailable { store.get(CloudSecretStore.PASSPHRASE) }
    }

    @Test
    fun missingValue_isNull_andRemoveClears() {
        val store = CloudSecretStore(SoftwareCipher(), MapStore())
        assertNull(store.get(CloudSecretStore.WEBDAV_URL))
        store.put(CloudSecretStore.WEBDAV_URL, "https://dav.example.test/")
        assertTrue(store.contains(CloudSecretStore.WEBDAV_URL))
        store.remove(CloudSecretStore.WEBDAV_URL, CloudSecretStore.WEBDAV_USERNAME)
        assertNull(store.get(CloudSecretStore.WEBDAV_URL))
    }

    @Test
    fun credentialsToString_isRedacted() {
        val text = WebDavCredentials("https://dav.example.test/", "user", "pw-secret").toString()
        assertFalse(text.contains("pw-secret"))
        assertFalse(text.contains("dav.example.test"))
    }
}
