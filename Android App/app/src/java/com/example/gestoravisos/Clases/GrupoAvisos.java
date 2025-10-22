package com.example.gestoravisos.Clases;

import com.example.gestoravisos.basedatos.AvisoEntity;

import java.util.List;

public class GrupoAvisos {
    private String fecha;
    private List<AvisoEntity> avisos;

    public GrupoAvisos(String fecha, List<AvisoEntity> avisos) {
        this.fecha = fecha;
        this.avisos = avisos;
    }

    // Getters
    public String getFecha() { return fecha; }
    public List<AvisoEntity> getAvisos() { return avisos; }
}
