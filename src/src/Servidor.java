package src;

import java.io.*;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.security.*;

public class Servidor {
    static final Map<String, Usuario> usuarios = new HashMap<>(); // Tabla predefinida de usuarios y paquetes

    public static void main(String[] args) {
        cargarDatosPredefinidos();
        mostrarMenu();
    }

    private static void cargarDatosPredefinidos() {
        // Crear 32 usuarios para cumplir con el enunciado
        for (int i = 1; i <= 32; i++) {
            String idUsuario = "user" + i;
            String idPaquete = "pkg" + i;
            int estado = (i % 6) + 1;  // Asigna un estado numérico entre 1 y 6
            usuarios.put(idUsuario, new Usuario(idUsuario, new Paquete(idPaquete, estado)));
        }
    }

    private static void mostrarMenu() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.println("Seleccione una opción:");
                System.out.println("1. Generar llaves asimétricas");
                System.out.println("2. Iniciar servidor");
                int opcion = Integer.parseInt(br.readLine());

                if (opcion == 1) {
                    generarLlavesAsimetricas();
                } else if (opcion == 2) {
                    iniciarServidor();
                } else {
                    System.out.println("Opción no válida");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generarLlavesAsimetricas() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(1024); // Tamaño de la llave (1024 bits)
            KeyPair parLlaves = generador.generateKeyPair();
            PublicKey llavePublica = parLlaves.getPublic();
            PrivateKey llavePrivada = parLlaves.getPrivate();

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("publicKey.ser"))) {
                oos.writeObject(llavePublica);
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("privateKey.ser"))) {
                oos.writeObject(llavePrivada);
            }
            System.out.println("Llaves asimétricas generadas y guardadas");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] cifrarEstado(String estado, SecretKey claveAES, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, claveAES, iv);
        return cipher.doFinal(estado.getBytes("UTF-8"));
    }

    private static void iniciarServidor() {
        // Aquí se inicia el servidor concurrente que crea un hilo para cada cliente
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Servidor iniciado en el puerto 12345");

            while (true) {
                Socket socketCliente = serverSocket.accept();
                new HiloCliente(socketCliente).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
