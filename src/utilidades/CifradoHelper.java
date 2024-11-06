package utilidades;

import java.math.BigInteger;
import java.security.*;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class CifradoHelper {
    private static final int LONGITUD_LLAVE_AES = 256;
    private static final int LONGITUD_LLAVE_HMAC = 256;

    // Genera la clave maestra usando Diffie-Hellman
    public static SecretKey generarClaveMaestra() throws Exception {
        // Genera el par de llaves Diffie-Hellman
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(1024);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Configura el protocolo DH con el par de llaves y genera la clave maestra
        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
        keyAgreement.init(keyPair.getPrivate());
        byte[] claveMaestra = keyAgreement.generateSecret();

        // Deriva las claves AES y HMAC a partir del hash SHA-512 de la clave maestra
        MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
        byte[] hashClaveMaestra = sha512.digest(claveMaestra);
        
        byte[] claveAES = Arrays.copyOfRange(hashClaveMaestra, 0, LONGITUD_LLAVE_AES / 8);
        byte[] claveHMAC = Arrays.copyOfRange(hashClaveMaestra, LONGITUD_LLAVE_AES / 8, (LONGITUD_LLAVE_AES + LONGITUD_LLAVE_HMAC) / 8);

        return new SecretKeySpec(claveAES, "AES"); // Devuelve solo la clave AES para cifrado
    }
}

