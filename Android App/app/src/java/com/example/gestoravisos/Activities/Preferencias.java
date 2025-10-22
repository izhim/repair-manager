package com.example.gestoravisos.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.gestoravisos.Clases.PollingWorker;
import com.example.gestoravisos.R;

import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class Preferencias extends AppCompatActivity {
    private static boolean hasRecreated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        setContentView(R.layout.settings_activity);

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.menu_preferencias_titulo);
        }
        if (savedInstanceState == null) {
            findViewById(android.R.id.content).post(() -> {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.settings, new SettingsFragment())
                        .commit();
            });
        }
        if (!hasRecreated) {
            hasRecreated = true;
            findViewById(android.R.id.content).post(this::recreate);
        }
        // Registra el callback para back físico (o gesto)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                hasRecreated = false;

                finish();
            }
        });
    }




    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            hasRecreated = false;
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {
        private SharedPreferences sharedPreferences;
        private AlertDialog currentDialog;
        private boolean shouldShowDialog = false;
        private static final String DIALOG_STATE = "dialog_visible";

        private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferencias, rootKey);
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

            if (savedInstanceState != null && savedInstanceState.getBoolean(DIALOG_STATE)) {
                shouldShowDialog = true;
            }



            preferenceChangeListener = (prefs, key) -> {
                if ("sync_interval".equals(key)) {
                    String intervaloStr = prefs.getString("sync_interval", "900000");
                    long intervalo;

                    try {
                        intervalo = Long.parseLong(intervaloStr);
                    } catch (NumberFormatException e) {
                        intervalo = 900000; // fallback
                    }

                    WorkManager workManager = WorkManager.getInstance(requireContext());
                    workManager.cancelUniqueWork("PollingWorker");

                    if (intervalo > 0) {
                        // 🔥 Añadir initialDelay para evitar ejecución inmediata
                        PeriodicWorkRequest syncWork =
                                new PeriodicWorkRequest.Builder(PollingWorker.class, intervalo, TimeUnit.MILLISECONDS)
                                        .setInitialDelay(intervalo, TimeUnit.MILLISECONDS) // Esperar el primer intervalo
                                        .addTag("sync_worker")
                                        .build();

                        workManager.enqueueUniquePeriodicWork(
                                "PollingWorker",
                                ExistingPeriodicWorkPolicy.REPLACE,
                                syncWork
                        );
                    } else {
                        Log.i("Preferencias", "Sincronización desactivada");
                    }
                }
            };
            setupVisualPreferences();
            sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        }




        private void setupVisualPreferences() {
            SwitchPreference tema = findPreference("tema");
            if (tema != null) {
                tema.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean isDarkMode = (Boolean) newValue;
                    AppCompatDelegate.setDefaultNightMode(
                            isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                    );
                    return true;
                });
            }

            ListPreference fuente = findPreference("fuente");
            if (fuente != null) {
                fuente.setOnPreferenceChangeListener((preference, newValue) -> {
                    cambiarEscala(requireContext(), newValue.toString());
                    requireActivity().recreate();
                    return true;
                });
            }

            ListPreference idiomaPref = findPreference("idioma");
            if (idiomaPref != null) {
                idiomaPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    cambiarIdioma(requireContext(), newValue.toString());
                    requireActivity().recreate();
                    return true;
                });
            }
        }




        @Override
        public void onResume() {
            super.onResume();
            if (preferenceChangeListener != null)
                sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

            if (shouldShowDialog) {
                shouldShowDialog = false;
                // Mostrar diálogo si fuera necesario
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            if (preferenceChangeListener != null)
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean(DIALOG_STATE, currentDialog != null && currentDialog.isShowing());
        }

        @Override
        public void onDestroyView() {
            if (currentDialog != null && currentDialog.isShowing()) {
                currentDialog.dismiss();
            }
            super.onDestroyView();
        }
    }

    // método para cambiar la escala de la fuente
    private static void cambiarEscala(Context contexto, String valorEscala) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(contexto);
        prefs.edit().putString("fuente", valorEscala).apply();
    }

    public void btnRestablecer(View view) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(
                this,
                R.string.preferencias_restablecidas,
                Toast.LENGTH_SHORT).show();

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        recreate();
    }

    public static void cambiarIdioma(Context context, String codigoIdioma) {
        Locale nuevaLocale = new Locale(codigoIdioma);
        Locale.setDefault(nuevaLocale);
        Configuration config = new Configuration();
        config.setLocale(nuevaLocale);

        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        String valorEscala = prefs.getString("fuente", "2"); // "2" = normal
        int valor = Integer.parseInt(valorEscala) - 2;
        float escala = valor > 0 ? 1.4f : (valor == 0 ? 1.2f : 1.0f);

        Configuration config = newBase.getResources().getConfiguration();
        config = new Configuration(config); // Clona config
        config.fontScale = escala;

        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }
}