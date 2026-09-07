package com.dhan.app.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Passphrase-based AES-256-GCM for backup payloads, mirroring WhatsApp's encrypted-backup
 * model: the key is derived from a passphrase the user sets and remembers themselves (never
 * uploaded, never cached server-side). Deliberately NOT derived from the Android Keystore —
 * Keystore keys don't migrate to a new device, which would make "restore on a new device"
 * (an explicit requirement) impossible. [PassphraseStore] separately caches the passphrase
 * ON THIS device (Keystore-backed) purely so scheduled background backups can run without a
 * UI prompt; a fresh device restore still requires the user to type the passphrase in by hand.
 *
 * Output layout: [1-byte version][16-byte salt][12-byte IV][ciphertext+16-byte GCM tag].
 */
object BackupCrypto {
    private const val VERSION: Byte = 1
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 200_000
    private const val KEY_LEN_BITS = 256

    class DecryptionFailedException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun encrypt(plaintext: ByteArray, passphrase: String): ByteArray {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)

        return byteArrayOf(VERSION) + salt + iv + ciphertext
    }

    fun decrypt(payload: ByteArray, passphrase: String): ByteArray {
        if (payload.size < 1 + SALT_LEN + IV_LEN) {
            throw DecryptionFailedException("Backup file is too short to be valid")
        }
        val version = payload[0]
        if (version != VERSION) {
            throw DecryptionFailedException("Unsupported backup format version $version")
        }
        val salt = payload.copyOfRange(1, 1 + SALT_LEN)
        val iv = payload.copyOfRange(1 + SALT_LEN, 1 + SALT_LEN + IV_LEN)
        val ciphertext = payload.copyOfRange(1 + SALT_LEN + IV_LEN, payload.size)
        val key = deriveKey(passphrase, salt)

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            // Wrong passphrase and a corrupted file both fail GCM tag verification the same
            // way — there's no way to tell them apart, so the caller shows one generic message.
            throw DecryptionFailedException("Could not decrypt backup — wrong passphrase or corrupted file", e)
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LEN_BITS)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(raw, "AES")
    }
}
