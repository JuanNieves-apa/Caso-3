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

public class Cliente implements Runnable {

    private final String idUsuario;
    private final String idPaquete;

    public Cliente(String idUsuario, String idPaquete) {
        this.idUsuario = idUsuario;
        this.idPaquete = idPaquete;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket("localhost", 12345);
             ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream())) {

            // Generación del par de claves DH y envío de la clave pública
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
            keyPairGen.initialize(1024);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            salida.writeObject(keyPair.getPublic());  // Enviar clave pública del cliente
            salida.flush();

            // Recibir la clave pública del servidor
            PublicKey clavePublicaServidor = (PublicKey) entrada.readObject();

            // Generar clave AES compartida
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(keyPair.getPrivate());
            keyAgreement.doPhase(clavePublicaServidor, true);

            byte[] claveCompartida = keyAgreement.generateSecret();
            SecretKey claveAES = new SecretKeySpec(claveCompartida, 0, 16, "AES");

            // Enviar datos de consulta al servidor
            salida.writeObject(idUsuario);
            salida.writeObject(idPaquete);
            salida.flush();

            // Leer la respuesta cifrada del servidor como byte[]
            byte[] respuestaCifrada = (byte[]) entrada.readObject();

            // Configurar el IV (debe coincidir con el IV usado en el servidor)
            IvParameterSpec iv = new IvParameterSpec(new byte[16]);  // Asegúrate de usar el mismo IV que el servidor

            // Descifrar la respuesta
            String respuesta = descifrarRespuesta(respuestaCifrada, claveAES, iv);
            System.out.println("Respuesta del servidor: " + respuesta);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para descifrar la respuesta usando AES
    private String descifrarRespuesta(byte[] respuestaCifrada, SecretKey claveAES, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, claveAES, iv);
        byte[] respuestaDescifrada = cipher.doFinal(respuestaCifrada);
        return new String(respuestaDescifrada, "UTF-8");
    }

    // Método para ejecutar el cliente en modo iterativo (32 consultas consecutivas)
    private static void ejecutarConsultasIterativas(String idUsuario, String idPaquete) {
        for (int i = 0; i < 32; i++) {
            Cliente cliente = new Cliente(idUsuario, idPaquete);
            cliente.run();  // Realiza una solicitud al servidor
        }
    }

    // Método para ejecutar el cliente en modo concurrente con múltiples delegados
    private static void ejecutarConsultasConcurrentes(String idUsuario, String idPaquete, int numDelegados) {
        for (int i = 0; i < numDelegados; i++) {
            new Thread(() -> {
                Cliente cliente = new Cliente(idUsuario, idPaquete);
                cliente.run();
            }).start();
        }
    }

    // Método main que interpreta los argumentos y ejecuta el escenario correspondiente
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java Cliente <idUsuario> <idPaquete> [modo]");
            System.out.println("Modo:");
            System.out.println("  32    -> Ejecuta 32 consultas consecutivas (iterativo)");
            System.out.println("  4, 8, 32 -> Número de delegados para consultas concurrentes (modo concurrente)");
            System.out.println("Si no se especifica el modo, se ejecutará una sola consulta.");
            return;
        }

        String idUsuario = args[0];
        String idPaquete = args[1];
        String modo = args.length > 2 ? args[2] : "1";  // Valor predeterminado de 1 para una consulta única

        try {
            if (modo.equals("32")) {
                ejecutarConsultasIterativas(idUsuario, idPaquete);
            } else {
                int numDelegados = Integer.parseInt(modo);
                if (numDelegados == 1) {
                    new Cliente(idUsuario, idPaquete).run();  // Una sola consulta sin delegados
                } else {
                    ejecutarConsultasConcurrentes(idUsuario, idPaquete, numDelegados);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Modo inválido. Debe ser 32 para iterativo o un número para concurrente (1, 4, 8, o 32).");
        }
    }
}
