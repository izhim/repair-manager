package com.example.gestoravisos.Clases;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.preference.PreferenceManager;

public class FontScaleContextWrapper extends ContextWrapper {
    public FontScaleContextWrapper(Context base) {
        super(base);
    }

    public static ContextWrapper wrap(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String valorEscala = prefs.getString("fuente", "2"); // valor por defecto "2" (equivale a escala 1.0)

        int valor = Integer.parseInt(valorEscala) - 2;
        float escala = valor > 0 ? 1.3f : (valor == 0 ? 1.0f : 0.9f);

        Configuration config = context.getResources().getConfiguration();
        config = new Configuration(config); // clona para evitar afectar el original
        config.fontScale = escala;

        return new FontScaleContextWrapper(context.createConfigurationContext(config));
    }
}
