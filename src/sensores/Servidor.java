package sensores;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Servidor {

    private static final int TIMEOUT_SECS = 5000;

    private static final int PUERTO = 9999;

    private static int puerto_cliente;

    private static InetAddress ip_cliente;

    private static DatagramSocket socket;

    private static byte[] buffer;

    private static Lectura lectura;

    private static String respuesta;

    private static HashMap<String, ArrayList<Lectura>> lecturas;

    private static boolean generar_errores = false;

    private static final Random RAND = new Random();

    private static boolean validar_args(String[] args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("--help")) {
                System.out.println("Para ejecutar el servidor:");
                System.out.println("\njava -jar servidor.jar [-e]");
                System.out.println("\n-e: Simular errores de red");
                System.out.println("-h: Muestra este mensaje");
                return false;
            }
        }
        if (args.length > 1) {
            System.out.println("¡Demasiados argumentos!");
            System.out.println("Para más información pruebe:\n'java -jar servidor.jar --help'");
            return false;
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("-e")){
                generar_errores = true;
            }else{
                System.out.println("Opción incorrecta");
                System.out.println("Para más información pruebe:\n'java -jar servidor.jar --help'");
                return false;
            }
        }
        return true;
    }

    public static void generar_log(String respuesta, String sensor) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("log", true))) {
            bw.write(sensor + " | " + new Timestamp(System.currentTimeMillis()) + " | " + respuesta + "\n");
        } catch (IOException ex) {
            System.out.println("Error al generar registro en el log");
        }
    }

    public static void generar_log(String respuesta) {
        generar_log(respuesta, "NULL");
    }

    public static void main(String[] args) {
        
        if (!validar_args(args)){
            return;
        }
        
        lecturas = Lectura.cargar_datos();
        int estado = 1;
        while (true) {
            switch (estado) {
                case 1: //Comprobar conexión con el colector
                    try {
                        System.out.println("\n---------------------------------------------\n");
                        socket = new DatagramSocket(PUERTO);
                        socket.setSoTimeout(TIMEOUT_SECS);
                        buffer = new byte[1000];
                        DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                        socket.receive(paquete);
                        puerto_cliente = paquete.getPort();
                        ip_cliente = paquete.getAddress();
                        buffer = "ONLINE".getBytes();
                        paquete = new DatagramPacket(buffer, buffer.length, ip_cliente, puerto_cliente);
                        socket.send(paquete);
                        estado = 2;
                    } catch (IOException ex) {
                        System.out.println("Error al establecer conexión con el colector");
                        socket.close();
                    }
                    break;
                case 2: //Esperando lectura del colector
                    try {
                        System.out.println("Esperando Lectura...");
                        buffer = new byte[1000];
                        DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                        socket.receive(paquete);
                        lectura = new Lectura(paquete.getData());
                        System.out.println("Lectura Recibida: " + lectura.toString().trim());
                        if (!lecturas.keySet().contains(lectura.getSensor())) {
                            lecturas.put(lectura.getSensor(), new ArrayList<>());
                        }
                        if (lecturas.get(lectura.getSensor()).contains(lectura)) {
                            System.out.println("DUPLICADA");
                            respuesta = "DUPLICATED";
                        } else {
                            lecturas.get(lectura.getSensor()).add(lectura);
                            Lectura.guardar_datos(lecturas);
                            respuesta = "OK";
                        }
                        generar_log(respuesta, lectura.getSensor());
                    } catch (Exception ex) {
                        System.out.println("Error en la recepción del paquete de la lectura");
                        respuesta = "FAILED";
                        generar_log(respuesta);
                    }
                    estado = 3;
                    break;
                case 3: //Enviar respuesta al colector
                    try {
                        if (generar_errores) {
                            int delay = RAND.nextInt(15) + 1;
                            System.out.println("Delay de " + delay + " segundo/s");
                            Thread.sleep(delay * 1000);
                        }
                        buffer = respuesta.getBytes();
                        DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, ip_cliente, puerto_cliente);
                        if (!generar_errores || (generar_errores && RAND.nextInt(10) < 7)) {
                            socket.send(paquete);
                            System.out.println("¡Respuesta enviada!");
                        } else {
                            System.out.println("Respuesta NO enviada");
                        }
                        socket.close();
                        estado = 1;
                    } catch (IOException ex) {
                        System.out.println("Error al enviar el paquete de respuesta");
                    } catch (InterruptedException ex) {
                        System.out.println("Error al generar delay");
                    }
                    break;
                default:
                    break;
            }
        }
    }

}
