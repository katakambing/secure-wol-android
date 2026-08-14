package com.securewol.app.core.security

import android.util.Base64
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.util.Arrays
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PinCryptoHelper: Implements secure password-based key derivation using PBKDF2WithHmacSHA256.
 * - Enforces strong salt generation using SecureRandom (32 bytes).
 * - High iteration count (100,000 iterations).
 * - Constant-time hash verification via MessageDigest.isEqual.
 * - Memory zeroing of sensitive buffers.
 */
object PinCryptoHelper {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH = 256 // bits
    private const val SALT_LENGTH = 32 // bytes

    /**
     * Generates a cryptographically strong 32-byte salt.
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Encodes byte array to Base64 string for storage.
     */
    fun encodeBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Decodes Base64 string to byte array.
     */
    fun decodeBase64(base64Str: String): ByteArray {
        return Base64.decode(base64Str, Base64.NO_WRAP)
    }

    /**
     * Hashes a PIN or password char array using PBKDF2 with salt.
     */
    fun hashPin(pinChars: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pinChars, salt, ITERATIONS, KEY_LENGTH)
        return try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            factory.generateSecret(spec).encoded
        } catch (e: NoSuchAlgorithmException) {
            throw SecurityException("Cryptographic algorithm unavailable", e)
        } catch (e: InvalidKeySpecException) {
            throw SecurityException("Invalid key specification", e)
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Verifies an entered PIN against a stored hash and salt in constant time.
     */
    fun verifyPin(enteredPin: CharArray, storedHashBase64: String, storedSaltBase64: String): Boolean {
        if (storedHashBase64.isBlank() || storedSaltBase64.isBlank()) {
            return false
        }
        return try {
            val salt = decodeBase64(storedSaltBase64)
            val expectedHash = decodeBase64(storedHashBase64)
            val calculatedHash = hashPin(enteredPin, salt)
            
            // Constant-time comparison to prevent timing attacks
            val match = MessageDigest.isEqual(calculatedHash, expectedHash)
            
            // Zero memory
            Arrays.fill(calculatedHash, 0.toByte())
            match
        } catch (e: Exception) {
            SecureLogger.e("PIN verification error", e)
            false
        }
    }

    /**
     * Clears sensitive CharArray from memory.
     */
    fun clearChars(chars: CharArray) {
        Arrays.fill(chars, '0')
    }
}
