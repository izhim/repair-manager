package com.example.gestoravisos.Dialogs;

import static android.app.PendingIntent.getActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.gestoravisos.R;

public class ObservacionesDialog extends DialogFragment {
    private String observaciones;
    private OnObservacionesCompleteListener listener;

    public interface OnObservacionesCompleteListener {
        void onObservacionesComplete(String observaciones);
        void onObservacionesLoaded();
    }

    public static ObservacionesDialog newInstance() {
        return new ObservacionesDialog();
    }

    public void setOnObservacionesCompleteListener(OnObservacionesCompleteListener listener) {
        this.listener = listener;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_observaciones, null);

        EditText editText = view.findViewById(R.id.textObservaciones);
        Button saveButton = view.findViewById(R.id.obsBtnGuardar);
        Button cancelButton = view.findViewById(R.id.obsBtnCancelar);

        // Cargar observaciones si existen
        if (observaciones != null) {
            editText.setText(observaciones);
            editText.setSelection(observaciones.length());
        }

        if (listener != null) {
            listener.onObservacionesLoaded();
        }

        saveButton.setOnClickListener(v -> {
            String texto = editText.getText().toString();
            if (listener != null) {
                listener.onObservacionesComplete(texto);
            }
            dismiss();
        });

        cancelButton.setOnClickListener(v -> dismiss());

        builder.setView(view);
        return builder.create();
    }
}