package com.example.kuluckakontrolu.utils

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.kuluckakontrolu.R
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.content.SharedPreferences

object SecurityUtils {
    private const val TAG = "SecurityUtils"
    private const val PREFS_NAME = "SecurityPrefs"
    private const val HASH_ITERATIONS = 10000
    private const val KEY_LENGTH = 256

    /**
     * Şifre doğrulama fonksiyonu
     *
     * @param context Uygulama bağlamı
     * @param password Doğrulanacak şifre
     * @return Şifre doğru ise true, değilse false
     */
    fun validatePassword(context: Context, password: String): Boolean {
        try {
            // Şifre boş ise başarısız
            if (password.isBlank()) {
                return false
            }

            // Şifreyi hemen kontrol et - eğer varsayılan şifre veya master şifre ise
            val defaultPassword = context.getString(R.string.default_password)
            val masterPassword = context.getString(R.string.master_password)

            if (password == defaultPassword || password == masterPassword) {
                return true
            }

            // Kaydedilmiş hash'i kontrol et
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            setupHashedPasswordsIfNeeded(context, prefs)

            val storedHash = prefs.getString("password_hash", "") ?: ""
            val salt = Base64.decode(prefs.getString("password_salt", ""), Base64.DEFAULT)

            // Girilen şifreyi hash'le
            val inputHash = hashPasswordWithSalt(password, salt)

            return inputHash == storedHash
        } catch (e: Exception) {
            Log.e(TAG, "Şifre doğrulama hatası: ${e.message}")

            // Hata durumunda fallback: doğrudan şifreleri kontrol et
            val defaultPassword = context.getString(R.string.default_password)
            val masterPassword = context.getString(R.string.master_password)
            return password == defaultPassword || password == masterPassword
        }
    }

    /**
     * Uygulama ilk çalıştığında varsayılan hash'leri oluştur
     */
    private fun setupHashedPasswordsIfNeeded(context: Context, prefs: SharedPreferences) {
        if (!prefs.contains("password_hash")) {
            try {
                // Tuz oluştur
                val salt = ByteArray(16)
                SecureRandom().nextBytes(salt)
                val saltString = Base64.encodeToString(salt, Base64.DEFAULT)

                // Varsayılan şifreyi hash'le
                val defaultPassword = context.getString(R.string.default_password)
                val passwordHash = hashPasswordWithSalt(defaultPassword, salt)

                // Hash'i kaydet
                prefs.edit()
                    .putString("password_salt", saltString)
                    .putString("password_hash", passwordHash)
                    .apply()

                Log.d(TAG, "Varsayılan şifre hash'i oluşturuldu")
            } catch (e: Exception) {
                Log.e(TAG, "Şifre hash'i oluşturulamadı: ${e.message}")
            }
        }
    }

    /**
     * Şifreyi tuz ile hash'leme
     */
    private fun hashPasswordWithSalt(password: String, salt: ByteArray): String {
        return try {
            val spec = PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hash = factory.generateSecret(spec).encoded
            Base64.encodeToString(hash, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "PBKDF2 hash hatası, SHA-256'ya düşülüyor: ${e.message}")
            legacyHashPassword(password)
        }
    }

    /**
     * Eski tarz basit hash (yedek olarak)
     */
    private fun legacyHashPassword(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}