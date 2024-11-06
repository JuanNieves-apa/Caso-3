package src;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;

public class Cliente {
    public static void main(String[] args) {
        String idUsuario = "user1";
        String idPaquete = "pkg1";


        try (Socket socket = new Socket("localhost", 12345);
             ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream())) {

            // Paso 1: Recibir los parámetros DH del servidor
            BigInteger p = (BigInteger) entrada.readObject();
            BigInteger g = (BigInteger) entrada.readObject();
            DHParameterSpec dhSpec = new DHParameterSpec(p, g);

            // Paso 2: Generar el par de claves DH usando los parámetros recibidos
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
            keyPairGen.initialize(dhSpec);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            
            // Enviar clave pública al servidor
            salida.writeObject(keyPair.getPublic());
            salida.flush();
            
            // Paso 3: Recibir la clave pública del servidor y completar el acuerdo de clave
            PublicKey clavePublicaServidor = (PublicKey) entrada.readObject();
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(keyPair.getPrivate());
            keyAgreement.doPhase(clavePublicaServidor, true);

            // Generar la clave AES compartida
            byte[] claveCompartida = keyAgreement.generateSecret();
            SecretKey claveAES = new SecretKeySpec(claveCompartida, 0, 16, "AES");

            // Paso 4: Enviar `idUsuario` e `idPaquete` al servidor
            salida.writeObject(idUsuario);
            salida.writeObject(idPaquete);
            
            // Paso 5: Recibir y descifrar el estado del paquete
            byte[] estadoCifrado = (byte[]) entrada.readObject();
            String estado = descifrarEstado(estadoCifrado, claveAES);

            if ("DESCONOCIDO".equals(estado)) {
                System.out.println("Error en la consulta");
            } else {
                System.out.println("Estado del paquete: " + estado);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para descifrar el estado del paquete
    private static String descifrarEstado(byte[] estadoCifrado, SecretKey claveAES) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, claveAES);
        byte[] estadoDescifrado = cipher.doFinal(estadoCifrado);
        return new String(estadoDescifrado, "UTF-8");
    }
}
