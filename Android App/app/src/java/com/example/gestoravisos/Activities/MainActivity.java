package com.example.gestoravisos.Activities;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.gestoravisos.Clases.PollingWorker;
import com.example.gestoravisos.R;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
/*
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags("es");
        AppCompatDelegate.setApplicationLocales(appLocale);
*/

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String langCode = prefs.getString("idioma", "es"); // por defecto español
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(langCode);
        AppCompatDelegate.setApplicationLocales(appLocale);

        Locale locale = AppCompatDelegate.getApplicationLocales().get(0);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            //NavigationUI.setupActionBarWithNavController(this, navController);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        // Maneja la acción de navegación
        if ("OPEN_SYNC_FRAGMENT".equals(getIntent().getAction())) {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
            navController.navigate(R.id.listaTrabajosFragment);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    public void showBackButton(boolean show) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(show);

        }
    }

    @Override
    protected void attachBaseContext(Context contexto) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(contexto);

        // Idioma
        String codigoIdioma = prefs.getString("idioma", "es");
        Locale nuevaLocale = new Locale(codigoIdioma);
        Locale.setDefault(nuevaLocale);

        // Escala de fuente
        String valorEscala = prefs.getString("fuente", "1"); // El valor por defecto debe coincidir con el de tus preferencias
        int valor = Integer.parseInt(valorEscala) - 2;
        float escala = valor > 0 ? 1.4f : (valor == 0 ? 1.2f : 1.0f);

        Configuration config = new Configuration();
        config.setLocale(nuevaLocale);
        config.fontScale = escala;

        Context context = contexto.createConfigurationContext(config);
        super.attachBaseContext(context);
    }
/*
    private void configurarPolling() {
        // Definir las restricciones (opcional)
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();


        // Crear la solicitud de trabajo periódico
        PeriodicWorkRequest pollingRequest =
                new PeriodicWorkRequest.Builder(PollingWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        // Programar el trabajo
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "polling_work",
                ExistingPeriodicWorkPolicy.KEEP, // Mantener el trabajo existente si ya está programado
                pollingRequest
        );
    }

 */


}