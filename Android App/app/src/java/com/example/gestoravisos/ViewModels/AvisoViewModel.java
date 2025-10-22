package com.example.gestoravisos.ViewModels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.example.gestoravisos.Clases.Event;
import com.example.gestoravisos.Login.Data.Model.LoggedInUser;
import com.example.gestoravisos.Login.Data.Repository.LoginRepository;
import com.example.gestoravisos.Login.Data.Source.LoginDataSource;
import com.example.gestoravisos.Login.Network.ApiCallback;
import com.example.gestoravisos.basedatos.AvisoEntity;
import com.example.gestoravisos.basedatos.RepositorioSQLite;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AvisoViewModel extends AndroidViewModel {

    private RepositorioSQLite repositorio;
    private LoginRepository loginRepository;
    private LiveData<List<AvisoEntity>> listaAvisosLiveData;
    private MutableLiveData<List<AvisoEntity>> listaFiltrada = new MutableLiveData<>();
    private MutableLiveData<Boolean> soloUrgentes = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> pendientes = new MutableLiveData<>(true);
    private MutableLiveData<Boolean> parciales = new MutableLiveData<>(true);
    private MutableLiveData<Boolean> terminados = new MutableLiveData<>(true);

    private MutableLiveData<Pair<Long, Long>> rangoFechas = new MutableLiveData<>();
    private MutableLiveData<Boolean> firmados = new MutableLiveData<>(true);
    private final MutableLiveData<Event<Boolean>> conectado = new MutableLiveData<>();
    public LiveData<Event<Boolean>> ldConectado = conectado;

    public AvisoViewModel(@NonNull Application application) {
        super(application);
        repositorio = RepositorioSQLite.getInstance(application);
        loginRepository = LoginRepository.getInstance(new LoginDataSource(application), application);
        listaAvisosLiveData = repositorio.getListaAvisosLiveData();
        cargarFiltrosIniciales(application);
    }

    public LiveData<List<AvisoEntity>> getListaAvisosLiveData() {
        return listaAvisosLiveData;
    }

    // Método para sincronizar los avisos
    public void sincronizarAvisos(Context contexto) {

        SharedPreferences sharedPreferences;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(contexto);
        LoginDataSource login = new LoginDataSource(contexto);
        login.login(
                sharedPreferences.getString("usuario", null),
                sharedPreferences.getString("password", null),
                new ApiCallback<LoggedInUser>() {
                    @Override
                    public void onSuccess(LoggedInUser result) {
                        if (result.getAuthToken() == null) {
                            conectado.postValue(new Event<>(false));
                            return;
                        }

                        repositorio.sincronizarAvisos(result.getAuthToken());
                        actualizarListaFiltrada();
                        conectado.postValue(new Event<>(true));
                    }

                    @Override
                    public void onError(String error) {
                        conectado.postValue(new Event<>(false));
                        Log.d("SINCRONIZACION", "Sincronización fallida");
                    }
                });
    }
    public List<AvisoEntity> getAvisosPorFecha(String fecha) {
        List<AvisoEntity> todosAvisos = listaAvisosLiveData.getValue();
        if (todosAvisos == null) return Collections.emptyList();

        return todosAvisos.stream()
                .filter(aviso -> aviso.getFecha_aviso().equals(fecha))
                .collect(Collectors.toList());
    }

    public void eliminarDatos(){
        repositorio.eliminarDatos();
    }

    public void actualizarLista(){
        repositorio.actualizarLista();
    }

    // Método para convertir String fecha a timestamp
    private long convertirFechaATimestamp(String fecha) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = sdf.parse(fecha);
            return date != null ? date.getTime() : 0;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void actualizarListaFiltrada() {
        List<AvisoEntity> listaOriginal = listaAvisosLiveData.getValue();
        if (listaOriginal == null) return;

        List<AvisoEntity> filtrados = listaOriginal.stream()
                .filter(aviso -> {
                    Boolean urgente = soloUrgentes.getValue();
                    return urgente == null || !urgente || aviso.isUrgente();
                })

                .filter(aviso -> {
                    String estado = aviso.getEstado();
                    boolean cumpleFiltro = false;

                    if (pendientes.getValue()) cumpleFiltro = cumpleFiltro || "PENDIENTE".equalsIgnoreCase(estado);
                    if (parciales.getValue()) cumpleFiltro = cumpleFiltro || "PARCIAL".equalsIgnoreCase(estado);
                    if (terminados.getValue()) cumpleFiltro = cumpleFiltro || "TERMINADO".equalsIgnoreCase(estado);

                    // Si no hay ningún filtro de estado seleccionado, mostrar todos
                    if (!pendientes.getValue() && !parciales.getValue() && !terminados.getValue()) {
                        return true;
                    }

                    return cumpleFiltro;
                })

                .filter(aviso -> {
                    if (firmados.getValue()) {
                        return true; // Mostrar todos si firmados = true
                    } else {
                        // Mostrar solo avisos sin firma si firmados = false
                        return aviso.getFirma() == null || aviso.getFirma().length == 0;
                    }
                })

                .filter(aviso -> {
                    Pair<Long, Long> fechas = rangoFechas.getValue();
                    if (fechas == null) return true;

                    long fechaAviso = convertirFechaATimestamp(aviso.getFecha_ejecucion());
                    return fechaAviso >= fechas.first && fechaAviso <= fechas.second;
                })


                .collect(Collectors.toList());

        //listaFiltrada.postValue(filtrados);
        listaFiltrada.postValue(new ArrayList<>(filtrados));
    }

    public void limpiarFiltros() {
        firmados.setValue(true);
        soloUrgentes.setValue(false);
        pendientes.setValue(true);
        parciales.setValue(true);
        terminados.setValue(true);
        rangoFechas.setValue(null);
        listaFiltrada.postValue(listaAvisosLiveData.getValue());
    }

    public LiveData<List<AvisoEntity>> getListaFiltrada() {
        return listaFiltrada;
    }

    public void aplicarFiltrosDesdePreferencias(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean urgentes = prefs.getBoolean("urgentes", false);

        boolean estPendientes = prefs.getBoolean("pendientes", true);
        boolean estParciales = prefs.getBoolean("parciales", true);
        boolean estTerminados = prefs.getBoolean("terminados", true);
        boolean estFirmados = prefs.getBoolean("firmados", true);

        long fechaInicio = prefs.getLong("fecha_inicio", -1);
        long fechaFin = prefs.getLong("fecha_fin", -1);
        Pair<Long, Long> rango = null;
        if (fechaInicio != -1 && fechaFin != -1) {
            rango = new Pair<>(fechaInicio, fechaFin);
        }

        soloUrgentes.setValue(urgentes);
        pendientes.setValue(estPendientes);
        parciales.setValue(estParciales);
        terminados.setValue(estTerminados);
        firmados.setValue(estFirmados);
        rangoFechas.setValue(rango);
        actualizarListaFiltrada();
    }

    private void cargarFiltrosIniciales(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        soloUrgentes.setValue(prefs.getBoolean("urgentes", false));
        firmados.setValue(prefs.getBoolean("firmados", true));
        pendientes.setValue(prefs.getBoolean("pendientes", true));
        parciales.setValue(prefs.getBoolean("parciales", true));
        terminados.setValue(prefs.getBoolean("terminados", true));

        long fechaInicio = prefs.getLong("fecha_inicio", -1);
        long fechaFin = prefs.getLong("fecha_fin", -1);
        if (fechaInicio != -1 && fechaFin != -1) {
            rangoFechas.setValue(new Pair<>(fechaInicio, fechaFin));
        }

        // Aplicar filtros iniciales
        actualizarListaFiltrada();
    }

    public void guardarFiltrosEnPreferencias(Context context) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        editor.putBoolean("urgentes", soloUrgentes.getValue() != null ? soloUrgentes.getValue() : false);
        editor.putBoolean("firmados", firmados.getValue() != null ? firmados.getValue() : true);
        editor.putBoolean("pendientes", pendientes.getValue() != null ? pendientes.getValue() : true);
        editor.putBoolean("parciales", parciales.getValue() != null ? parciales.getValue() : true);
        editor.putBoolean("terminados", terminados.getValue() != null ? terminados.getValue() : true);

        Pair<Long, Long> fechas = rangoFechas.getValue();
        if (fechas != null) {
            editor.putLong("fecha_inicio", fechas.first);
            editor.putLong("fecha_fin", fechas.second);
        } else {
            editor.remove("fecha_inicio");
            editor.remove("fecha_fin");
        }

        editor.apply();
    }

    public void aplicarFiltros(
            boolean urgentes,
            boolean firmados,
            boolean pendientes,
            boolean parciales,
            boolean terminados,
            Pair<Long, Long> fechasSeleccionadas
    ) {
        this.soloUrgentes.setValue(urgentes);
        this.firmados.setValue(firmados);
        this.pendientes.setValue(pendientes);
        this.parciales.setValue(parciales);
        this.terminados.setValue(terminados);
        this.rangoFechas.setValue(fechasSeleccionadas);

        actualizarListaFiltrada();
    }

    public void limpiarFiltros(Context context) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.clear();
        editor.apply();

        soloUrgentes.setValue(false);
        firmados.setValue(true);
        pendientes.setValue(true);
        parciales.setValue(true);
        terminados.setValue(true);
        rangoFechas.setValue(null);

        actualizarListaFiltrada();
    }
}