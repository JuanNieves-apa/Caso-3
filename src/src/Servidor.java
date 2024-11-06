package src;
import java.io.*;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.security.*;

public class Servidor {
    static final Map<String, Usuario> usuarios = new HashMap<>(); // Tabla predefinida de usuarios y paquetes

    public static void main(String[] args) {
        cargarDatosPredefinidos();
        generarLlavesAsimetricas();
        iniciarServidor();
    }

    private static void cargarDatosPredefinidos() {
        usuarios.put("user1", new Usuario("user1", new Paquete("pkg1", "ENOFICINA")));
        usuarios.put("user2", new Usuario("user2", new Paquete("pkg2", "RECOGIDO")));
        
    }
    
    // Método para cifrar el estado del paquete
    private static byte[] cifrarEstado(String estado, SecretKey claveAES) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, claveAES);
        return cipher.doFinal(estado.getBytes("UTF-8"));
    }
 // Generar las llaves RSA y guardarlas en archivos
    
    private static void generarLlavesAsimetricas() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(1024); // Tamaño de la llave (1024 bits)
            KeyPair parLlaves = generador.generateKeyPair();
            PublicKey llavePublica = parLlaves.getPublic();
            PrivateKey llavePrivada = parLlaves.getPrivate();
            
            // Guardar la llave pública
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("src\\keys\\llavePublic.ser"))) {
                oos.writeObject(llavePublica);
            }
            
            // Guardar la llave privada
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("src\\keys\\llavePrivada.ser"))) {
                oos.writeObject(llavePrivada);
            }
            
            System.out.println("Llaves RSA generadas y guardadas en " + "src\\keys\\llavePublic.ser" + " y " + "src\\keys\\llavePrivada.ser");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void iniciarServidor() {
        try (ServerSocket servidorSocket = new ServerSocket(12345)) {
            System.out.println("Servidor iniciado en el puerto 12345...");
            while (true) {
                Socket clienteSocket = servidorSocket.accept();
                new HiloCliente(clienteSocket).start(); // Manejamos la conexión en un nuevo hilo
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
