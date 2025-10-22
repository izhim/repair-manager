package com.example.gestoravisos.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gestoravisos.Adapters.CalAvisoAdapter;
import com.example.gestoravisos.Clases.EventDecorator;
import com.example.gestoravisos.R;
import com.example.gestoravisos.ViewModels.AvisoViewModel;
import com.example.gestoravisos.basedatos.AvisoEntity;
import com.example.gestoravisos.databinding.FragmentCalendarioAvisosBinding;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarioAvisosFragment extends Fragment {

    private FragmentCalendarioAvisosBinding binding;
    private AvisoViewModel avisoViewModel;
    private CalAvisoAdapter avisosAdapter;
    TextView textoFecha;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCalendarioAvisosBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar(view);
        /*
        MaterialCalendarView calendarView = view.findViewById(R.id.calendarView);
        calendarView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.pendiente));

*/
        textoFecha = view.findViewById(R.id.tvFechaAvisos);
        // Configurar ViewModel
        avisoViewModel = new ViewModelProvider(this).get(AvisoViewModel.class);

        // Configurar RecyclerView con el listener correcto
        avisosAdapter = new CalAvisoAdapter(new CalAvisoAdapter.OnAvisoClickListener() {
            @Override
            public void onAvisoClick(AvisoEntity aviso) {
                mostrarDetalleAviso(aviso);
            }
        }, getContext());

        binding.recyclerViewAvisos.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewAvisos.setAdapter(avisosAdapter);

        // Configurar calendario
        binding.calendarView.state().edit()
                .setMinimumDate(CalendarDay.from(2023, 1, 1))
                .setMaximumDate(CalendarDay.from(2025, 12, 31))
                .commit();
        Locale currentLocale = AppCompatDelegate.getApplicationLocales().get(0); // idioma actual
        String[] shortWeekdays = new DateFormatSymbols(currentLocale).getShortWeekdays();

// El array empieza en el índice 1 (1 = Sunday) así que reorganizamos para que el lunes sea el primero
        CharSequence[] diasSemana = new CharSequence[] {
                shortWeekdays[2], // Lunes
                shortWeekdays[3], // Martes
                shortWeekdays[4], // Miércoles
                shortWeekdays[5], // Jueves
                shortWeekdays[6], // Viernes
                shortWeekdays[7], // Sábado
                shortWeekdays[1]  // Domingo
        };
        binding.calendarView.setWeekDayLabels(diasSemana);

        // Configurar el título del mes
        binding.calendarView.setTitleFormatter(day -> {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", currentLocale);
            return sdf.format(day.getDate());
        });

        // Configurar el primer día de la semana (Lunes)
        binding.calendarView.state().edit().setFirstDayOfWeek(1).commit();
        binding.calendarView.setSelectedDate(CalendarDay.today());
        binding.calendarView.setOnMonthChangedListener(this::actualizarDecorador);
        binding.calendarView.setOnDateChangedListener(this::cargarAvisosDelDia);

        // Observar cambios en los avisos
        avisoViewModel.getListaAvisosLiveData().observe(getViewLifecycleOwner(), avisos -> {
            if (avisos != null) {
                actualizarDecorador(binding.calendarView, binding.calendarView.getCurrentDate());
                cargarAvisosDelDia(binding.calendarView, binding.calendarView.getSelectedDate(), true);
            }
        });
    }

    private void setupToolbar(View view) {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == android.R.id.home) {
                    Navigation.findNavController(view).navigateUp();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setTitle(requireContext().getString(R.string.barraCalendario));
        }
    }


    private void actualizarDecorador(MaterialCalendarView widget, CalendarDay date) {
        List<AvisoEntity> avisos = avisoViewModel.getListaAvisosLiveData().getValue();
        if (avisos == null) return;

        widget.removeDecorators();

        Set<CalendarDay> daysWithNormalEvents = new HashSet<>();
        Set<CalendarDay> daysWithUrgentEvents = new HashSet<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        for (AvisoEntity aviso : avisos) {
            try {
                Date fechaDate = sdf.parse(aviso.getFecha_ejecucion());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(fechaDate);

                CalendarDay day = CalendarDay.from(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

                // Verificar si el aviso es urgente
                if (aviso.isUrgente()) {
                    daysWithUrgentEvents.add(day);
                } else {
                    daysWithNormalEvents.add(day);
                }

            } catch (ParseException e) {
                Log.e("Calendario", "Error al parsear fecha: " + aviso.getFecha_ejecucion(), e);
            }
        }

        // Añadir decoradores para días normales y urgentes
        if (!daysWithNormalEvents.isEmpty()) {
            widget.addDecorator(new EventDecorator(daysWithNormalEvents, requireContext(), false));
        }

        if (!daysWithUrgentEvents.isEmpty()) {
            widget.addDecorator(new EventDecorator(daysWithUrgentEvents, requireContext(), true));
        }

        widget.invalidateDecorators();
    }

    private void cargarAvisosDelDia(MaterialCalendarView widget, CalendarDay date, boolean selected) {
        if (!selected) return;

        List<AvisoEntity> todosAvisos = avisoViewModel.getListaAvisosLiveData().getValue();
        if (todosAvisos == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        List<AvisoEntity> avisosDelDia = new ArrayList<>();

        for (AvisoEntity aviso : todosAvisos) {
            try {
                Date fecha = sdf.parse(aviso.getFecha_ejecucion());
                Calendar cal = Calendar.getInstance();
                cal.setTime(fecha);

                CalendarDay avisoDay = CalendarDay.from(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) ,
                        cal.get(Calendar.DAY_OF_MONTH)
                );

                Log.d("DEBUG", "AVISO: " + aviso.getFecha_ejecucion() + " -> CalendarDay: " + avisoDay);
                Log.d("DEBUG", "DATE SELECCIONADA: " + date);

                if (avisoDay.equals(date)) {
                    avisosDelDia.add(aviso);
                }

            } catch (ParseException e) {
                Log.e("CALENDARIO", "Error al parsear fecha: " + aviso.getFecha_ejecucion(), e);
            }
        }

        avisosAdapter.setAvisos(avisosDelDia);
        textoFecha.setText(calendarToString(date));

        if (avisosDelDia.isEmpty()) {
            Toast.makeText(getContext(), requireContext().getString(R.string.no_avisos) + date.getDay() + "/" + (date.getMonth() + 1) + "/" + date.getYear(), Toast.LENGTH_SHORT).show();
        }
    }

    public String calendarToString(CalendarDay day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(day.getYear(), day.getMonth(), day.getDay()); // ¡Mes base 0!
        Date date = calendar.getTime();
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
    }

    private void mostrarDetalleAviso(AvisoEntity aviso) {
        if (aviso != null) {
            Bundle args = new Bundle();
            args.putParcelable("aviso", aviso);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_calendarioAvisosFragment_to_detallesTrabajoFragment, args);
        }
    }







    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null; // Limpiar el binding cuando la vista es destruida
    }


}