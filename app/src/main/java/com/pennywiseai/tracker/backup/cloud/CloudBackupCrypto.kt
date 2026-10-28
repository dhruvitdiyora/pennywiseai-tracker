package com.pennywiseai.tracker.backup.cloud

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Client-side encryption for cloud backups. Every byte that reaches a cloud
 * provider goes through [seal]; there is no plaintext path.
 *
 * ## Envelope v1 (all integers big-endian)
 * ```
 * offset size  field
 *  0      4    magic "PWCB"
 *  4      1    envelope version (1)
 *  5      1    KDF id (1 = PBKDF2-HMAC-SHA256)
 *  6      4    KDF iterations (u32)
 * 10      1    payload encoding (1 = gzip(PennyWise backup JSON))
 * 11     16    salt (random per file)
 * 27     12    GCM IV (random per file)
 * 39     ..    AES-256-GCM ciphertext || 128-bit tag
 * ```
 * The whole 39-byte header is the GCM associated data, so any change to the
 * version, KDF parameters, salt or IV fails authentication exactly like a
 * change to the ciphertext does.
 *
 * ## KDF choice
 * PBKDF2-HMAC-SHA256 at 600,000 iterations (the OWASP 2023 recommendation for
 * that PRF). Argon2id would be stronger against GPU attackers but needs a
 * native or Bouncy Castle dependency; PBKDF2 ships with every Android 8+ device
 * (`PBKDF2WithHmacSHA256`, API 26) and the JVM, so the same code is tested on
 * the JVM. The iteration count lives in the authenticated header, so a later
 * version can raise it (or add an Argon2id KDF id) without breaking old files.
 * Decryption only accepts counts in [MIN_ITERATIONS]..[MAX_ITERATIONS], so a
 * crafted header can neither downgrade the cost nor stall the device.
 */
class CloudBackupCrypto(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom(),
) {
    init {
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "iterations out of range" }
    }

    /** gzip + encrypt [plaintext] (a PennyWise backup JSON) into an envelope. */
    fun seal(plaintext: ByteArray, passphrase: CharArray): ByteArray {
        val salt = ByteArray(SALT_LENGTH).also(random::nextBytes)
        val iv = ByteArray(IV_LENGTH).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_LENGTH)
            .put(MAGIC)
            .put(VERSION_1)
            .put(KDF_PBKDF2_SHA256)
            .putInt(iterations)
            .put(PAYLOAD_GZIP_JSON)
            .put(salt)
            .put(iv)
            .array()
        val key = deriveKey(passphrase, salt, iterations)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            updateAAD(header)
        }
        val ciphertext = cipher.doFinal(gzip(plaintext))
        return header + ciphertext
    }

    /**
     * Authenticate, decrypt and decompress [envelope]. Nothing is returned
     * unless the GCM tag verifies, so a wrong passphrase and a tampered or
     * truncated file both fail here, before any import starts.
     *
     * @throws CloudBackupException UNSUPPORTED_FORMAT, WRONG_PASSPHRASE_OR_DAMAGED or TOO_LARGE.
     */
    fun open(envelope: ByteArray, passphrase: CharArray, maxPlaintextBytes: Long = MAX_PLAINTEXT_BYTES): ByteArray {
        val header = parseHeader(envelope)
        val key = deriveKey(passphrase, header.salt, header.iterations)
        val compressed = try {
            Cipher.getInstance(TRANSFORMATION).run {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, header.iv))
                updateAAD(envelope, 0, HEADER_LENGTH)
                doFinal(envelope, HEADER_LENGTH, envelope.size - HEADER_LENGTH)
            }
        } catch (e: AEADBadTagException) {
            throw CloudBackupException(CloudBackupError.WRONG_PASSPHRASE_OR_DAMAGED, e)
        } catch (e: java.security.GeneralSecurityException) {
            throw CloudBackupException(CloudBackupError.WRONG_PASSPHRASE_OR_DAMAGED, e)
        }
        return gunzip(compressed, maxPlaintextBytes)
    }

    internal class Header(val iterations: Int, val salt: ByteArray, val iv: ByteArray)

    internal fun parseHeader(envelope: ByteArray): Header {
        if (envelope.size < HEADER_LENGTH + TAG_BITS / 8) unsupported()
        if (!envelope.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) unsupported()
        val buf = ByteBuffer.wrap(envelope, MAGIC.size, HEADER_LENGTH - MAGIC.size)
        if (buf.get() != VERSION_1) unsupported()
        if (buf.get() != KDF_PBKDF2_SHA256) unsupported()
        val iterations = buf.int
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) unsupported()
        if (buf.get() != PAYLOAD_GZIP_JSON) unsupported()
        val salt = ByteArray(SALT_LENGTH).also { buf.get(it) }
        val iv = ByteArray(IV_LENGTH).also { buf.get(it) }
        return Header(iterations, salt, iv)
    }

    private fun unsupported(): Nothing = throw CloudBackupException(CloudBackupError.UNSUPPORTED_FORMAT)

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        try {
            val raw = SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
            return SecretKeySpec(raw, "AES").also { raw.fill(0) }
        } finally {
            spec.clearPassword()
        }
    }

    private fun gzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(maxOf(1024, data.size / 4))
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    /** Decompress with a hard output budget, so a small file can't expand into an OOM. */
    private fun gunzip(data: ByteArray, maxBytes: Long): ByteArray {
        try {
            GZIPInputStream(ByteArrayInputStream(data)).use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > maxBytes) throw CloudBackupException(CloudBackupError.TOO_LARGE)
                    out.write(buffer, 0, n)
                }
                return out.toByteArray()
            }
        } catch (e: CloudBackupException) {
            throw e
        } catch (e: java.io.IOException) {
            // Authenticated but not valid gzip: written by something other than this format.
            throw CloudBackupException(CloudBackupError.UNSUPPORTED_FORMAT, e)
        }
    }

    companion object {
        val MAGIC = byteArrayOf('P'.code.toByte(), 'W'.code.toByte(), 'C'.code.toByte(), 'B'.code.toByte())
        const val VERSION_1: Byte = 1
        const val KDF_PBKDF2_SHA256: Byte = 1
        const val PAYLOAD_GZIP_JSON: Byte = 1
        const val DEFAULT_ITERATIONS = 600_000
        const val MIN_ITERATIONS = 100_000
        const val MAX_ITERATIONS = 10_000_000
        const val SALT_LENGTH = 16
        const val IV_LENGTH = 12
        const val HEADER_LENGTH = 4 + 1 + 1 + 4 + 1 + SALT_LENGTH + IV_LENGTH
        const val TAG_BITS = 128
        private const val KEY_BITS = 256
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"

        /** Largest decompressed backup accepted on restore. */
        const val MAX_PLAINTEXT_BYTES: Long = 512L * 1024 * 1024

        /** Largest envelope uploaded or downloaded. */
        const val MAX_ENVELOPE_BYTES: Long = 256L * 1024 * 1024

        /** Minimum passphrase length the UI accepts. */
        const val MIN_PASSPHRASE_LENGTH = 10
    }
}
