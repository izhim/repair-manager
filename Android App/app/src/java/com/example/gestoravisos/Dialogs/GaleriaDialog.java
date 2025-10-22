package com.example.gestoravisos.Dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestoravisos.Adapters.ImagenesAdapter;
import com.example.gestoravisos.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class GaleriaDialog extends DialogFragment {

    private static final String ARG_RUTAS = "rutas";
    private String path;
    private ImageButton captura;
    private ArrayList<String> rutas;
    private ImagenesAdapter adapter;

    // Nuevo launcher para capturar la imagen

    public static GaleriaDialog newInstance(ArrayList<String> rutas) {
        GaleriaDialog fragment = new GaleriaDialog();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_RUTAS, rutas);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        rutas = getArguments().getStringArrayList(ARG_RUTAS);



        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.galeria));

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_imagenes, null);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewImages);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 2 columnas
        adapter  = new ImagenesAdapter(getContext(), rutas);
        recyclerView.setAdapter(adapter);



        builder.setView(view);
        builder.setNegativeButton(getString(R.string.btnCerrar), (dialog, which) -> dismiss());

        return builder.create();
    }


}