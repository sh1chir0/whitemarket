package ua.sh1chiro.Bot.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Créé par 永愛 Programmeur Suicidaire ャ le 04.01.2026 18:48
 * <p>
 * Quand j’ai écrit ce code, seuls Dieu et moi
 * savions comment il fonctionnait.
 * Maintenant, seul Dieu le sait !
 *
 * @author @Programmeur_Suicidaire ャ
 */

@Component
public class EncryptionUtils {

    private static String secretKey;
    private static final String ALGORITHM = "AES";

    public EncryptionUtils(@Value("${encryption.secret-key}") String secretKey) {
        EncryptionUtils.secretKey = secretKey;
    }

    public static String encrypt(String value){
        try {
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return null;
    }

    public static String decrypt(String encrypted){
        try {
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(original);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return null;
    }
}
