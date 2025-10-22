package com.example.gestoravisos.Clases;

import com.example.gestoravisos.basedatos.AvisoEntity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AgrupadorAvisos {
    public static List<GrupoAvisos> groupByFecha(List<AvisoEntity> avisos, boolean ordenAscendente) {
        Map<String, List<AvisoEntity>> groupedMap = new HashMap<>();

        for (AvisoEntity aviso : avisos) {
            String fecha = aviso.getFecha_ejecucion();
            if (!groupedMap.containsKey(fecha)) {
                groupedMap.put(fecha, new ArrayList<>());
            }
            groupedMap.get(fecha).add(aviso);
        }

        List<GrupoAvisos> groupedList = new ArrayList<>();
        for (Map.Entry<String, List<AvisoEntity>> entry : groupedMap.entrySet()) {
            groupedList.add(new GrupoAvisos(entry.getKey(), entry.getValue()));
        }

        // Ordenar según la preferencia
        Collections.sort(groupedList, (grupo1, grupo2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date fecha1 = sdf.parse(grupo1.getFecha());
                Date fecha2 = sdf.parse(grupo2.getFecha());

                return ordenAscendente ?
                        fecha1.compareTo(fecha2) : // Ascendente (antiguo -> nuevo)
                        fecha2.compareTo(fecha1);  // Descendente (nuevo -> antiguo)
            } catch (ParseException e) {
                return 0;
            }
        });

        return groupedList;
    }
}
