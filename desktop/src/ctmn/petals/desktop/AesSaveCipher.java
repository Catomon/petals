package ctmn.petals.desktop;

import ctmn.petals.utils.SaveCipher;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

/** AES cipher used by the desktop build for profiles and story saves. */
public class AesSaveCipher implements SaveCipher {

    @Override
    public String encrypt(String text, String key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(key));
            return Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String decrypt(String text, String key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(key));
            return new String(cipher.doFinal(Base64.getDecoder().decode(text)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SecretKeySpec secretKey(String password) throws Exception {
        byte[] salt = new byte[]{1};
        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(factory.generateSecret(keySpec).getEncoded(), "AES");
    }
}
