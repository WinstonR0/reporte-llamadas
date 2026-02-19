package com.example.reportellamadas;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.provider.CallLog;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
 * Clase encargada de:
 * Obtener llamadas entrantes del día
 * Generar el archivo CSV
 */
public class ReporteUtils {

    /*
     * Método principal que será llamado desde el Worker
     */
    public static void generarReporte(Context context) {

        List<String[]> llamadas = obtenerLlamadasDelDia(context);

        if (llamadas.isEmpty()) {
            return; // No generamos archivo si no hay llamadas
        }

        exportarCSV(llamadas);
    }

    /*
     * Obtiene llamadas entrantes desde las 00:00 del día actual
     * y las ordena de la primera a la última
     */
    private static List<String[]> obtenerLlamadasDelDia(Context context) {

        List<String[]> lista = new ArrayList<>();

        // Marcamos las 00:00 del día actual
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long inicioDelDia = calendar.getTimeInMillis();

        /*
         * Consulta al registro de llamadas
         * Filtramos desde inicio del día
         * Orden ASC (primera → última)
         */
        Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null,
                CallLog.Calls.DATE + " >= ?",
                new String[]{String.valueOf(inicioDelDia)},
                CallLog.Calls.DATE + " ASC"
        );

        if (cursor != null) {

            int indexTipo = cursor.getColumnIndex(CallLog.Calls.TYPE);
            int indexNumero = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int indexFecha = cursor.getColumnIndex(CallLog.Calls.DATE);
            int indexDuracion = cursor.getColumnIndex(CallLog.Calls.DURATION);

            while (cursor.moveToNext()) {

                if (indexTipo == -1 ||
                        indexNumero == -1 ||
                        indexFecha == -1 ||
                        indexDuracion == -1) continue;

                int tipo = cursor.getInt(indexTipo);

                // Solo llamadas entrantes
                if (tipo == CallLog.Calls.INCOMING_TYPE) {

                    String numero = cursor.getString(indexNumero);
                    long fechaMillis = cursor.getLong(indexFecha);
                    int duracion = cursor.getInt(indexDuracion);

                    Date fechaDate = new Date(fechaMillis);

                    String fecha = new SimpleDateFormat(
                            "dd-MM-yyyy",
                            Locale.getDefault()
                    ).format(fechaDate);

                    String hora = new SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.getDefault()
                    ).format(fechaDate);

                    String spam = (duracion == 0) ? "Si" : "No";

                    lista.add(new String[]{
                            numero,
                            spam,
                            fecha,
                            hora
                    });
                }
            }

            cursor.close();
        }

        return lista;
    }

    /*
     * Genera el archivo CSV con nombre dinámico según la fecha actual
     */
    private static void exportarCSV(List<String[]> datos) {

        try {

            String fechaHoy = new SimpleDateFormat(
                    "dd-MM-yyyy",
                    Locale.getDefault()
            ).format(new Date());

            File archivo = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS),
                    "Reporte_" + fechaHoy + ".csv"
            );

            FileOutputStream fos = new FileOutputStream(archivo);

            // Encabezado del CSV
            String encabezado = "Numero,Spam,Fecha,Hora\n";
            fos.write(encabezado.getBytes());

            for (String[] fila : datos) {

                String linea = fila[0] + "," +
                        fila[1] + "," +
                        fila[2] + "," +
                        fila[3] + "\n";

                fos.write(linea.getBytes());
            }

            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
