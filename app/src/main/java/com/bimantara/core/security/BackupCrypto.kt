package com.bimantara.core.security

import android.content.Context
import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Robust AES-256-GCM authenticated encryption utility for Multi-Tools database backups.
 * Features:
 * - 256-bit AES symmetric encryption
 * - 12-byte random IV per backup
 * - 128-bit Galois Counter Mode (GCM) authentication tag
 * - Magic header verification ("MTBKP1")
 */
object BackupCrypto {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private const val KEY_SIZE_BITS = 256
    private val MAGIC_HEADER = "MTBKP1".toByteArray(Charsets.UTF_8)
    private const val KEY_FILE_NAME = ".security_backup_key.bin"

    /**
     * Retrieves or creates a secure 256-bit AES master key in the app's private files directory.
     */
    @Synchronized
    private fun getOrCreateSecretKey(context: Context): SecretKey {
        val securityDir = File(context.filesDir, ".security")
        if (!securityDir.exists()) {
            securityDir.mkdirs()
        }
        val keyFile = File(securityDir, KEY_FILE_NAME)

        if (keyFile.exists() && keyFile.length() == (KEY_SIZE_BITS / 8).toLong()) {
            val keyBytes = keyFile.readBytes()
            return SecretKeySpec(keyBytes, ALGORITHM)
        }

        // Generate a new cryptographically secure 256-bit key
        val keyGen = KeyGenerator.getInstance(ALGORITHM)
        keyGen.init(KEY_SIZE_BITS, SecureRandom())
        val newKey = keyGen.generateKey()

        try {
            FileOutputStream(keyFile).use { it.write(newKey.encoded) }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return newKey
    }

    /**
     * Encrypts the raw JSON string into a binary payload containing:
     * [MAGIC_HEADER (6 bytes)] + [IV (12 bytes)] + [CIPHERTEXT + GCM_TAG]
     */
    fun encryptPayload(context: Context, plainJson: String): ByteArray {
        val key = getOrCreateSecretKey(context)
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val plainBytes = plainJson.toByteArray(Charsets.UTF_8)
        val cipherBytes = cipher.doFinal(plainBytes)

        // Combine Header + IV + CipherText
        val result = ByteArray(MAGIC_HEADER.size + iv.size + cipherBytes.size)
        System.arraycopy(MAGIC_HEADER, 0, result, 0, MAGIC_HEADER.size)
        System.arraycopy(iv, 0, result, MAGIC_HEADER.size, iv.size)
        System.arraycopy(cipherBytes, 0, result, MAGIC_HEADER.size + iv.size, cipherBytes.size)

        return result
    }

    /**
     * Decrypts an encrypted backup binary payload and returns the original JSON string.
     * Throws exception if header is invalid or authentication tag does not match (data corrupted/tampered).
     */
    fun decryptPayload(context: Context, encryptedBytes: ByteArray): String {
        if (encryptedBytes.size < MAGIC_HEADER.size + IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
            throw IllegalArgumentException("Invalid backup file: file too small")
        }

        // Verify magic header
        for (i in MAGIC_HEADER.indices) {
            if (encryptedBytes[i] != MAGIC_HEADER[i]) {
                throw IllegalArgumentException("Invalid backup file: unknown magic header")
            }
        }

        val iv = ByteArray(IV_LENGTH_BYTES)
        System.arraycopy(encryptedBytes, MAGIC_HEADER.size, iv, 0, IV_LENGTH_BYTES)

        val cipherLength = encryptedBytes.size - MAGIC_HEADER.size - IV_LENGTH_BYTES
        val cipherBytes = ByteArray(cipherLength)
        System.arraycopy(encryptedBytes, MAGIC_HEADER.size + IV_LENGTH_BYTES, cipherBytes, 0, cipherLength)

        val key = getOrCreateSecretKey(context)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val decryptedBytes = cipher.doFinal(cipherBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Checks if a file has the valid magic header of an encrypted backup.
     */
    fun isEncryptedBackupFile(file: File): Boolean {
        if (!file.exists() || file.length() < MAGIC_HEADER.size) return false
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(MAGIC_HEADER.size)
                val read = input.read(header)
                read == MAGIC_HEADER.size && header.contentEquals(MAGIC_HEADER)
            }
        } catch (e: Exception) {
            false
        }
    }
}
