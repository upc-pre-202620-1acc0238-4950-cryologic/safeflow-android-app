package com.safeflow.app.features.iam.infrastructure.repository

import android.os.Build
import android.util.Base64
import com.safeflow.app.features.iam.domain.PasswordHasher
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject

class LocalPasswordHasher @Inject constructor() : PasswordHasher {
    private fun encode(value: ByteArray) = Base64.encodeToString(value, Base64.NO_WRAP)
    private fun digest(password: String, salt: ByteArray, algorithm: String, iterations: Int): ByteArray {
        val key = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return try { SecretKeyFactory.getInstance(algorithm).generateSecret(key).encoded }
        finally { key.clearPassword() }
    }
    override fun hash(password: String): String {
        val algorithm = if (Build.VERSION.SDK_INT >= 26) "PBKDF2WithHmacSHA256" else "PBKDF2WithHmacSHA1"
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return "$algorithm:120000:${encode(salt)}:${encode(digest(password, salt, algorithm, 120000))}"
    }
    override fun matches(password: String, hash: String): Boolean {
        val parts = hash.split(':')
        if (parts.size != 4) return false
        return MessageDigest.isEqual(Base64.decode(parts[3], Base64.NO_WRAP),
            digest(password, Base64.decode(parts[2], Base64.NO_WRAP), parts[0], parts[1].toInt()))
    }
}
