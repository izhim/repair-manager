package com.example.gestoravisos.Fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.gestoravisos.Activities.MainActivity;
import com.example.gestoravisos.Activities.Preferencias;
import com.example.gestoravisos.Adapters.AvisoAdapter;
import com.example.gestoravisos.Clases.PollingWorker;
import com.example.gestoravisos.Login.Ui.Login.LoginViewModel;
import com.example.gestoravisos.Login.Ui.Login.LoginViewModelFactory;
import com.example.gestoravisos.R;
import com.example.gestoravisos.ViewModels.AvisoViewModel;
import com.example.gestoravisos.basedatos.AvisoEntity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ListaTrabajosFragment extends Fragment implements AvisoAdapter.OnDoubleClickListener {

    private AvisoViewModel avisoViewModel;
    private RecyclerView recyclerView;
    private AvisoAdapter avisoAdapter;
    // Bandera para controlar la primera configuración
    private boolean mIsFirstConfig = true;
    private boolean recreado = false;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configurarPolling();
        Log.e("PRUEBA", "SE EJECUTA EL ONCREATE");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_trabajos, container, false);

        // Inicializar el RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inicializar el adaptador
        avisoAdapter = new AvisoAdapter(getViewLifecycleOwner(), getContext(), this);
        ordenarAvisos(avisoAdapter);
        recyclerView.setAdapter(avisoAdapter);


        return view;
    }




    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // Configurar MenuProvider para el menú de opciones
        MenuProvider menuProvider = new MenuProvider() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu, menu); // Inflar el menú
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                // Manejar clics en los ítems del menú
                int itemId = menuItem.getItemId();
                if(itemId == R.id.itCalendario){
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_listaTrabajosFragment_to_calendarioAvisosFragment);
                    return true;
                }else if (itemId == R.id.itFiltrar) {
                    showFilterBottomSheet();
                    return true;
                } else if (itemId == R.id.itPreferencias) {
                    mIsFirstConfig = false;
                    actividadLauncher.launch(new Intent(getContext(), Preferencias.class));
                    return true;

                } else if (itemId == R.id.itSincronizar) {
                    avisoViewModel.sincronizarAvisos(getContext());
                    return true;

                } else if (itemId == R.id.itEliminarDatos) {
                    eliminarDatos();
                    return true;

                }else if (itemId == R.id.itCerrarSesion){
                    confirmarCerrarSesion();
                    return true;

                } else if (itemId == R.id.itSalir) {
                    confirmarSalirAplicacion();
                    return true;

                } else {
                    return false;
                }
            }
        };
        requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setTitle(requireContext().getString(R.string.app_name));

        }

        // Obtener el ViewModel
        avisoViewModel = new ViewModelProvider(this).get(AvisoViewModel.class);

        // Aplicar las preferencias de filtración
        avisoViewModel.aplicarFiltrosDesdePreferencias(requireContext());

        // Observar los cambios en la lista de avisos
        avisoViewModel.getListaFiltrada().observe(getViewLifecycleOwner(), new Observer<List<AvisoEntity>>() {
            @Override
            public void onChanged(List<AvisoEntity> avisos) {
                if (avisos != null) {
                    ordenarAvisos(avisoAdapter);
                    avisoAdapter.setAvisos(avisos);
                }
            }
        });
        avisoViewModel.getListaAvisosLiveData().observe(getViewLifecycleOwner(), avisos -> {
            avisoViewModel.actualizarListaFiltrada();
        });

        avisoViewModel.ldConectado.observe(getViewLifecycleOwner(), event -> {
            Boolean success = event.getContentIfNotHandled();
            if (success != null) {
                String msg = success ? getString(R.string.sync_exitosa) : getString(R.string.sync_error);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        SharedPreferences.OnSharedPreferenceChangeListener listener = (prefs, key) -> {
            if (key.equals("sync_interval") && isAdded()) {
                configurarPolling();
            }
        };

        int actionBarHeight = 0;
        if(!recreado){
            TypedValue tv = new TypedValue();
            if (getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());

        }

        recyclerView.setPadding(
                recyclerView.getPaddingLeft(),
                actionBarHeight,
                recyclerView.getPaddingRight(),
                recyclerView.getPaddingBottom()
        );
    }



    private ActivityResultLauncher<Intent> actividadLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Preferencias.RESULT_OK) {

                    requireActivity().recreate();

                } else {
                }
            }
    );

    @Override
    public void onDoubleClick(AvisoEntity aviso) {
        // Manejar el doble click aquí
        abrirDetalleAviso(aviso);
    }

    private void abrirDetalleAviso(AvisoEntity aviso) {
        // Implementa la lógica para mostrar los detalles del aviso
        Bundle bundle = new Bundle();
        bundle.putParcelable("aviso", aviso);

        Navigation.findNavController(requireView())
                .navigate(R.id.action_listaTrabajos_to_detalleTrabajo, bundle);
    }

    private void showFilterBottomSheet() {
        BotonFiltrarFragment bottomSheet = new BotonFiltrarFragment();

        bottomSheet.setFilterListener(new BotonFiltrarFragment.FilterListener() {
            @Override
            public void onFiltersApplied(
                    boolean urgentes,
                    boolean  firmados,
                    boolean  pendientes,
                    boolean parciales,
                    boolean terminados,
                    Pair<Long, Long> fechasSeleccionadas
            ) {
                avisoViewModel.aplicarFiltros(
                    urgentes,
                    firmados,
                    pendientes,
                    parciales,
                    terminados,
                    fechasSeleccionadas
                );
            }

            @Override
            public void onFiltersCleared() {
                avisoViewModel.limpiarFiltros();
            }
        });
        bottomSheet.show(getParentFragmentManager(), bottomSheet.getTag());
    }

    @Override
    public void onResume() {
        ordenarAvisos(avisoAdapter);
        super.onResume();
        ((MainActivity) requireActivity()).showBackButton(false);
        setupPreferenceListener();
    }


    public void ordenarAvisos(AvisoAdapter adapter){
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean ascendente = preferencias.getBoolean("orden", true);
        adapter.setOrdenAscendente(ascendente);
    }

    private void confirmarCerrarSesion() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(requireContext().getString(R.string.cerrar_sesion))
                .setMessage(requireContext().getString(R.string.pregunta_salir))
                .setPositiveButton(requireContext().getString(R.string.si), (dialog, which) -> {
                    realizarLogout();
                })
                .setNegativeButton(requireContext().getString(R.string.cancelar), null)
                .show();
    }

    private void realizarLogout() {
        // Obtener el ViewModel usando la factory correcta
        LoginViewModel loginViewModel = new ViewModelProvider(
                requireActivity(),
                new LoginViewModelFactory(requireContext())
        ).get(LoginViewModel.class);

        // Ejecutar logout
        loginViewModel.logout();

        // Navegar al login
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_global_logout);
    }

    private void confirmarSalirAplicacion() {
        mostrarDialogoPersonalizado(
                getString(R.string.salir),
                getString(R.string.pregunta_salir),
                ContextCompat.getDrawable(requireContext(), R.drawable.salida),
                this::salirDeLaAplicacion
        );
    }

    private void salirDeLaAplicacion() {
        requireActivity().finishAffinity();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void mostrarDialogoPersonalizado(String titulo, String mensaje, Drawable salida, Runnable accionPositiva) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogTheme);

        builder.setTitle(titulo)
                .setMessage(mensaje)
                .setIcon(salida)
                .setPositiveButton(requireContext().getString(R.string.confirmar), (d, w) -> accionPositiva.run())
                .setNegativeButton(requireContext().getString(R.string.cancelar), null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        });
        dialog.show();
    }


    /*  CÓDIGO PARA PRUEBAS
    private void configurarPolling() {
        // Definir las restricciones (opcional)
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Crear la solicitud de trabajo único (para pruebas)
        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(PollingWorker.class)
                        .setConstraints(constraints)
                        .setInitialDelay(15, TimeUnit.SECONDS) // puedes poner 0 para que sea inmediato
                        .build();

        // Encolar el trabajo único
        WorkManager.getInstance(getContext()).enqueueUniqueWork(
                "polling_work",
                ExistingWorkPolicy.REPLACE, // Reemplaza si ya existe
                workRequest
        );
    }
*/
    private void configurarPolling() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String intervaloStr = prefs.getString("sync_interval", "900000");
        long intervaloMillis;

        try {
            intervaloMillis = Long.parseLong(intervaloStr);
        } catch (NumberFormatException e) {
            intervaloMillis = 900000;
        }

        WorkManager workManager = WorkManager.getInstance(requireContext());
        workManager.cancelUniqueWork("PollingWorker");

        if (intervaloMillis > 0 && intervaloMillis >= PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS) {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest pollingRequest =
                    new PeriodicWorkRequest.Builder(PollingWorker.class, intervaloMillis, TimeUnit.MILLISECONDS)
                            .setConstraints(constraints)
                           // .setInitialDelay(1, TimeUnit.SECONDS)
                            .addTag("sync_worker")
                            .build();

            workManager.enqueueUniquePeriodicWork(
                    "PollingWorker",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    pollingRequest
            );

            Log.d("POLLING_SETUP", "Polling configurado cada " + (intervaloMillis / 60000) + " minutos");
        } else {
            Log.d("POLLING_SETUP", "Sincronización desactivada o intervalo menor al mínimo permitido");
        }
    }

    private void setupPreferenceListener() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        // Configuración inicial solo en la primera carga
        if (mIsFirstConfig) {
            mIsFirstConfig = false;
            configurePolling(false); // false = no forzar ejecución
        }
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = (prefs, key) -> {
        if ("sync_interval".equals(key)) {
            configurePolling(false); // false = solo cambiar frecuencia, no ejecutar
        }
    };

    private void configurePolling(boolean shouldRunNow) {
        String intervaloStr = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("sync_interval", "900000");
        long intervalo;

        try {
            intervalo = Long.parseLong(intervaloStr);
        } catch (NumberFormatException e) {
            intervalo = 900000;
        }

        WorkManager workManager = WorkManager.getInstance(requireContext());
        workManager.cancelUniqueWork("PollingWorker");

        if (intervalo > 0) {
            PeriodicWorkRequest.Builder builder = new PeriodicWorkRequest.Builder(
                    PollingWorker.class,
                    intervalo,
                    TimeUnit.MILLISECONDS
            )
                    .addTag("sync_worker");

            // Solo añade initialDelay si NO queremos ejecución inmediata
            if (!shouldRunNow) {
                builder.setInitialDelay(intervalo, TimeUnit.MILLISECONDS);
            }

            workManager.enqueueUniquePeriodicWork(
                    "PollingWorker",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    builder.build()
            );
        }
    }

    public void eliminarDatos() {
        Context contexto = getContext();
        // Crear un AlertDialog para confirmar la eliminación
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(contexto,
                R.style.CustomAlertDialogTheme);


        builder.setTitle(R.string.confirmar_eliminacion)
                .setMessage(R.string.info_eliminacion)
                .setIcon(R.drawable.ic_warning)

                // 3. Personalizar botones
                .setPositiveButton(R.string.aceptar, (dialog, which) -> {
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            avisoViewModel.eliminarDatos();
                            avisoViewModel.actualizarLista();
                        }
                    });


                })
                .setNegativeButton(R.string.cancelar, null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // 5. Cambiar color del texto y botones
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(ContextCompat.getColor(contexto, R.color.colorPrimary));

            Button negativeButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setTextColor(ContextCompat.getColor(contexto, R.color.colorAccent));
        });

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }
    }

}
