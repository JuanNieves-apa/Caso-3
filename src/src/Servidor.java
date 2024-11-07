package src;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.util.HashMap;
import java.util.Map;

public class Servidor {
    static final Map<String, Usuario> usuarios = new HashMap<>(); // Tabla predefinida de usuarios y paquetes

    public static void main(String[] args) {
        mostrarMenu();
    }

    private static void mostrarMenu() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.println("Seleccione una opción:");
                System.out.println("1. Generar llaves asimétricas");
                System.out.println("2. Iniciar servidor");
               /*  System.out.println("3. Iniciar servidor iterativo (Escenario 1)");
                System.out.println("4. Iniciar servidor concurrente (Escenario 2)");
                System.out.println("5. Medir tiempos de cifrado (Simétrico y Asimétrico)"); */
                int opcion = Integer.parseInt(br.readLine());

                if (opcion == 1) {
                    generarLlavesAsimetricas();
                    //verificarLlavesGeneradas();
                } else if (opcion == 2) {
                    cargarDatosPredefinidos();
                    iniciarServidor();
                } else if (opcion == 3) {
                    cargarDatosPredefinidos();
                    iniciarServidorIterativo();
                } else if (opcion == 4) {
                    cargarDatosPredefinidos();
                    iniciarServidorConcurrente();
                } else if (opcion == 5) {
                    medirTiemposCifrado();
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

    private static void verificarLlavesGeneradas() {
        try (ObjectInputStream oisPublic = new ObjectInputStream(new FileInputStream("publicKey.ser"));
             ObjectInputStream oisPrivate = new ObjectInputStream(new FileInputStream("privateKey.ser"))) {

            PublicKey publicKey = (PublicKey) oisPublic.readObject();
            PrivateKey privateKey = (PrivateKey) oisPrivate.readObject();

            System.out.println("Llave pública cargada: " + publicKey);
            System.out.println("Llave privada cargada: " + privateKey);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cargarDatosPredefinidos() {
        // Crear 32 usuarios para cumplir con el enunciado
        for (int i = 1; i <= 32; i++) {
            String idUsuario = "user" + i;
            String idPaquete = "pkg" + i;
            int estado = (i % 6) + 1;  // Asigna un estado numérico entre 1 y 6
            usuarios.put(idUsuario, new Usuario(idUsuario, new Paquete(idPaquete, estado)));
        }
        System.out.println("Datos predefinidos cargados.");
    }

    public static void iniciarServidor() {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Servidor iniciado en el puerto 12345");

            while (true) {
                Socket socketCliente = serverSocket.accept();
                System.out.println("Cliente conectado");

                // Crear un nuevo hilo para manejar cada cliente
                new HiloCliente(socketCliente).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Escenario 1: Servidor iterativo para pruebas de 32 consultas consecutivas
    public static void iniciarServidorIterativo() {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Servidor iterativo iniciado en el puerto 12345");

            for (int i = 0; i < 32; i++) {
                Socket socketCliente = serverSocket.accept();
                System.out.println("Cliente conectado - Consulta " + (i + 1));

                // Procesa el cliente en el hilo actual (sin delegar)
                HiloCliente cliente = new HiloCliente(socketCliente);
                cliente.run();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Escenario 2: Servidor concurrente para pruebas con múltiples delegados
    public static void iniciarServidorConcurrente() {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Servidor concurrente iniciado en el puerto 12345");

            // Bucle infinito para aceptar conexiones con delegados concurrentes
            while (true) {
                Socket socketCliente = serverSocket.accept();
                System.out.println("Cliente conectado");

                // Crear un nuevo hilo para manejar cada cliente
                new HiloCliente(socketCliente).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void medirTiemposCifrado() {
        try {
            // Mensaje de ejemplo
            String mensaje = "Estado del paquete: ENOFICINA";

            // Cifrado Simétrico (AES)
            SecretKey claveAES = KeyGenerator.getInstance("AES").generateKey();
            IvParameterSpec iv = new IvParameterSpec(new byte[16]);

            long inicioSimetrico = System.nanoTime();
            Cipher cipherSimetrico = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipherSimetrico.init(Cipher.ENCRYPT_MODE, claveAES, iv);
            byte[] mensajeCifradoSimetrico = cipherSimetrico.doFinal(mensaje.getBytes("UTF-8"));
            long finSimetrico = System.nanoTime();

            System.out.println("Tiempo de cifrado simétrico (AES): " + (finSimetrico - inicioSimetrico) + " ns");

            // Cifrado Asimétrico (RSA)
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair keyPair = keyGen.generateKeyPair();
            PublicKey llavePublica = keyPair.getPublic();

            long inicioAsimetrico = System.nanoTime();
            Cipher cipherAsimetrico = Cipher.getInstance("RSA");
            cipherAsimetrico.init(Cipher.ENCRYPT_MODE, llavePublica);
            byte[] mensajeCifradoAsimetrico = cipherAsimetrico.doFinal(mensaje.getBytes("UTF-8"));
            long finAsimetrico = System.nanoTime();

            System.out.println("Tiempo de cifrado asimétrico (RSA): " + (finAsimetrico - inicioAsimetrico) + " ns");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
