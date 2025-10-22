package com.example.gestoravisos.Clases;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;

/*
    CLASE PARA LA CONVERSIÓN DE TABLAS MÚLTIPLES PARA ALMACENARLAS EN SQLITE
    - AYUDANTES
    - IMÁGENES
 */
public class MapConverter {

    @TypeConverter
    public static Map<Integer, String> fromString(String value) {
        Type mapType = new TypeToken<Map<Integer, String>>() {}.getType();
        return new Gson().fromJson(value, mapType);
    }

    @TypeConverter
    public static String fromMap(Map<Integer, String> map) {
        return new Gson().toJson(map);
    }
}