package com.example.reportellamadas;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
 * Worker que crea un archivo de prueba en la carpeta Descargas.
 *
 * Esto nos permitirá comprobar que:
 * - WorkManager funciona
 * - La app puede escribir archivos
 * - Se ejecuta aunque la app esté cerrada
 */
public class ReporteWorker extends Worker {

    public ReporteWorker(@NonNull Context context,
                         @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /*
     * Método que se ejecuta en segundo plano
     */
    @NonNull
    @Override
    public Result doWork() {

        try {

            // Llamamos la lógica real del reporte
            ReporteUtils.generarReporte(getApplicationContext());

            return Result.success();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }

}
