package com.xentoryx.labs.reelo.core.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 65536
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    fun hashPassword(password: String): String {
        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        val saltBase64 = Base64.getEncoder().encodeToString(saltBytes)

        val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        val skf = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = skf.generateSecret(spec).encoded
        val hashBase64 = Base64.getEncoder().encodeToString(hashBytes)

        return "$saltBase64:$hashBase64"
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false

        val saltBytes = Base64.getDecoder().decode(parts[0])
        val storedHashBytes = Base64.getDecoder().decode(parts[1])

        val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        val skf = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = skf.generateSecret(spec).encoded

        if (storedHashBytes.size != hashBytes.size) return false
        var result = 0
        for (i in storedHashBytes.indices) {
            result = result or (storedHashBytes[i].toInt() xor hashBytes[i].toInt())
        }
        return result == 0
    }
}
