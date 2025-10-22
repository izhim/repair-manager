package com.example.gestoravisos.Dialogs;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.gestoravisos.Clases.FirmaView;
import com.example.gestoravisos.R;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;

public class FormularioDialog extends DialogFragment {
    private FirmaView firmaView;
    private OnFormularioCompleteListener listener;
    private boolean tieneDatosExistentes = false;
    ArrayList<String> imagenes;

    public interface OnFormularioCompleteListener {
        void onFormularioComplete(String nombre, String dni, Bitmap firma, boolean esTerminado);
        void onDatosCargados(); // Nuevo callback para actualizar detalles
    }

    // Método para crear nueva instancia con datos
    public static FormularioDialog newInstance(String nombre, String dni, byte[] firmaBytes, boolean esTerminado) {
        FormularioDialog dialog = new FormularioDialog();
        Bundle args = new Bundle();
        args.putString("nombre", nombre);
        args.putString("dni", dni);
        args.putByteArray("firma", firmaBytes);
        args.putBoolean("estado_terminado", esTerminado);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_formulario, null);

        final EditText etNombre = view.findViewById(R.id.etNombre);
        final EditText etDni = view.findViewById(R.id.etDni);
        firmaView = view.findViewById(R.id.firmaView);
        Button btnLimpiarFirma = view.findViewById(R.id.btnLimpiarFirma);
        MaterialButtonToggleGroup rgEstado = view.findViewById(R.id.toggleGroupEstado);
        Button btnCancelar = view.findViewById(R.id.btnCancelar);
        Button btnAceptar = view.findViewById(R.id.btnAceptar);

        // Cargar datos existentes si los hay
        if (getArguments() != null) {
            String nombre = getArguments().getString("nombre");
            String dni = getArguments().getString("dni");
            byte[] firmaBytes = getArguments().getByteArray("firma");
            boolean estadoTerminado = getArguments().getBoolean("estado_terminado", false);
            imagenes = getArguments().getStringArrayList("imagenes");

            if (nombre != null && dni != null && firmaBytes != null) {
                tieneDatosExistentes = true;

                // Mostrar datos existentes
                etNombre.setText(nombre);
                etDni.setText(dni);

                // Cargar firma existente
                Bitmap firmaBitmap = BitmapFactory.decodeByteArray(firmaBytes, 0, firmaBytes.length);
                firmaView.cargarFirmaExistente(firmaBitmap);

                // Establecer estado
                if (estadoTerminado) {
                    rgEstado.check(R.id.rbTerminado);
                } else {
                    rgEstado.check(R.id.rbParcial);
                }

                // Deshabilitar edición
                etNombre.setEnabled(false);
                etDni.setEnabled(false);
                btnLimpiarFirma.setEnabled(false);
                btnAceptar.setEnabled(false);

                // Cambiar texto del botón Aceptar
                btnAceptar.setText(getString(R.string.verificado));

                // Notificar que los datos se cargaron
                if (listener != null) {
                    listener.onDatosCargados();
                }
            }
        }

        btnLimpiarFirma.setOnClickListener(v -> firmaView.limpiarFirma());

        btnCancelar.setOnClickListener(v -> dismiss());

        btnAceptar.setOnClickListener(v -> {
            if (tieneDatosExistentes) {
                dismiss(); // Solo cerrar si es visualización
                return;
            }

            String nombre = etNombre.getText().toString().trim();
            String dni = etDni.getText().toString().trim();

            if (nombre.isEmpty() || dni.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.complete_campos), Toast.LENGTH_SHORT).show();
                return;
            }

            if (firmaView.paths.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.ingresar_firma), Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedId = rgEstado.getCheckedButtonId();
            boolean esTerminado = selectedId == R.id.rbTerminado;
            Bitmap firmaBitmap = firmaView.getFirmaBitmap();

            if (listener != null) {
                listener.onFormularioComplete(nombre, dni, firmaBitmap, esTerminado);
            }

            dismiss();
        });

        builder.setView(view);
        return builder.create();
    }

    public void setOnFormularioCompleteListener(OnFormularioCompleteListener listener) {
        this.listener = listener;
    }



}