package com.example.gestoravisos.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gestoravisos.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImagenesAdapter extends RecyclerView.Adapter<ImagenesAdapter.ImageViewHolder> {
    private Context context;
    private List<String> imagePaths;

    public ImagenesAdapter(Context context, ArrayList<String> imageMap) {
        this.context = context;
        this.imagePaths = imageMap;
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_imagen, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        String imagePath = imagePaths.get(position);

        // Usando Glide para cargar la imagen (recomendado)
        Glide.with(context)
                .load(new File(imagePath))
                .into(holder.imageView);
    }

    public int getCount() {
        return imagePaths.size();
    }

    @Override
    public int getItemCount() {
        return imagePaths.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }

    public void actualizarListaCompleta(List<String> nuevasImagenes) {
        this.imagePaths = new ArrayList<>(nuevasImagenes); // Nueva instancia
        notifyDataSetChanged(); // Notificar cambio
    }
}