package com.example.gestoravisos.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.core.util.Pair;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.example.gestoravisos.R;
import com.example.gestoravisos.ViewModels.AvisoViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BotonFiltrarFragment extends BottomSheetDialogFragment {
    private Pair<Long, Long> fechasSeleccionadas;
    private SharedPreferences sharedPreferences;
    private AvisoViewModel avisoViewModel;
    private boolean urgentes, firmados, pendientes, parciales, terminados, fecha, semana, rango;
    private Switch sUrgentes, sFirmados, sFecha;
    private MaterialButton mPendientes, mParciales, mTerminados, mSemanaActual, mRango;
    private View lFechas;
    private Button btnSeleccionarFecha, btnAplicar, btnLimpiar;
    private Long fechaInicio, fechaFin;

    public interface FilterListener {
        void onFiltersApplied(
                boolean urgentes,
                boolean  firmados,
                boolean  pendientes,
                boolean parciales,
                boolean terminados,
                Pair<Long, Long> fechasSeleccionadas);
        void onFiltersCleared();
    }

    private FilterListener listener;

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.boton_filtrar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        avisoViewModel = new ViewModelProvider(requireActivity()).get(AvisoViewModel.class);

        sUrgentes = view.findViewById(R.id.switchUrgentes);
        sFirmados = view.findViewById(R.id.switchFirmados);
        mPendientes = view.findViewById(R.id.tgFiltrarPendientes);
        mParciales = view.findViewById(R.id.tgFiltrarParciales);
        mTerminados = view.findViewById(R.id.tgFiltrarTerminados);
        sFecha = view.findViewById(R.id.switchFecha);
        lFechas = view.findViewById(R.id.layoutFechas);
        mSemanaActual = view.findViewById(R.id.tgSemanaActual);
        mRango = view.findViewById(R.id.tgRangoFechas);
        btnSeleccionarFecha = view.findViewById(R.id.btnSeleccionarFecha);
        btnAplicar = view.findViewById(R.id.btnAplicarFiltros);
        btnLimpiar = view.findViewById(R.id.btnLimpiarFiltros);

        // Restaurar filtros guardados
        urgentes = sharedPreferences.getBoolean("urgentes", false);
        firmados = sharedPreferences.getBoolean("firmados", true);
        pendientes = sharedPreferences.getBoolean("pendientes", true);
        parciales = sharedPreferences.getBoolean("parciales", true);
        terminados = sharedPreferences.getBoolean("terminados", true);
        fecha = sharedPreferences.getBoolean("fecha", false);
        semana = sharedPreferences.getBoolean("sem_actual", false);
        rango = sharedPreferences.getBoolean("rango", false);
        fechaInicio = sharedPreferences.getLong("fecha_inicio", -1);
        fechaFin = sharedPreferences.getLong("fecha_fin", -1);

        // Aplicar SharedPreferences a la vista
        sUrgentes.setChecked(urgentes);
        sFirmados.setChecked(firmados);
        mPendientes.setChecked(pendientes);
        mParciales.setChecked(parciales);
        mTerminados.setChecked(terminados);

        sFecha.setChecked(fecha);


        mSemanaActual.setEnabled(fecha);
        mSemanaActual.setChecked(semana);
        mSemanaActual.setActivated(semana); // fuerza el estado visual correcto

        mRango.setEnabled(fecha);
        mRango.setChecked(rango);
        mRango.setActivated(rango);

        btnSeleccionarFecha.setEnabled(fecha && rango);

        if (semana && fecha) {
            fechasSeleccionadas = obtenerSemanaActual();
            fechaInicio = fechasSeleccionadas.first;
            fechaFin = fechasSeleccionadas.second;
            btnSeleccionarFecha.setText(formatDateRange(fechaInicio, fechaFin));
        } else if (fechaInicio != -1 && fechaFin != -1 && fecha && rango) {
            fechasSeleccionadas = new Pair<>(fechaInicio, fechaFin);
            btnSeleccionarFecha.setText(formatDateRange(fechaInicio, fechaFin));
        } else {
            btnSeleccionarFecha.setText(R.string.seleccionar_rango_fechas);
            fechasSeleccionadas = null;
        }



        sFecha.setOnClickListener(v -> {
            boolean activado = sFecha.isChecked();

            mSemanaActual.setChecked(false);
            mSemanaActual.setEnabled(activado);
            mSemanaActual.setActivated(false); // Fuerza el cambio visual

            mRango.setChecked(false);
            mRango.setEnabled(activado);
            mRango.setActivated(false);

            lFechas.setEnabled(activado);
            btnSeleccionarFecha.setEnabled(false);
            btnSeleccionarFecha.setText(R.string.seleccionar_rango_fechas);
            fechaInicio = -1L;
            fechaFin = -1L;
            fechasSeleccionadas = null;
        });

        mRango.setOnClickListener(v -> {
            btnSeleccionarFecha.setEnabled(mRango.isChecked());
        });
        mSemanaActual.setOnClickListener(v -> {
            btnSeleccionarFecha.setEnabled(!mSemanaActual.isChecked());
            if(mSemanaActual.isChecked()) {
                fechasSeleccionadas = obtenerSemanaActual();
                fechaInicio = fechasSeleccionadas.first;
                fechaFin = fechasSeleccionadas.second;
                btnSeleccionarFecha.setText(formatDateRange(
                        fechasSeleccionadas.first,
                        fechasSeleccionadas.second));
            }else btnSeleccionarFecha.setText(getString(R.string.seleccionar_rango_fechas));
        });

        // Listener del botón para seleccionar fecha
        btnSeleccionarFecha.setOnClickListener(v -> showDateRangePicker(btnSeleccionarFecha));

        // Listener del botón de aplicar filtros
        btnAplicar.setOnClickListener(v -> {
            // Capturamos los valores de la vista
            urgentes = sUrgentes.isChecked();
            firmados = sFirmados.isChecked();
            pendientes = mPendientes.isChecked();
            parciales = mParciales.isChecked();
            terminados = mTerminados.isChecked();
            fecha = sFecha.isChecked();
            semana = mSemanaActual.isChecked();
            rango = mRango.isChecked();

            if(semana)
                fechasSeleccionadas = obtenerSemanaActual();

            // Actualizamos las SharedPreferences
            sharedPreferences.edit()
                    .putBoolean("urgentes", urgentes)
                    .putBoolean("firmados", firmados)
                    .putBoolean("pendientes", pendientes)
                    .putBoolean("parciales", parciales)
                    .putBoolean("terminados", terminados)
                    .putBoolean("fecha", fecha)
                    .putBoolean("sem_actual", semana)
                    .putBoolean("rango", rango)
                    .putLong("fecha_inicio", fechasSeleccionadas != null ? fechasSeleccionadas.first : -1)
                    .putLong("fecha_fin", fechasSeleccionadas != null ? fechasSeleccionadas.second : -1)
                    .apply();


            if (listener != null) {
                listener.onFiltersApplied(
                        urgentes,
                        firmados,
                        pendientes,
                        parciales,
                        terminados,
                        fechasSeleccionadas);
            }else{
                Log.e("ATENCION", "EL LISTENER ES NULL");

                avisoViewModel.aplicarFiltros(
                        urgentes,
                        firmados,
                        pendientes,
                        parciales,
                        terminados,
                        fechasSeleccionadas
                );
                avisoViewModel.actualizarListaFiltrada();
            }
            dismiss();
        });

        btnLimpiar.setOnClickListener(v -> {
            sUrgentes.setChecked(false);
            sFirmados.setChecked(true);
            mPendientes.setChecked(true);
            mParciales.setChecked(true);
            mTerminados.setChecked(true);
            sFecha.setChecked(false);
            lFechas.setEnabled(false);
            mSemanaActual.setChecked(false);
            mSemanaActual.setEnabled(false);
            mRango.setChecked(false);
            mRango.setEnabled(false);
            btnSeleccionarFecha.setEnabled(false);
            fechasSeleccionadas = null;
            btnSeleccionarFecha.setText(R.string.seleccionar_rango_fechas);

            // Limpiar SharedPreferences
            sharedPreferences.edit()
                    .remove("urgentes")
                    .remove("firmados")
                    .remove("pendientes")
                    .remove("parciales")
                    .remove("terminados")
                    .remove("fecha")
                    .remove("sem_actual")
                    .remove("rango")
                    .remove("fecha_inicio")
                    .remove("fecha_fin")
                    .apply();
        });
    }

    private void showDateRangePicker(Button btnSeleccionarFecha) {

        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder
                .dateRangePicker()
                .setTitleText(getString(R.string.seleccionar_rango_fechas))
                .setSelection(fechasSeleccionadas)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            fechasSeleccionadas = selection;
            fechaInicio = selection.first;
            fechaFin = selection.second;
            btnSeleccionarFecha.setText(formatDateRange(selection.first, selection.second));
        });

        picker.show(getParentFragmentManager(), picker.toString());
    }

    private String formatDateRange(long startDate, long endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(startDate)) + " - " + sdf.format(new Date(endDate));
    }

    public static Pair<Long, Long> obtenerSemanaActual() {
        Calendar inicioSemana = Calendar.getInstance();
        inicioSemana.set(Calendar.DAY_OF_WEEK, inicioSemana.getFirstDayOfWeek());
        inicioSemana.set(Calendar.HOUR_OF_DAY, 0);
        inicioSemana.set(Calendar.MINUTE, 0);
        inicioSemana.set(Calendar.SECOND, 0);
        inicioSemana.set(Calendar.MILLISECOND, 0);

        Calendar finSemana = (Calendar) inicioSemana.clone();
        finSemana.add(Calendar.DAY_OF_WEEK, 6);
        finSemana.set(Calendar.HOUR_OF_DAY, 23);
        finSemana.set(Calendar.MINUTE, 59);
        finSemana.set(Calendar.SECOND, 59);
        finSemana.set(Calendar.MILLISECOND, 999);

        long fechaInicio = inicioSemana.getTimeInMillis();
        long fechaFin = finSemana.getTimeInMillis();

        return new Pair<>(fechaInicio, fechaFin);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("urgentes", sUrgentes.isChecked());
        outState.putBoolean("firmados", sFirmados.isChecked());
        outState.putBoolean("pendientes", mPendientes.isChecked());
        outState.putBoolean("parciales", mParciales.isChecked());
        outState.putBoolean("terminados", mTerminados.isChecked());
        outState.putBoolean("fecha", sFecha.isChecked());
        outState.putBoolean("semana", mSemanaActual.isChecked());
        outState.putBoolean("rango", mRango.isChecked());
        if (fechasSeleccionadas != null) {
            outState.putLong("fecha_inicio", fechasSeleccionadas.first);
            outState.putLong("fecha_fin", fechasSeleccionadas.second);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            urgentes = savedInstanceState.getBoolean("urgentes", false);
            firmados = savedInstanceState.getBoolean("firmados", false);
            pendientes = savedInstanceState.getBoolean("pendientes", false);
            parciales = savedInstanceState.getBoolean("parciales", false);
            terminados = savedInstanceState.getBoolean("terminados", false);
            fecha = savedInstanceState.getBoolean("fecha", false);
            semana = savedInstanceState.getBoolean("semana", false);
            rango = savedInstanceState.getBoolean("rango", false);
            fechaInicio = savedInstanceState.getLong("fecha_inicio", -1);
            fechaFin = savedInstanceState.getLong("fecha_fin", -1);

            sUrgentes.setChecked(urgentes);
            sFirmados.setChecked(firmados);
            mPendientes.setChecked(pendientes);
            mParciales.setChecked(parciales);
            mTerminados.setChecked(terminados);

            sFecha.setChecked(fecha);

            mSemanaActual.setEnabled(fecha);
            mSemanaActual.setChecked(semana);

            mRango.setEnabled(fecha);
            mRango.setChecked(rango);

            btnSeleccionarFecha.setEnabled(fecha && rango);

            if (semana && fecha) {
                fechasSeleccionadas = obtenerSemanaActual();
                fechaInicio = fechasSeleccionadas.first;
                fechaFin = fechasSeleccionadas.second;
                btnSeleccionarFecha.setText(formatDateRange(fechaInicio, fechaFin));
            } else if (fechaInicio != -1 && fechaFin != -1 && fecha && rango) {
                fechasSeleccionadas = new Pair<>(fechaInicio, fechaFin);
                btnSeleccionarFecha.setText(formatDateRange(fechaInicio, fechaFin));
            } else {
                btnSeleccionarFecha.setText(R.string.seleccionar_rango_fechas);
                fechasSeleccionadas = null;
            }
        }
    }

}