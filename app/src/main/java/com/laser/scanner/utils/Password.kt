package com.laser.scanner.utils

import android.util.Base64
import com.laser.scanner.contract.DEFAULT_PASSWORD
import com.nice.common.helper.decodeBase64
import com.nice.common.helper.encodeBase64ToString
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


fun createCodeString(password: String): String {
    val timestamp = System.currentTimeMillis() + 5 * 60 * 1000
    return AESEncrypt.encrypt(timestamp.toString(), password).orEmpty()
}

fun isPassword(str: String): Boolean = DEFAULT_PASSWORD == str

fun checkCode(str: String): Boolean {
    if (DEFAULT_PASSWORD == str) return true

    val timestamp = AESEncrypt.decrypt(str, DEFAULT_PASSWORD)?.toLongOrNull() ?: return false

    log { "timestamp: $timestamp" }

    return System.currentTimeMillis() <= timestamp
}

object AESEncrypt {

    /**
     * 加密算法
     */
    private const val KEY_ALGORITHM = "AES"

    /**
     * AES 的 密钥长度，32 字节，范围：16 - 32 字节
     */
    private const val SECRET_KEY_LENGTH = 16

    /**
     * 字符编码
     */
    private val CHARSET = StandardCharsets.ISO_8859_1

    /**
     * 秘钥长度不足 16 个字节时，默认填充位数
     */
    private const val DEFAULT_VALUE = "0"

    /**
     * 加解密算法/工作模式/填充方式
     */
    private const val CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"

    private const val IV = "Fdpf5iXs2Ie1ElgC"

    /**
     * AES 加密
     *
     * @param data      待加密内容
     * @param secretKey 加密密码，长度：16 或 32 个字符
     * @return 返回Base64转码后的加密数据
     */
    fun encrypt(data: String, secretKey: String): String? {
        try {
            val cipher: Cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val iv = IvParameterSpec(IV.toByteArray(CHARSET))
            cipher.init(Cipher.ENCRYPT_MODE, createSecretKey(secretKey), iv)
            val encryptBytes = cipher.doFinal(data.toByteArray(CHARSET))
            return encryptBytes.encodeBase64ToString(flag = Base64.NO_WRAP)
        } catch (e: Exception) {
            log(error = e) { e.message }
        }
        return null
    }

    /**
     * AES 解密
     *
     * @param base64Data 加密的密文 Base64 字符串
     * @param secretKey  解密的密钥，长度：16 或 32 个字符
     */
    fun decrypt(base64Data: String, secretKey: String): String? {
        try {
            val cipher: Cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val iv = IvParameterSpec(IV.toByteArray(CHARSET))
            cipher.init(Cipher.DECRYPT_MODE, createSecretKey(secretKey), iv)
            val decryptBytes = cipher.doFinal(base64Data.decodeBase64(flag = Base64.NO_WRAP))
            return decryptBytes.toString(CHARSET)
        } catch (e: Exception) {
            log(error = e) { e.message }
        }
        return null
    }

    /**
     * 使用密码获取 AES 秘钥
     */
    private fun createSecretKey(secretKey: String): SecretKeySpec {
        return SecretKeySpec(
            ensureKey(secretKey).toByteArray(CHARSET),
            KEY_ALGORITHM
        )
    }

    private fun ensureKey(secretKey: String): String {
        val strLen = secretKey.length
        if (strLen < SECRET_KEY_LENGTH) {
            val builder = StringBuilder()
            builder.append(secretKey)
            for (i in 0 until SECRET_KEY_LENGTH - strLen) {
                builder.append(DEFAULT_VALUE)
            }
            return builder.toString()
        }
        return secretKey
    }

}