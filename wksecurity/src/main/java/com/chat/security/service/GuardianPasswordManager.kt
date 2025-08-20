package com.chat.security.service

import android.content.Context
import android.util.Base64
import com.chat.base.config.WKSharedPreferencesUtil
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * 监护人密码管理器
 * 负责密码的安全存储、验证和管理
 */
class GuardianPasswordManager private constructor() {

    companion object {
        private val HOLDER = GuardianPasswordManager()
        fun getInstance(): GuardianPasswordManager = HOLDER
        
        private const val GUARDIAN_PASSWORD_KEY = "guardian_password_hash"
        private const val GUARDIAN_SALT_KEY = "guardian_password_salt"
        private const val PASSWORD_ATTEMPTS_KEY = "password_attempts"
        private const val LAST_ATTEMPT_TIME_KEY = "last_attempt_time"
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_TIME = 30 * 60 * 1000L // 30分钟锁定时间
    }

    /**
     * 检查是否已设置监护人密码
     */
    fun hasGuardianPassword(): Boolean {
        return WKSharedPreferencesUtil.getInstance().getString(GUARDIAN_PASSWORD_KEY, "").isNotEmpty()
    }

    /**
     * 设置监护人密码
     */
    fun setGuardianPassword(password: String): Boolean {
        return try {
            val salt = generateSalt()
            val hashedPassword = hashPassword(password, salt)
            
            WKSharedPreferencesUtil.getInstance().putString(GUARDIAN_PASSWORD_KEY, hashedPassword)
            WKSharedPreferencesUtil.getInstance().putString(GUARDIAN_SALT_KEY, salt)
            resetAttempts() // 重置尝试次数
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 验证监护人密码
     */
    fun verifyGuardianPassword(password: String): Boolean {
        if (isLockedOut()) {
            return false
        }

        val storedHash = WKSharedPreferencesUtil.getInstance().getString(GUARDIAN_PASSWORD_KEY, "")
        val salt = WKSharedPreferencesUtil.getInstance().getString(GUARDIAN_SALT_KEY, "")
        
        if (storedHash.isEmpty() || salt.isEmpty()) {
            return false
        }

        val inputHash = hashPassword(password, salt)
        val isValid = storedHash == inputHash
        
        if (isValid) {
            resetAttempts()
        } else {
            incrementAttempts()
        }
        
        return isValid
    }

    /**
     * 检查是否被锁定
     */
    fun isLockedOut(): Boolean {
        val attempts = WKSharedPreferencesUtil.getInstance().getInt(PASSWORD_ATTEMPTS_KEY, 0)
        val lastAttemptTime = WKSharedPreferencesUtil.getInstance().getLong(LAST_ATTEMPT_TIME_KEY, 0)
        
        if (attempts >= MAX_ATTEMPTS) {
            val currentTime = System.currentTimeMillis()
            return currentTime - lastAttemptTime < LOCKOUT_TIME
        }
        return false
    }

    /**
     * 获取剩余锁定时间（分钟）
     */
    fun getRemainingLockoutTime(): Int {
        if (!isLockedOut()) return 0
        
        val lastAttemptTime = WKSharedPreferencesUtil.getInstance().getLong(LAST_ATTEMPT_TIME_KEY, 0)
        val currentTime = System.currentTimeMillis()
        val remainingTime = LOCKOUT_TIME - (currentTime - lastAttemptTime)
        
        return (remainingTime / (60 * 1000)).toInt() + 1
    }

    /**
     * 获取剩余尝试次数
     */
    fun getRemainingAttempts(): Int {
        val attempts = WKSharedPreferencesUtil.getInstance().getInt(PASSWORD_ATTEMPTS_KEY, 0)
        return maxOf(0, MAX_ATTEMPTS - attempts)
    }

    /**
     * 重置监护人密码（用于忘记密码的情况）
     */
    fun resetGuardianPassword() {
        WKSharedPreferencesUtil.getInstance().putString(GUARDIAN_PASSWORD_KEY, "")
        WKSharedPreferencesUtil.getInstance().putString(GUARDIAN_SALT_KEY, "")
        resetAttempts()
    }

    /**
     * 生成随机盐值
     */
    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.DEFAULT)
    }

    /**
     * 使用盐值哈希密码
     */
    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray())
        val hashedBytes = md.digest(password.toByteArray())
        return Base64.encodeToString(hashedBytes, Base64.DEFAULT)
    }

    /**
     * 重置尝试次数
     */
    private fun resetAttempts() {
        WKSharedPreferencesUtil.getInstance().putInt(PASSWORD_ATTEMPTS_KEY, 0)
        WKSharedPreferencesUtil.getInstance().putLong(LAST_ATTEMPT_TIME_KEY, 0)
    }

    /**
     * 增加尝试次数
     */
    private fun incrementAttempts() {
        val currentAttempts = WKSharedPreferencesUtil.getInstance().getInt(PASSWORD_ATTEMPTS_KEY, 0)
        WKSharedPreferencesUtil.getInstance().putInt(PASSWORD_ATTEMPTS_KEY, currentAttempts + 1)
        WKSharedPreferencesUtil.getInstance().putLong(LAST_ATTEMPT_TIME_KEY, System.currentTimeMillis())
    }
}
