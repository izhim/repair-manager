package com.example.gestoravisos.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestoravisos.Clases.AgrupadorAvisos;
import com.example.gestoravisos.Clases.GrupoAvisos;
import com.example.gestoravisos.R;
import com.example.gestoravisos.basedatos.AvisoEntity;
import com.example.gestoravisos.basedatos.RepositorioSQLite;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AvisoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private Context contexto;
    private List<GrupoAvisos> grupos;
    private RepositorioSQLite repositorio;
    private static OnDoubleClickListener doubleClickListener;
    private boolean ordenAscendente = true; // Valor por defecto


    public AvisoAdapter(LifecycleOwner lifecycleOwner, Context contexto, OnDoubleClickListener listener) {
        this.contexto = contexto;
        this.repositorio = RepositorioSQLite.getInstance(contexto);
        this.grupos = new ArrayList<>();
        this.doubleClickListener = listener;

        repositorio.getListaAvisosLiveData().observe(lifecycleOwner, avisos -> {
            if (avisos != null) {
                this.grupos = AgrupadorAvisos.groupByFecha(avisos, ordenAscendente);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        int currentGroup = 0;
        int itemsProcessed = 0;

        while (currentGroup < grupos.size()) {
            if (position == itemsProcessed) {
                return TYPE_HEADER;
            }
            itemsProcessed++;

            int itemsInGroup = grupos.get(currentGroup).getAvisos().size();
            if (position < itemsProcessed + itemsInGroup) {
                return TYPE_ITEM;
            }
            itemsProcessed += itemsInGroup;
            currentGroup++;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cabecera_elementos, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.vista_elemento, parent, false);
            return new ItemViewHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int currentGroup = 0;
        int itemsProcessed = 0;

        while (currentGroup < grupos.size()) {
            if (position == itemsProcessed) {
                ((HeaderViewHolder) holder).bind(grupos.get(currentGroup).getFecha());
                return;
            }
            itemsProcessed++;

            int itemsInGroup = grupos.get(currentGroup).getAvisos().size();
            if (position < itemsProcessed + itemsInGroup) {
                int itemPosition = position - itemsProcessed;
                ((ItemViewHolder) holder).bindAviso(grupos.get(currentGroup).getAvisos().get(itemPosition));
                return;
            }
            itemsProcessed += itemsInGroup;
            currentGroup++;
        }
    }

    @Override
    public int getItemCount() {
        int count = 0;
        for (GrupoAvisos grupo : grupos) {
            count += 1 + grupo.getAvisos().size(); // 1 header + N items
        }
        return count;
    }

    // ViewHolder para encabezados de fecha
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvFecha;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.cabecera);
        }

        public void bind(String fecha) {
            tvFecha.setText(fecha);

            // Comparar con fecha actual
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date fechaAviso = sdf.parse(fecha);
                Date fechaActual = new Date();

                // Si la fecha del aviso es anterior a hoy
                if (fechaAviso != null && fechaAviso.before(fechaActual)) {
                    tvFecha.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorAccent));
                } else {
                    tvFecha.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorTextPrimary));
                }
            } catch (ParseException e) {
                e.printStackTrace();
                // Mantener color por defecto si hay error en el parseo
                tvFecha.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorPrimaryDark));
            }
        }
    }

    // Método para actualizar la lista de avisos
    public void setAvisos(List<AvisoEntity> avisos) {
        this.grupos = AgrupadorAvisos.groupByFecha(avisos, ordenAscendente);
        notifyDataSetChanged();
    }

    // ViewHolder para el RecyclerView
    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView tvFecha, tvTurno, tvNombre, tvDireccion, tvPoblacion, tvDescripcion, tvUrgente;
        private View barraEstado;
        private AvisoEntity currentAviso;
        private ConstraintLayout layout;
        private Drawable defaultBackground;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            // Vinculamos las vistas con las variables
            barraEstado = itemView.findViewById(R.id.barraEstado);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvTurno = itemView.findViewById(R.id.tvTurno);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvDireccion = itemView.findViewById(R.id.tvDireccion);
            tvPoblacion = itemView.findViewById(R.id.tvPoblacion);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvUrgente = itemView.findViewById(R.id.tvUrgente);

            layout = itemView.findViewById(R.id.relativeLayout);
            defaultBackground = layout.getBackground();
            // Configura el doble click
            itemView.setOnClickListener(new View.OnClickListener() {
                private long lastClickTime = 0;

                @Override
                public void onClick(View v) {
                    long clickTime = System.currentTimeMillis();
                    if (clickTime - lastClickTime < 300) { // Umbral de 300ms para doble click
                        if (doubleClickListener != null && currentAviso != null) {
                            doubleClickListener.onDoubleClick(currentAviso);
                        }
                    }
                    lastClickTime = clickTime;
                }
            });
        }

        // Método para asignar los datos del aviso a las vistas
        @SuppressLint("ResourceAsColor")
        public void bindAviso(AvisoEntity aviso) {
            tvFecha.setText(aviso.getFecha_ejecucion());
            tvNombre.setText(aviso.getNombre_cliente());
            tvDireccion.setText(aviso.getDireccion());
            tvPoblacion.setText(aviso.getPoblacion());
            tvDescripcion.setText(aviso.getDescripcion());

            this.currentAviso = aviso;

            // Cambiamos el color de la barra de estado según el estado del aviso
            switch (aviso.getEstado()) {
                case "PENDIENTE":
                    barraEstado.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.pendiente));
                    break;
                case "PARCIAL":
                    barraEstado.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.parcial));
                    break;
                case "TERMINADO":
                    barraEstado.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.terminado));
                    break;
                case "CERRADO":
                    barraEstado.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.cerrado));
                    break;
            }
            switch (aviso.getTurno()) {
                case "MANANAS":
                    tvTurno.setText(R.string.mananas);
                    tvTurno.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.manianas));
                    break;
                case "TARDES":
                    tvTurno.setText(R.string.tardes);
                    tvTurno.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.tardes));
                    break;
                case "INDIFERENTE":
                    tvTurno.setText(null);
                    tvTurno.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.indiferente));
                    break;
            }
            ConstraintLayout layout = itemView.findViewById(R.id.relativeLayout);

            layout.setBackgroundResource(R.color.recyclerViewItemBackground);
            if(aviso.isUrgente()) {
                tvUrgente.setText(R.string.urgente);
                layout.setBackgroundResource(R.color.urgente);
               // TODO
            }
            else
                tvUrgente.setText(null);
            if(aviso.getFirma() != null )
                layout.setBackgroundResource(R.color.cerrado);
        }
    }

    public void setOrdenAscendente(boolean ordenAscendente) {
        this.ordenAscendente = ordenAscendente;
        // Volver a agrupar y ordenar los avisos
        if (!grupos.isEmpty()) {
            List<AvisoEntity> todosAvisos = new ArrayList<>();
            for (GrupoAvisos grupo : grupos) {
                todosAvisos.addAll(grupo.getAvisos());
            }
            this.grupos = AgrupadorAvisos.groupByFecha(todosAvisos, ordenAscendente);
            notifyDataSetChanged();
        }
    }

    public interface OnDoubleClickListener {
        void onDoubleClick(AvisoEntity aviso);
    }
}

