package com.pennywiseai.tracker.backup.cloud

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.security.SecureRandom

class CloudBackupCryptoTest {

    // The minimum accepted cost keeps the suite fast; the default is covered separately.
    private val crypto = CloudBackupCrypto(iterations = CloudBackupCrypto.MIN_ITERATIONS)
    private val passphrase = "correct horse battery staple"
    private val plaintext = """{"_format":"PennyWise Backup v1.3","database":{"transactions":[]}}""".toByteArray()

    private fun seal(data: ByteArray = plaintext) = crypto.seal(data, passphrase.toCharArray())

    private fun assertFails(error: CloudBackupError, block: () -> Unit) {
        val e = assertThrows(CloudBackupException::class.java) { block() }
        assertEquals(error, e.error)
    }

    @Test
    fun roundTrip_returnsOriginalBytes() {
        val envelope = seal()
        assertArrayEquals(plaintext, crypto.open(envelope, passphrase.toCharArray()))
    }

    @Test
    fun roundTrip_largeAndBinaryPayload() {
        val big = ByteArray(3 * 1024 * 1024).also { SecureRandom().nextBytes(it) }
        assertArrayEquals(big, crypto.open(seal(big), passphrase.toCharArray()))
    }

    @Test
    fun envelope_neverContainsPlaintext() {
        val marker = "SECRET-MERCHANT-NAME-0123456789".repeat(20).toByteArray()
        val envelope = seal(marker)
        val text = String(envelope, Charsets.ISO_8859_1)
        assertFalse(text.contains("SECRET-MERCHANT-NAME"))
        assertFalse(text.contains("PennyWise Backup"))
    }

    @Test
    fun header_hasVersionedLayout() {
        val envelope = seal()
        assertArrayEquals("PWCB".toByteArray(), envelope.copyOfRange(0, 4))
        assertEquals(1, envelope[4].toInt()) // envelope version
        assertEquals(1, envelope[5].toInt()) // PBKDF2-HMAC-SHA256
        assertEquals(CloudBackupCrypto.MIN_ITERATIONS, ByteBuffer.wrap(envelope, 6, 4).int)
        assertEquals(1, envelope[10].toInt()) // gzip JSON payload
        assertEquals(39, CloudBackupCrypto.HEADER_LENGTH)
    }

