package com.example.gestoravisos.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestoravisos.R;
import com.example.gestoravisos.basedatos.AvisoEntity;

import java.util.ArrayList;
import java.util.List;

public class CalAvisoAdapter extends RecyclerView.Adapter<CalAvisoAdapter.AvisoViewHolder>{


    private List<AvisoEntity> avisos;
    private OnAvisoClickListener listener;
    private Context contexto;

    public interface OnAvisoClickListener {
        void onAvisoClick(AvisoEntity aviso);
    }

    public CalAvisoAdapter(OnAvisoClickListener listener, Context contexto) {
        this.listener = listener;
        this.avisos = new ArrayList<>();
        this.contexto = contexto;
    }

    public void setAvisos(List<AvisoEntity> avisos) {
        this.avisos = avisos;
        notifyDataSetChanged();
    }



    @NonNull
    @Override
    public AvisoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_aviso, parent, false);
        return new AvisoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvisoViewHolder holder, int position) {
        holder.bind(avisos.get(position));
    }

    @Override
    public int getItemCount() {
        return avisos.size();
    }

    class AvisoViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCliente, tvEstado;
        private LinearLayout calItem;
        private long lastClickTime = 0;

        public AvisoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCliente = itemView.findViewById(R.id.tvCliente);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            calItem = itemView.findViewById(R.id.calItem);

            itemView.setOnClickListener(v -> {
                long clickTime = System.currentTimeMillis();
                if (clickTime - lastClickTime < 300) { // 300ms es el tiempo máximo entre clics para considerarlo doble clic
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onAvisoClick(avisos.get(position));
                    }
                }
                lastClickTime = clickTime;
            });
        }

        public void bind(AvisoEntity aviso) {
            tvCliente.setText(aviso.getNombre_cliente());
            tvEstado.setText(aviso.getEstado());

            switch (aviso.getEstado()) {
                case "PENDIENTE":
                    tvEstado.setText(contexto.getString(R.string.pendiente));
                    tvEstado.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.pendiente));
                    break;
                case "PARCIAL":
                    tvEstado.setText(contexto.getString(R.string.parcial));
                    tvEstado.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.parcial));
                    break;
                case "TERMINADO":
                    tvEstado.setText(contexto.getString(R.string.terminado));
                    tvEstado.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.terminado));
                    break;
                default:
                    break;
            }

            if (aviso.isUrgente())
                calItem.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.urgente));
            else
                calItem.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), android.R.color.transparent));
        }
    }
}
