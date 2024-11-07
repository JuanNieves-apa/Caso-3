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

class HiloCliente extends Thread {
    private final Socket socket;
    private SecretKey claveAES;

    public HiloCliente(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream())) {

            // Medir tiempo para generar G, P y Gx (clave pública de Diffie-Hellman)
            long tiempoInicioClave = System.nanoTime();
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
            keyPairGen.initialize(1024);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            salida.writeObject(keyPair.getPublic());  // Enviar clave pública del servidor (Gx)
            salida.flush();
            long tiempoFinClave = System.nanoTime();
            System.out.println("Tiempo para generar G, P y Gx: " + (tiempoFinClave - tiempoInicioClave) + " ns");

            // Medir tiempo para responder el reto (generar clave compartida)
            long tiempoInicioReto = System.nanoTime();
            PublicKey clavePublicaCliente = (PublicKey) entrada.readObject();
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(keyPair.getPrivate());
            keyAgreement.doPhase(clavePublicaCliente, true);

            byte[] claveCompartida = keyAgreement.generateSecret();
            claveAES = new SecretKeySpec(claveCompartida, 0, 16, "AES");
            long tiempoFinReto = System.nanoTime();
            System.out.println("Tiempo para responder el reto: " + (tiempoFinReto - tiempoInicioReto) + " ns");

            // Medir tiempo para verificar la consulta
            long tiempoInicioConsulta = System.nanoTime();
            String idUsuario = (String) entrada.readObject();
            String idPaquete = (String) entrada.readObject();
            String estado = procesarConsulta(idUsuario, idPaquete);
            long tiempoFinConsulta = System.nanoTime();
            System.out.println("Tiempo para verificar la consulta: " + (tiempoFinConsulta - tiempoInicioConsulta) + " ns");

            // Enviar respuesta cifrada al cliente
            IvParameterSpec iv = new IvParameterSpec(new byte[16]); // IV de ejemplo
            byte[] respuestaCifrada = cifrarEstado(estado, claveAES, iv);
            salida.writeObject(respuestaCifrada);

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
        Usuario usuario = Servidor.usuarios.get(idUsuario);
        if (usuario != null && usuario.getPaquete().getIdPaquete().equals(idPaquete)) {
            return usuario.getPaquete().estadoTexto();
        }
        return "DESCONOCIDO";
    }

    // Método para cifrar el estado del paquete usando AES
    private byte[] cifrarEstado(String estado, SecretKey claveAES, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, claveAES, iv);
        return cipher.doFinal(estado.getBytes("UTF-8"));
    }
}
