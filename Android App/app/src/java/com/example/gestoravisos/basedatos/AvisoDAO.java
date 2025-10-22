package com.example.gestoravisos.basedatos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AvisoDAO {
    // Consulta de todos los avisos
    @Query("SELECT * FROM avisoentity")
    // Consulta de todos los avisos cuando cambie el LiveData
    LiveData<List<AvisoEntity>> getAvisos();

    // Obtener una lista estática de avisos
    @Query("SELECT * FROM avisoentity")
    List<AvisoEntity> getStaticAvisos();
    @Query("SELECT COUNT(*) FROM avisoentity")
    int contarAvisos();
    @Insert
    void insertar(List<AvisoEntity> avisos);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarAvisos(List<AvisoEntity> avisos);

    default void setAvisos(List<AvisoEntity> avisosIniciales) {
        int count = contarAvisos();
        if (count == 0) {
            insertarAvisos(avisosIniciales);
        }
    }
    @Insert//(onConflict = OnConflictStrategy.IGNORE)
    void insertarAviso(AvisoEntity aviso);

    // modificar una tarea
    @Update
    void modificarAviso(AvisoEntity aviso);

    // eliminar una tarea
    @Delete
    void eliminarAviso(AvisoEntity aviso);

    @Query("DELETE FROM avisoentity")
    void eliminarDatos();
    @Query("DELETE FROM avisoentity")
    void eliminarTodosAvisos();
    @Query("DELETE FROM avisoentity WHERE id_aviso IN (:ids)")
    void eliminarAvisos(List<Integer> ids);

    @Query("SELECT * FROM avisoentity WHERE id_aviso = :id")
    AvisoEntity getAvisoById(int id);

    @Query("SELECT id_aviso FROM avisoentity")
    List<Integer> obtenerIdsAvisos();

    @Query("SELECT * FROM avisoentity WHERE sincronizado = 0 OR " +
            "(firma IS NOT NULL AND sincronizado = 0) OR " +
            "(imagenesInst IS NOT NULL AND json_array_length(imagenesInst) > 0 AND sincronizado = 0)")
    List<AvisoEntity> getAvisosModificados();

    @Query("SELECT * FROM avisoentity WHERE sincronizado = 1")
    List<AvisoEntity> getAvisosSincronizados();
}