    @Test
    fun saltAndIv_areFreshForEverySeal() {
        val a = seal()
        val b = seal()
        val saltIv = 11 until CloudBackupCrypto.HEADER_LENGTH
        assertFalse(a.sliceArray(saltIv).contentEquals(b.sliceArray(saltIv)))
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun defaultCost_isOwaspPbkdf2Sha256Level() {
        assertEquals(600_000, CloudBackupCrypto.DEFAULT_ITERATIONS)
        val envelope = CloudBackupCrypto().seal(plaintext, passphrase.toCharArray())
        assertEquals(600_000, ByteBuffer.wrap(envelope, 6, 4).int)
        assertArrayEquals(plaintext, crypto.open(envelope, passphrase.toCharArray()))
    }

    @Test
    fun wrongPassphrase_isRejected() {
        val envelope = seal()
        assertFails(CloudBackupError.WRONG_PASSPHRASE_OR_DAMAGED) {
            crypto.open(envelope, "correct horse battery stapler".toCharArray())
        }
    }

    @Test
    fun tamperedCiphertext_isRejected() {
        val envelope = seal()
        envelope[envelope.size - 20] = (envelope[envelope.size - 20].toInt() xor 0x01).toByte()
        assertFails(CloudBackupError.WRONG_PASSPHRASE_OR_DAMAGED) { crypto.open(envelope, passphrase.toCharArray()) }
    }

    @Test
    fun tamperedTag_isRejected() {
        val envelope = seal()
        envelope[envelope.size - 1] = (envelope[envelope.size - 1].toInt() xor 0x80).toByte()
        assertFails(CloudBackupError.WRONG_PASSPHRASE_OR_DAMAGED) { crypto.open(envelope, passphrase.toCharArray()) }
    }

    @Test
    fun tamperedHeader_isAuthenticated() {
        // Salt and IV changes are caught by GCM (the header is associated data).
        for (offset in listOf(11, 26, 27, 38)) {
            val envelope = seal()
            envelope[offset] = (envelope[offset].toInt() xor 0x01).toByte()
            assertFails(CloudBackupError.WRONG_PASSPHRASE_OR_DAMAGED) { crypto.open(envelope, passphrase.toCharArray()) }
        }
        // An iteration count edited to another in-range value is caught too.
        val envelope = seal()
        ByteBuffer.wrap(envelope).putInt(6, CloudBackupCrypto.MIN_ITERATIONS + 1)
        assertFails(CloudBackupError.WRONG_PASSPHRASE_OR_DAMAGED) { crypto.open(envelope, passphrase.toCharArray()) }
    }

    @Test
    fun truncatedEnvelope_isRejected() {
        val envelope = seal()
        assertFails(CloudBackupError.WRONG_PASSPHRASE_OR_DAMAGED) {
            crypto.open(envelope.copyOf(envelope.size - 5), passphrase.toCharArray())
        }
        assertFails(CloudBackupError.UNSUPPORTED_FORMAT) {
            crypto.open(envelope.copyOf(CloudBackupCrypto.HEADER_LENGTH + 3), passphrase.toCharArray())
        }
        assertFails(CloudBackupError.UNSUPPORTED_FORMAT) { crypto.open(ByteArray(0), passphrase.toCharArray()) }
    }

    @Test
    fun plaintextJsonBackup_isNotAcceptedAsEnvelope() {
        assertFails(CloudBackupError.UNSUPPORTED_FORMAT) {
            crypto.open(plaintext + ByteArray(64), passphrase.toCharArray())
        }
    }

    @Test
    fun unknownVersionKdfOrPayload_isUnsupported() {
        for ((offset, value) in listOf(4 to 2, 5 to 9, 10 to 7)) {
            val envelope = seal()
            envelope[offset] = value.toByte()
            assertFails(CloudBackupError.UNSUPPORTED_FORMAT) { crypto.open(envelope, passphrase.toCharArray()) }
        }
    }

    @Test
    fun iterationCountOutsideBounds_isRefusedBeforeDerivation() {
        for (iterations in listOf(1, 99_999, 10_000_001, Int.MAX_VALUE, -1)) {
            val envelope = seal()
            ByteBuffer.wrap(envelope).putInt(6, iterations)
            assertFails(CloudBackupError.UNSUPPORTED_FORMAT) { crypto.open(envelope, passphrase.toCharArray()) }
        }
        assertThrows(IllegalArgumentException::class.java) { CloudBackupCrypto(iterations = 1_000) }
    }

    @Test
    fun decompressedSize_isCapped() {
        val zeros = ByteArray(2 * 1024 * 1024) // compresses to a few KB
        val envelope = seal(zeros)
        assertTrue(envelope.size < 64 * 1024)
        assertFails(CloudBackupError.TOO_LARGE) {
            crypto.open(envelope, passphrase.toCharArray(), maxPlaintextBytes = 1024 * 1024)
        }
    }

    /**
     * Known-answer fixture: an envelope written by envelope v1 code. If this
     * stops decrypting, files already in users' clouds have become unreadable.
     */
    @Test
    fun knownAnswer_v1EnvelopeStillOpens() {
        val envelope = hex(KAT_ENVELOPE_HEX)
        assertArrayEquals(KAT_PLAINTEXT.toByteArray(), crypto.open(envelope, KAT_PASSPHRASE.toCharArray()))
        assertFails(CloudBackupError.WRONG_PASSPHRASE_OR_DAMAGED) { crypto.open(envelope, "not the passphrase".toCharArray()) }
    }

    private fun hex(s: String): ByteArray = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private companion object {
        const val KAT_PASSPHRASE = "pennywise kat passphrase"
        const val KAT_PLAINTEXT = """{"_format":"PennyWise Backup v1.3"}"""
        const val KAT_ENVELOPE_HEX =
            "505743420101000186a0010102030405060708090a0b0c0d0e0f10a0a1a2a3a4a5a6a7a8a9aaab" +
                "0d00185d21341c2096c9910b59ca694cc83f52b8fd61b848258563d71fcb1ca3d34b0fbaab3d94fc" +
                "c641ed58398111b4658dbf04b4b278a891362bbe02e62c58cffec48b104e79"
    }
}
