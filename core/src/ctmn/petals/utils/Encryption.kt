package ctmn.petals.utils

/**
 * Turns saved data into text and back. Desktop installs an AES cipher so save files are not
 * readable; targets without one keep the text as it is.
 */
interface SaveCipher {
    fun encrypt(text: String, key: String): String
    fun decrypt(text: String, key: String): String
}

object PlainTextCipher : SaveCipher {
    override fun encrypt(text: String, key: String): String = text
    override fun decrypt(text: String, key: String): String = text
}

object SaveCrypto : SaveCipher {
    var cipher: SaveCipher = PlainTextCipher

    override fun encrypt(text: String, key: String): String = cipher.encrypt(text, key)

    override fun decrypt(text: String, key: String): String = cipher.decrypt(text, key)
}
