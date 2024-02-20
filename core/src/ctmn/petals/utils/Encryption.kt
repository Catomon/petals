package ctmn.petals.utils

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.Cipher

fun encryptData(text: String, secretKey: SecretKeySpec): String {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)

    val encryptedBytes = cipher.doFinal(text.toByteArray())
    return Base64.getEncoder().encodeToString(encryptedBytes)
}


fun decryptData(encryptedString: String, secretKey: SecretKeySpec): String {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secretKey)

    val encryptedBytes = Base64.getDecoder().decode(encryptedString)
    val decryptedBytes = cipher.doFinal(encryptedBytes)
    return String(decryptedBytes)
}

fun generateSecretKey(password: String, salt: ByteArray? = ByteArray(1) { 1 }): SecretKeySpec {
    val iterations = 10000
    val keyLength = 256

    val keySpec: KeySpec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)

    val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val secretKey = secretKeyFactory.generateSecret(keySpec)

    return SecretKeySpec(secretKey.encoded, "AES")
}