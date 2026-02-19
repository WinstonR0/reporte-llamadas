package com.example.reportellamadas;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CallLog;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // se programa el worker para que funcione en 10s
        programarPruebaWorker();


        Button btnGenerar = findViewById(R.id.btnGenerar);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALL_LOG},
                    REQUEST_CODE);
        }

        btnGenerar.setOnClickListener(v -> {

            List<String[]> datos = obtenerLlamadasDelDia();

            if (datos.isEmpty()) {
                Toast.makeText(this,
                        "No hay llamadas entrantes hoy",
                        Toast.LENGTH_SHORT).show();
            } else {
                exportarCSV(datos);
            }
        });
    }

    // 🔹 Obtener llamadas entrantes del día
    private List<String[]> obtenerLlamadasDelDia() {

        List<String[]> lista = new ArrayList<>();

        long inicioDia = obtenerInicioDelDia();

        Cursor cursor = getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null,
                CallLog.Calls.DATE + " >= ?",
                new String[]{String.valueOf(inicioDia)},
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
                        indexDuracion == -1) {
                    continue;
                }

                int tipo = cursor.getInt(indexTipo);

                if (tipo == CallLog.Calls.INCOMING_TYPE) {

                    String numero = cursor.getString(indexNumero);
                    long fechaMillis = cursor.getLong(indexFecha);
                    int duracion = cursor.getInt(indexDuracion);

                    Date date = new Date(fechaMillis);

                    SimpleDateFormat formatoFecha =
                            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                    SimpleDateFormat formatoHora =
                            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                    String fecha = formatoFecha.format(date);
                    String hora = formatoHora.format(date);

                    // 🔹 Lógica simple para Spam
                    // Puedes cambiar esto luego si quieres algo más avanzado
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

    // 🔹 Obtener inicio del día
    private long obtenerInicioDelDia() {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    // 🔹 Exportar a CSV en Descargas
    private void exportarCSV(List<String[]> datos) {

        try {

            File file = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS),
                    "Reporte_Llamadas.csv");

            FileOutputStream fos = new FileOutputStream(file);

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

            Toast.makeText(this,
                    "Reporte generado en Descargas",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Error al generar reporte",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * Este método programa una ejecución única.
     *
     * Usamos OneTimeWorkRequest porque solo queremos
     * que se ejecute UNA vez.
     */
    private void programarPruebaWorker() {

        /*
         * Creamos la solicitud del trabajo.
         *
         * setInitialDelay → indica cuánto tiempo debe esperar
         * antes de ejecutarse.
         *
         * En este caso: 10 segundos.
         */
        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(ReporteWorker.class)
                        .setInitialDelay(10, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

        /*
         * Enviamos la solicitud al sistema.
         * WorkManager se encargará de ejecutarla.
         */
        WorkManager.getInstance(this).enqueue(workRequest);
    }

}
