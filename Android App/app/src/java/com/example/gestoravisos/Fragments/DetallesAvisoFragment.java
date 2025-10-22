package com.example.gestoravisos.Fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import com.example.gestoravisos.Activities.MainActivity;
import com.example.gestoravisos.Adapters.ImagenesAdapter;
import com.example.gestoravisos.Dialogs.FormularioDialog;
import com.example.gestoravisos.Dialogs.ObservacionesDialog;
import com.example.gestoravisos.R;
import com.example.gestoravisos.basedatos.AvisoEntity;
import com.example.gestoravisos.basedatos.RepositorioSQLite;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class DetallesAvisoFragment extends Fragment {
    private AvisoEntity aviso;
    private View headerLayout;
    private ImageButton botonFirmar;
    private ImageButton botonGaleria;
    private ImageButton botonFotos;
    private RepositorioSQLite repositorio;
    private Dialog dialog;
    private String path;
    private ActivityResultLauncher<Intent> captureImageLauncher;
    private ImagenesAdapter adapter;
    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    TextView tvNombre, tvDireccion, tvTelefono, tvPoblacion, tvDescripcion, tvObservaciones, tvTurno, tvFecha;



    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            aviso = getArguments().getParcelable("aviso");
        }

        repositorio = RepositorioSQLite.getInstance(requireContext());

        // Inicializar el ActivityResultLauncher
        captureImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (path != null) {
                            // Asegurar que el mapa no sea null
                            Map<Integer, String> imagenes = aviso.getImagenesInst();
                            if (imagenes == null) {
                                imagenes = new HashMap<>();
                                aviso.setImagenesInst(imagenes); // asegúrate de tener este setter
                            }

                            int nextId = imagenes.isEmpty() ? 1 :
                                    Collections.max(imagenes.keySet()) + 1;
                            imagenes.put(nextId, path);
                            repositorio.modificarAviso(aviso);

                            Log.e("ATENCION", "path añadido: " + nextId + " / " + path);

                            if (adapter != null) {
                                List<String> nuevasImagenes = new ArrayList<>(imagenes.values());
                                adapter.actualizarListaCompleta(nuevasImagenes);

                                // Desplazar al final para mostrar la nueva imagen
                                RecyclerView recyclerView = dialog.findViewById(R.id.recyclerViewImages);
                                if (recyclerView != null) {
                                    recyclerView.smoothScrollToPosition(nuevasImagenes.size() - 1);
                                }
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), requireContext().getString(R.string.captura_cancel), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detalles_aviso, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) requireActivity()).showBackButton(true);
        actualizarVista(); // Actualizar vista al volver
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvNombre = view.findViewById(R.id.tvNombre);
        tvDireccion = view.findViewById(R.id.tvDireccion);
        tvTelefono = view.findViewById(R.id.tvTelefono);
        tvPoblacion = view.findViewById(R.id.tvPoblacion);
        tvDescripcion = view.findViewById(R.id.tvDescripcion);
        tvObservaciones = view.findViewById(R.id.tvObservaciones);
        tvTurno = view.findViewById(R.id.tvTurno);
        tvFecha = view.findViewById(R.id.tvFecha);

        setupToolbar(view);
        setupViews(view);
        cargarDatosAviso();
    }

    private void setupToolbar(View view) {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                // Inflar menú si es necesario
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
            activity.getSupportActionBar().setTitle(requireContext().getString(R.string.barraDetalles));
        }
    }

    private void setupViews(View view) {
        headerLayout = view.findViewById(R.id.headerLayout);
        botonFirmar = view.findViewById(R.id.btnFirmar);
        botonFirmar.setOnClickListener(v -> mostrarDialogoFormulario());
        ImageButton btnObservaciones = view.findViewById(R.id.btnObservaciones);
        btnObservaciones.setOnClickListener(v -> mostrarDialogoObservaciones());
        botonGaleria = view.findViewById(R.id.btnImagenes);
        botonGaleria.setOnClickListener(v -> showImageDialog(true));
        botonFotos = view.findViewById(R.id.btnTomarFoto);
        botonFotos.setOnClickListener(v -> showImageDialog(false));
        tvObservaciones = view.findViewById((R.id.tvObservaciones));
        TextView tvTelefono = view.findViewById(R.id.tvTelefono);
        tvTelefono.setOnLongClickListener(v -> {
            mostrarMenuTelefono(aviso.getTelefono());
            return true;
        });
    }

    private void cargarDatosAviso() {
        if (aviso == null) return;

        // Configurar vistas
        View view = getView();
        if (view == null) return;

        // Mostrar datos
        tvNombre.setText(aviso.getNombre_cliente());
        tvDireccion.setText(aviso.getDireccion());
        tvTelefono.setText(aviso.getTelefono());
        tvPoblacion.setText(aviso.getPoblacion());
        tvDescripcion.setText(aviso.getDescripcion());
        tvObservaciones.setText(aviso.getObsInstalador());
        //tvObservaciones.setText(aviso.getObservaciones());

        switch (aviso.getTurno()) {
            case "MANANAS":
                tvTurno.setText(R.string.mananas);
                break;
            case "TARDES":
                tvTurno.setText(R.string.tardes);
                break;
            case "INDIFERENTE":
                tvTurno.setText(null);
                break;
        }
        tvFecha.setText(aviso.getFecha_aviso());

        // Configurar estado
        configurarEstado();
    }

    private void configurarEstado() {
        if (aviso == null) return;
        ColorStateList activado = ContextCompat.getColorStateList(requireContext(), R.color.colorAccent);
        ColorStateList desactivado = ContextCompat.getColorStateList(requireContext(), R.color.btn_disabled);
        int colorRes;
        switch (aviso.getEstado()) {
            case "PENDIENTE":
                colorRes = R.color.pendiente;
                botonFirmar.setEnabled(true);
                botonFirmar.setImageTintList(activado);
                break;
            case "PARCIAL":
                colorRes = R.color.parcial;
                if(aviso.getFirma() == null){
                    botonFirmar.setEnabled(true);
                    botonFirmar.setImageTintList(activado);
                }else{
                    botonFirmar.setEnabled(false);
                    botonFirmar.setImageTintList(desactivado);
                }
                break;
            case "TERMINADO":
                colorRes = R.color.terminado;
                botonFirmar.setEnabled(false);
                botonFirmar.setImageTintList(desactivado);
                break;
            default:
                colorRes = R.color.pendiente;
                break;
        }
        botonGaleria.setImageTintList(aviso.getImagenes().isEmpty() ? desactivado : activado);
        botonGaleria.setEnabled(!aviso.getImagenes().isEmpty());

        headerLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), colorRes));
        if (aviso.isUrgente()) {
            View rootView = getView();
            if (rootView != null) {
                rootView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.urgente));
            }
        }

        if(aviso.getObservaciones() == null || aviso.getObservaciones().isEmpty()){
            TypedValue typedValue = new TypedValue();
            Context context = tvNombre.getContext();
            context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
            int color = ContextCompat.getColor(context, typedValue.resourceId);
            tvNombre.setTextColor(color);
            tvNombre.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            tvNombre.setClickable(false);
            tvNombre.setFocusable(false);
            tvNombre.setOnClickListener(null);
        }else{
            tvNombre.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
            tvNombre.setCompoundDrawablesWithIntrinsicBounds(R.drawable.obs_cliente, 0, 0, 0);
            tvNombre.setClickable(true);
            tvNombre.setFocusable(true);
            tvNombre.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle(requireContext().getString(R.string.tituloObs))
                        .setMessage(aviso.getObservaciones())
                        .setNeutralButton(requireContext().getString(R.string.btnCerrar), (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .show();
            });
        }
    }

    private void mostrarDialogoFormulario() {
        // Obtener datos actualizados del aviso
        repositorio.getExecutorService().execute(() -> {
            AvisoEntity avisoActualizado = repositorio.getBaseDatos().avisoDAO().getAvisoById(aviso.getId_aviso());

            requireActivity().runOnUiThread(() -> {
                FormularioDialog dialog = FormularioDialog.newInstance(
                        avisoActualizado.getNombre_firma(),
                        avisoActualizado.getDni_firma(),
                        avisoActualizado.getFirma(),
                        "TERMINADO".equals(avisoActualizado.getEstado())
                );

                dialog.setOnFormularioCompleteListener(new FormularioDialog.OnFormularioCompleteListener() {
                    @Override
                    public void onFormularioComplete(String nombre, String dni, Bitmap firma, boolean esTerminado) {
                        guardarYActualizar(nombre, dni, firma, esTerminado);
                    }

                    @Override
                    public void onDatosCargados() {
                        actualizarVista();
                    }
                });

                dialog.show(getParentFragmentManager(), "FormularioDialog");
            });
        });
    }

    private void mostrarDialogoObservaciones() {
        // Obtener datos actualizados del aviso
        repositorio.getExecutorService().execute(() -> {
            AvisoEntity avisoActualizado = repositorio.getBaseDatos().avisoDAO().getAvisoById(aviso.getId_aviso());

            requireActivity().runOnUiThread(() -> {
                ObservacionesDialog dialog = ObservacionesDialog.newInstance();
                dialog.setObservaciones(avisoActualizado.getObsInstalador());

                dialog.setOnObservacionesCompleteListener(new ObservacionesDialog.OnObservacionesCompleteListener() {
                    @Override
                    public void onObservacionesComplete(String observaciones) {
                        repositorio.guardarObservaciones(aviso.getId_aviso(), observaciones);
                        aviso.setObservaciones(observaciones);
                        if (getView() != null) {
                            TextView tvObs = getView().findViewById(R.id.tvObservaciones);
                            if (tvObs != null) {
                                tvObs.setText(observaciones);
                            }
                        }
                    }

                    @Override
                    public void onObservacionesLoaded() {    }
                });

                dialog.show(getChildFragmentManager(), "ObservacionesDialog");
            });
        });
    }

    private void guardarYActualizar(String nombre, String dni, Bitmap firma, boolean esTerminado) {
        repositorio.guardarDatosFirma(aviso.getId_aviso(), nombre, dni, firma, esTerminado);

        // Actualizar el aviso local con los nuevos datos
        aviso.setNombre_firma(nombre);
        aviso.setDni_firma(dni);
        aviso.setEstado(esTerminado ? "TERMINADO" : "PARCIAL");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        firma.compress(Bitmap.CompressFormat.PNG, 100, stream); // O JPEG si prefieres
        aviso.setFirma(stream.toByteArray());

        // Actualizar la UI
        actualizarVista();

        Toast.makeText(requireContext(), requireContext().getString(R.string.datos_firma_guardados), Toast.LENGTH_SHORT).show();
    }

    private void mostrarMenuTelefono(String numeroTelefono) {
        // Eliminar caracteres no numéricos por si acaso
        String numeroLimpio = numeroTelefono.replaceAll("[^0-9+]", "");

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(requireContext().getString(R.string.opciones_para) + numeroTelefono);

        // Opciones del menú
        String[] opciones = getResources().getStringArray(R.array.opciones_dialogo_numero);

        builder.setItems(opciones, (dialog, which) -> {
            switch (which) {
                case 0: // Llamar
                    realizarLlamada(numeroLimpio);
                    break;
                case 1: // WhatsApp
                    enviarWhatsApp(numeroLimpio);
                    break;
                case 2: // Copiar
                    copiarNumero(numeroTelefono);
                    break;
                case 3: // Cancelar
                    dialog.dismiss();
                    break;
            }
        });

        builder.show();
    }

    private void realizarLlamada(String numero) {
        // Verificar permiso de llamada
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CALL_PHONE},
                    101);
        } else {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + numero));
            startActivity(intent);
        }
    }
    private void enviarWhatsApp(String numero) {
        try {
            // Verificar si WhatsApp está instalado
            PackageManager pm = requireContext().getPackageManager();
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);

            // Abrir chat en WhatsApp
            Uri uri = Uri.parse("https://wa.me/" + numero);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (PackageManager.NameNotFoundException e) {
            // WhatsApp no está instalado
            Toast.makeText(requireContext(), requireContext().getString(R.string.whats_no_instalado), Toast.LENGTH_SHORT).show();
        }
    }

    private void copiarNumero(String numero) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Número de teléfono", numero);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), requireContext().getString(R.string.num_copiado), Toast.LENGTH_SHORT).show();
    }


    private void actualizarVista() {
        if (isAdded() && getView() != null) {
            configurarEstado();
        }
    }

    private void showImageDialog(Boolean cliente) {
        // Crear el Dialog con estilo para que ocupe casi toda la pantalla
        dialog = new Dialog(requireActivity());
        dialog.setContentView(R.layout.dialog_imagenes);

        TextView titulo = dialog.findViewById(R.id.dialogTitle);
        Button btnBorrar = dialog.findViewById(R.id.btnBorrar);
        ImageButton btnCaptura = dialog.findViewById(R.id.btnCaptura);
        ArrayList<String> imagenes = new ArrayList<>();
        if(cliente){
            titulo.setText(requireContext().getString(R.string.imgCliente));
            btnBorrar.setVisibility(View.INVISIBLE);
            btnBorrar.setEnabled(false);
            btnCaptura.setVisibility(View.INVISIBLE);
            btnCaptura.setEnabled(false);
            if(aviso.getImagenes() != null)
                imagenes.addAll(aviso.getImagenes().values());
        }else{
            titulo.setText(requireContext().getString(R.string.imgInstalador));
            btnBorrar.setVisibility(View.VISIBLE);
            btnBorrar.setEnabled(true);
            btnCaptura.setVisibility(View.VISIBLE);
            btnCaptura.setEnabled(true);
            if(aviso.getImagenesInst() != null)
                imagenes.addAll(aviso.getImagenesInst().values());
        }
        // Hacer que ocupe casi toda la pantalla
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Configurar el RecyclerView
        RecyclerView recyclerView = dialog.findViewById(R.id.recyclerViewImages);

        // Configurar el LinearLayoutManager horizontal
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        // Añadir efecto de snap (opcional)
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        // Configurar el adaptador

        adapter = new ImagenesAdapter(getContext(), imagenes);
        recyclerView.setAdapter(adapter);

        // Configurar el botón de cerrar
        Button btnClose = dialog.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnCaptura.setOnClickListener(v -> {
            Log.d("CAPTURA", "Click detectado");
            tomarImagen();
        });

        // Configurar el botón de borrar
        btnBorrar.setOnClickListener(v -> {
            int posicionVisible = ((LinearLayoutManager)recyclerView.getLayoutManager())
                    .findFirstVisibleItemPosition();

            if (posicionVisible != RecyclerView.NO_POSITION) {
                borrarImagen(posicionVisible, cliente);
            }
        });

        // Mostrar el Dialog
        dialog.show();
    }


    private void tomarImagen() {
        // Verificar permisos primero
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Verificar si hay apps de cámara disponibles
        /*
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) == null) {
            Toast.makeText(requireContext(), "No hay aplicación de cámara disponible", Toast.LENGTH_SHORT).show();
            return;
        }
*/
        // Crear archivo para la imagen
        File photoFile = null;
        try {
            photoFile = crearArchivo();
        } catch (IOException ex) {
            Toast.makeText(requireContext(),requireContext().getString(R.string.error_crear_archivo), Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            try {
                Uri photoURI = FileProvider.getUriForFile(
                        requireContext(),
                        "com.example.android.fileprovider",
                        photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Lanzar la cámara
                captureImageLauncher.launch(takePictureIntent);
            } catch (IllegalArgumentException e) {
                Toast.makeText(requireContext(), requireContext().getString(R.string.error_acceso_archivo), Toast.LENGTH_SHORT).show();
                Log.e("Camera", "File provider error", e);
            }
        }
    }

    private File crearArchivo() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        // Obtener el directorio con verificación de null
        File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null || !storageDir.exists()) {
            storageDir = getContext().getFilesDir(); // Fallback al directorio interno
        }

        File image = File.createTempFile(
                imageFileName,  /* prefijo */
                ".jpg",        /* sufijo */
                storageDir      /* directorio */
        );

        path = image.getAbsolutePath();
        return image;
    }

    private void borrarImagen(int position, boolean esCliente) {
        // Crear diálogo de confirmación
        new AlertDialog.Builder(requireContext())
                .setTitle(requireContext().getString(R.string.confirmar_borrado))
                .setMessage(requireContext().getString(R.string.confirmacion_borrado))
                .setPositiveButton(requireContext().getString(R.string.eliminar), (dialog, which) -> {
                    // Lógica de borrado al confirmar
                    ejecutarBorradoImagen(position, esCliente);
                })
                .setNegativeButton(requireContext().getString(R.string.cancelar), null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void ejecutarBorradoImagen(int position, boolean esCliente) {
        if (aviso == null || adapter == null) return;

        try {
            Map<Integer, String> mapaImagenes = aviso.getImagenesInst();
            if (mapaImagenes == null || mapaImagenes.isEmpty()) return;

            // 1. Obtener y eliminar la imagen
            Integer key = (Integer) mapaImagenes.keySet().toArray()[position];
            String ruta = mapaImagenes.remove(key); // Elimina y obtiene la ruta

            // 2. Borrar archivo físico
            if (ruta != null) {
                File file = new File(ruta);
                if (file.exists()) file.delete();
            }

            // 3. Actualizar modelo y base de datos
            aviso.setImagenesInst(mapaImagenes);
            repositorio.modificarAviso(aviso);

            // 4. Actualizar UI
            requireActivity().runOnUiThread(() -> {
                // Crear nueva lista inmutables
                List<String> nuevasImagenes = new ArrayList<>(mapaImagenes.values());

                // Actualizar adaptador (versión mejorada)
                adapter.actualizarListaCompleta(nuevasImagenes);

                // Feedback visual
                Toast.makeText(getContext(), requireContext().getString(R.string.img_borrada), Toast.LENGTH_SHORT).show();

                // Cerrar diálogo si está vacío
                if (mapaImagenes.isEmpty() && dialog != null) {
                    dialog.dismiss();
                }
            });

        } catch (Exception e) {
            Log.e("Borrado", "Error: " + e.getMessage());
            Toast.makeText(getContext(), requireContext().getString(R.string.error_borrado), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        super.onDestroyView();
    }

}