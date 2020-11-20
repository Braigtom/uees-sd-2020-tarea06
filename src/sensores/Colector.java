package sensores;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Colector {

    private static final int TIMEOUT_SECS = 5000;

    private static final int TIEMPO_ESPERA = 5000;

    private static String ip_str;

    private static InetAddress ip_servidor;

    private static final int PUERTO_SERVIDOR = 9999;

    private static DatagramSocket socket;

    private static byte[] buffer;

    private static final String[] SENSORES = {"S01", "S02", "S03", "S04", "S05"};

    private static ArrayList<Lectura> no_enviadas;

    private static final String ARCHIVO_NO_ENVIADAS = "no_enviadas.data";

    private static Lectura lectura;

    private static boolean generar_errores = false;

    private static final Random RAND = new Random();

    private static boolean validar_args(String[] args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("--help")) {
                System.out.println("Para ejecutar el colector:");
                System.out.println("\njava -jar colector.jar <IP> [-e]");
                System.out.println("\n-e: Simular errores de red");
                System.out.println("-h: Muestra este mensaje");
                return false;
            }
        }
        if (args.length == 0) {
            System.out.println("Necesita ingresar la dirección IP");
            System.out.println("'java -jar colector.jar <IP>'");
        } else if (args.length > 2) {
            System.out.println("¡Demasiados argumentos!");
            System.out.println("Para más información pruebe:\n'java -jar colector.jar --help'");
        } else {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("-e")) {
                    System.out.println("Necesita ingresar la dirección IP");
                    System.out.println("'java -jar colector.jar <IP> -e'");
                    return false;
                }
            } else {

                String numeros = "0123456789";

                if (numeros.contains(args[1].substring(0, 1)) || args[1].equalsIgnoreCase("localhost")) {
                    String tmp = args[1];
                    args[1] = args[0];
                    args[0] = tmp;
                }

                if (args[1].equalsIgnoreCase("-e")) {
                    generar_errores = true;
                } else {
                    System.out.println("Opción incorrecta");
                    System.out.println("Para más información pruebe:\n'java -jar colector.jar --help'");
                    return false;
                }
            }

            if (args[0].equalsIgnoreCase("localhost")) {
                args[0] = "127.0.0.1";
            }

            Matcher matcher = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})").matcher(args[0]);
            if (matcher.find()) {
                ip_str = matcher.group();
                for (String b : ip_str.split("\\.")) {
                    if (Integer.parseInt(b) > 255) {
                        System.out.println("¡IP incorrecta!");
                        return false;
                    }
                }
                return true;
            } else {
                System.out.println("¡IP incorrecta!");
            }
        }
        return false;
    }

    public static void guardar_no_enviadas() {
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(ARCHIVO_NO_ENVIADAS))) {
            output.writeObject(no_enviadas);
        } catch (Exception ex) {
            System.out.println("No se pudieron respaldar las lecturas no enviadas");
        }
    }

    public static ArrayList<Lectura> cargar_no_enviadas() {
        ArrayList<Lectura> no_enviadas;
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(ARCHIVO_NO_ENVIADAS))) {
            no_enviadas = (ArrayList<Lectura>) input.readObject();
        } catch (Exception ex) {
            no_enviadas = new ArrayList<>();
        }

        return no_enviadas;
    }

    public static void main(String[] args) {

        //Comentar para usar sin Netbeans
        //String[] a = {"localhost"};
        //args = a;
        /////////////////////////////////

        if (!validar_args(args)) {
            return;
        }

        try {
            ip_servidor = InetAddress.getByName(ip_str);
        } catch (UnknownHostException ex) {
            System.out.println("Error al obtener la direccion IP");
            return;
        }
        no_enviadas = cargar_no_enviadas();
        Iterator<String> itr = Arrays.stream(SENSORES).iterator();
        int estado = 1;
        while (true) {
            switch (estado) {
                case 1://Comprobar conexión con el servidor
                    try {
                        System.out.println("\n---------------------------------------------\n");
                        socket = new DatagramSocket();
                        socket.setSoTimeout(TIMEOUT_SECS);
                        buffer = "ONLINE".getBytes();
                        DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, ip_servidor, PUERTO_SERVIDOR);
                        socket.send(paquete);
                        buffer = new byte[10];
                        paquete = new DatagramPacket(buffer, buffer.length);
                        socket.receive(paquete);
                        estado = 2;
                    } catch (IOException ex) {
                        System.out.println("Error al establecer conexión con el servidor");
                        socket.close();
                    }
                    break;
                case 2: //Verificar si hay lecturas no enviadas
                    if (no_enviadas.isEmpty()) {
                        estado = 3;
                    } else {
                        lectura = no_enviadas.remove(0);
                        System.out.println("Se intentará reenviar la siguiente lectura:");
                        System.out.println(lectura);
                        estado = 4;
                    }
                    break;
                case 3: //Generar lecturas aleatorias
                    if (itr.hasNext()) {
                        lectura = new Lectura(itr.next());
                        System.out.println("Se intentará enviar la siguiente lectura:");
                        System.out.println(lectura.toString().trim());
                        estado = 4;
                    } else {
                        System.out.println("Esperando la siguiente lectura de sensores...");
                        try {
                            Thread.sleep(TIEMPO_ESPERA);
                        } catch (InterruptedException ex) {
                            System.out.println("¡Error en el proceso de espera!");
                        }
                        itr = Arrays.stream(SENSORES).iterator();
                    }
                    break;
                case 4: //Enviando lectura al servidor
                    try {
                        buffer = lectura.getBytes();
                        DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, ip_servidor, PUERTO_SERVIDOR);
                        if (!generar_errores || (generar_errores && RAND.nextInt(10) < 7)) {
                            socket.send(paquete);
                            System.out.println("¡Lectura enviada!");
                            guardar_no_enviadas();
                            if (no_enviadas.isEmpty()) {
                                File fichero = new File(ARCHIVO_NO_ENVIADAS);
                                fichero.delete();
                            }
                        } else {
                            System.out.println("Paquete perdido");
                        }
                        estado = 5;
                    } catch (IOException ex) {
                        estado = 6;
                        System.out.println("Error enviando el paquete");
                    }
                    break;
                case 5: //Recibiendo respuesta del servidor
                    try {
                        buffer = new byte[10];
                        DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                        socket.receive(paquete);
                        String respuesta = new String(paquete.getData());
                        System.out.println("Respuesta del Servidor: " + respuesta);
                        estado = 1;
                        if (respuesta.equalsIgnoreCase("FAILED\0\0\0\0")) {
                            estado = 6;
                        }
                    } catch (IOException ex) {
                        System.out.println("Error recibiendo respuesta del servidor");
                        estado = 6;
                    }
                    socket.close();
                    break;
                case 6: //Almacenando las lecturas no enviadas o donde no se recibió respuesta del servidor
                    System.out.println("Agendando lectura no enviada o sin respuesta del servidor");
                    no_enviadas.add(lectura);
                    guardar_no_enviadas();
                    estado = 1;
                    break;
                default:
                    break;
            }
        }
    }

}
