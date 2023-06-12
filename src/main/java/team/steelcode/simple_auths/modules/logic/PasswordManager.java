package team.steelcode.simple_auths.modules.logic;

import org.apache.commons.codec.digest.HmacAlgorithms;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

public class PasswordManager {


    public static String hashPassword(String unhashedPassword) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA256");
            digest.update(unhashedPassword.getBytes(StandardCharsets.UTF_8), 0 , unhashedPassword.length());
            String hashedPassword = DatatypeConverter.printHexBinary(digest.digest());

            return hashedPassword;

        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
