package com.example.gestoravisos.basedatos;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {AvisoEntity.class}, version = 1, exportSchema = false)
public abstract class BaseDatos extends RoomDatabase {

    private static BaseDatos INSTANCIA;

    public static BaseDatos getInstance(Context contexto){
        if(INSTANCIA  == null){
            INSTANCIA = Room.databaseBuilder(
                    contexto.getApplicationContext(),
                    BaseDatos.class,
                    "dbAvisos").build();
        }
        return INSTANCIA;
    }

    public static void destroyInstance(){
        INSTANCIA = null;
    }

    public abstract AvisoDAO avisoDAO();

}
