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

class HiloCliente extends Thread {
    private Socket socket;
    private SecretKey claveAES;
    
    // Valores de los parámetros DH
    private static final BigInteger P = new BigInteger("F4A9C7C3D431DA5C47C3D5B5047C671DADE4B7A73B5D920FDD5FAF0C2F59005", 16);
    private static final BigInteger G = BigInteger.valueOf(2);

    public HiloCliente(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream())) {

            // Paso 1: Enviar los parámetros DH al cliente
            DHParameterSpec dhSpec = new DHParameterSpec(P, G);
            salida.writeObject(dhSpec);
            salida.flush();

            // Paso 2: Generar el par de claves DH usando los parámetros enviados
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
            keyPairGen.initialize(dhSpec);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            salida.writeObject(keyPair.getPublic()); // Enviar clave pública del servidor
            salida.flush();

            // Paso 3: Recibir la clave pública del cliente y completar el acuerdo de clave
            PublicKey clavePublicaCliente = (PublicKey) entrada.readObject();
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(keyPair.getPrivate());
            keyAgreement.doPhase(clavePublicaCliente, true);
            
            // Generar la clave AES compartida
            byte[] claveCompartida = keyAgreement.generateSecret();
            this.claveAES = new SecretKeySpec(claveCompartida, 0, 16, "AES");

            // Paso 4: Recibir y procesar `idUsuario` y `idPaquete`
            String idUsuario = (String) entrada.readObject();
            String idPaquete = (String) entrada.readObject();
            
            // Consultar el estado del paquete
            Usuario usuario = Servidor.usuarios.get(idUsuario);
            String estado = (usuario != null && usuario.getPaquete().getIdPaquete().equals(idPaquete))
                    ? usuario.getPaquete().getEstado()
                    : "DESCONOCIDO";

            // Cifrar el estado y enviarlo al cliente
            byte[] estadoCifrado = cifrarEstado(estado, claveAES);
            salida.writeObject(estadoCifrado);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] cifrarEstado(String estado, SecretKey claveAES) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, claveAES);
        return cipher.doFinal(estado.getBytes("UTF-8"));
    }
}
