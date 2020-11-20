package sensores;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lectura implements Serializable{

    private static final String RUTA_JSON = "json/";
    private static final Random RAND = new Random();
    private String sensor;
    private Timestamp timestamp;
    private float medida;

    public Lectura(String sensor, Timestamp timestamp, float medida) {
        this.sensor = sensor;
        this.timestamp = timestamp;
        this.medida = medida;
    }

    public Lectura(String sensor) {
        this(sensor, new Timestamp(System.currentTimeMillis()), RAND.nextFloat() * 10);
    }

    public Lectura(String sensor, String timestamp, String medida) {
        this(sensor, Timestamp.valueOf(timestamp), Float.parseFloat(medida));
    }

    private Lectura(String[] data) {
        this(data[1], data[2], data[3]);
    }

    public Lectura(byte[] bytes) {
        this(new String(bytes).split(" & "));
    }

    @Override
    public String toString() {
        return " & " + sensor + " & " + timestamp + " & " + medida + " & ";
    }

    public byte[] getBytes() {
        return toString().getBytes();
    }

    public String getSensor() {
        return sensor;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public float getMedida() {
        return medida;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.sensor);
        hash = 59 * hash + Objects.hashCode(this.timestamp);
        hash = 59 * hash + Float.floatToIntBits(this.medida);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Lectura other = (Lectura) obj;
        if (Float.floatToIntBits(this.medida) != Float.floatToIntBits(other.medida)) {
            return false;
        }
        if (!Objects.equals(this.sensor, other.sensor)) {
            return false;
        }
        if (!Objects.equals(this.timestamp, other.timestamp)) {
            return false;
        }
        return true;
    }

    public static HashMap<String, ArrayList<Lectura>> cargar_datos() {
        HashMap<String, ArrayList<Lectura>> lecturas = new HashMap<>();
        String[] archivos;
        if (Files.exists(Paths.get(RUTA_JSON)) && (archivos = new File(RUTA_JSON).list()).length > 0) {
            for (String archivo : archivos) {
                if (archivo.toLowerCase().endsWith(".json")) {
                    try (BufferedReader br = new BufferedReader(new FileReader(RUTA_JSON + archivo))) {
                        String txt = "";
                        String linea;
                        while ((linea = br.readLine()) != null) {
                            txt = txt + linea + " ";
                        }
                        Matcher matcher_sensor = Pattern.compile("\"sensor\"[:\\s]+\"(.*?)\"").matcher(txt);
                        matcher_sensor.find();
                        String sensor = matcher_sensor.group(1);
                        Matcher matcher_timestamp = Pattern.compile("\"timestamp\"[:\\s]+\"(.*?)\"").matcher(txt);
                        Matcher matcher_medida = Pattern.compile("\"medida\"[:\\s]+([.\\d]+)").matcher(txt);
                        while (matcher_timestamp.find() & matcher_medida.find()) {
                            if (!lecturas.containsKey(sensor)) {
                                lecturas.put(sensor, new ArrayList<>());
                            }
                            lecturas.get(sensor).add(new Lectura(sensor, matcher_timestamp.group(1), matcher_medida.group(1)));
                        }

                    } catch (FileNotFoundException ex) {
                        System.out.println("El archivo no existe o ha sido eliminado manualmente");
                    } catch (IOException ex) {
                        System.out.println("Error al abrir el archivo");
                    }
                }
            }
        }

        return lecturas;
    }

    public static void guardar_datos(HashMap<String, ArrayList<Lectura>> lecturas) {
        if (Files.notExists(Paths.get(RUTA_JSON))) {
            try {
                Files.createDirectories(Paths.get(RUTA_JSON));
            } catch (IOException ex) {
                System.out.println("No se pudo crear carpeta '" + RUTA_JSON + "' para guardar los datos");
                System.out.println("Crearla manualmente con el administrador de archivos de su preferencia");
            }
        }
        for (String sensor : lecturas.keySet()) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(RUTA_JSON + sensor + ".json"))) {
                bw.write("{\n");
                bw.write("\t\"sensor\": \"" + sensor + "\",\n");
                bw.write("\t\"lecturas\": [\n");
                for (Lectura lectura : lecturas.get(sensor)) {
                    bw.write("\t\t{\n");
                    bw.write("\t\t\t\"timestamp\": \"" + lectura.getTimestamp() + "\",\n");
                    bw.write("\t\t\t\"medida\": " + lectura.getMedida() + "\n");
                    bw.write("\t\t},\n");
                }
                bw.write("\t]\n");
                bw.write("}\n");
            } catch (IOException ex) {
                System.out.println("Error al generar Archivo JSON");
            }
        }
    }

}
