package com.example.gestoravisos.Clases;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.gestoravisos.Activities.MainActivity;
import com.example.gestoravisos.Login.Data.Repository.SecurePreferences;
import com.example.gestoravisos.Login.Network.ApiClient;
import com.example.gestoravisos.R;
import com.example.gestoravisos.basedatos.AvisoEntity;
import com.example.gestoravisos.basedatos.RepositorioSQLite;

import java.util.List;

import retrofit2.Response;

public class PollingWorker extends Worker {
    private static final String TAG = "PollingWorker";
    private final RepositorioSQLite repositorio;
    private final Context context;

    public PollingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.repositorio = RepositorioSQLite.getInstance(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            SecurePreferences securePreferences = SecurePreferences.getInstance(context);
            String token = securePreferences.getToken();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            if (token == null) {
                Log.e("AUTH", "Token is null");
                return Result.failure();
            }

            long lastSyncTime = sharedPreferences.getLong("last_sync_time", 0);
            Log.d("LAST_SYNC", "Last sync time: " + lastSyncTime);

            Response<List<AvisoEntity>> response = ApiClient.getInstance(context)
                    .getApiService()
                    .checkForUpdates("Bearer " + token.trim(), lastSyncTime)
                    .execute();

            if (!response.isSuccessful()) {
                Log.e("API_ERROR", "Code: " + response.code() + " - " + response.message());
                if (response.errorBody() != null) {
                    Log.e("API_ERROR_BODY", response.errorBody().string());
                }
                return Result.failure();
            }

            // Procesar respuesta exitosa
            List<AvisoEntity> nuevosAvisos = response.body();
            if (nuevosAvisos != null && !nuevosAvisos.isEmpty()) {
                int urgentes = 0;
                for (AvisoEntity aviso : nuevosAvisos) {
                    if (aviso.isUrgente()) {
                        urgentes++;
                    }
                }

                // Mostrar notificación
                mostrarNotificacion(nuevosAvisos.size(), urgentes);

                // Actualizar última hora de sincronización

            }

            return Result.success();
        } catch (Exception e) {
            Log.e("WORKER_ERROR", "Full error: ", e);
            return Result.failure();
        }
    }

    private void mostrarNotificacion(int cantidadNuevos, int cantidadUrgentes) {
        String channelId = "updates_channel";
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Crear canal (necesario para Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Avisos urgentes",  // ¡Nombre importante para que el usuario lo identifique!
                    NotificationManager.IMPORTANCE_HIGH  // Debe ser HIGH o MAX
            );

            // Configuración CRÍTICA para heads-up:
            channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 250, 500});
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            notificationManager.createNotificationChannel(channel);
        }

        // Construir el mensaje según la cantidad de urgentes
        String contentText;
        if (cantidadUrgentes > 0) {
            contentText =
                    cantidadUrgentes +
                    getApplicationContext().getString(R.string.avisos_urgentes) +
                    cantidadNuevos +
                    getApplicationContext().getString(R.string.nuevos);
        } else {
            contentText = cantidadNuevos + getApplicationContext().getString(R.string.nuevos_avisos);
        }

        // Construir la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(getApplicationContext().getString(R.string.n_nuevos_avisos))
                .setContentText(contentText)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX) // Prioridad MAXIMA para Android 7.1-
                .setDefaults(Notification.DEFAULT_ALL) // Sonido, vibración y luces
                .setAutoCancel(true)
                .setContentIntent(createPendingIntent())
                .setFullScreenIntent(createPendingIntent(), true);

        // Añadir estilo de notificación expandida
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
                .bigText(contentText + "\n\n" + getApplicationContext().getString(R.string.abrir_aplicacion));

        builder.setStyle(bigTextStyle);

        // Cambiar color y vibración si hay avisos urgentes
        if (cantidadUrgentes > 0) {
            builder.setColor(ContextCompat.getColor(context, R.color.colorAccent))
                    .setLights(Color.RED, 1000, 1000)
                    .setVibrate(new long[]{0, 500, 250, 500}); // Patrón de vibración
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private PendingIntent createPendingIntent() {
        // Crea un Intent con la acción de navegación
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction("OPEN_SYNC_FRAGMENT");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}