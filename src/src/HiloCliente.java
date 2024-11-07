package src;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Map;

public class HiloCliente extends Thread {
    private final Socket socket;
    private SecretKey claveAES;

    public HiloCliente(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream())) {

            // Generación del par de claves DH y envío de la clave pública al cliente
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
            keyPairGen.initialize(1024);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            salida.writeObject(keyPair.getPublic());  // Enviar clave pública del servidor
            salida.flush();

            // Recibir la clave pública del cliente
            PublicKey clavePublicaCliente = (PublicKey) entrada.readObject();

            // Generar clave AES compartida
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(keyPair.getPrivate());
            keyAgreement.doPhase(clavePublicaCliente, true);

            byte[] claveCompartida = keyAgreement.generateSecret();
            claveAES = new SecretKeySpec(claveCompartida, 0, 16, "AES");

            // Leer idUsuario y idPaquete del cliente
            String idUsuario = (String) entrada.readObject();
            String idPaquete = (String) entrada.readObject();

            // Obtener el estado del paquete
            String estado = procesarConsulta(idUsuario, idPaquete);

            // Cifrar el estado del paquete
            IvParameterSpec iv = new IvParameterSpec(new byte[16]); // IV de ejemplo (debería ser aleatorio)
            byte[] respuestaCifrada = cifrarEstado(estado, claveAES, iv);
            salida.writeObject(respuestaCifrada);  // Enviar respuesta cifrada al cliente

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Método para obtener el estado del paquete en función de idUsuario y idPaquete
    private String procesarConsulta(String idUsuario, String idPaquete) {
        // Acceso a la tabla de usuarios en Servidor
        Usuario usuario = Servidor.usuarios.get(idUsuario);
        if (usuario != null && usuario.getPaquete().getIdPaquete().equals(idPaquete)) {
            return usuario.getPaquete().estadoTexto(); // Retorna el estado como texto
        }
        return "DESCONOCIDO"; // Retorna "DESCONOCIDO" si no se encuentra el paquete
    }

    private byte[] cifrarEstado(String estado, SecretKey claveAES, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, claveAES, iv);
        return cipher.doFinal(estado.getBytes("UTF-8"));
    }
}
