package com.example.gestoravisos.basedatos;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

// TODO import com.example.TrassTarea.modelo.Tarea;

import com.example.gestoravisos.Clases.MapConverter;
import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


@Entity(tableName = "avisoentity")
@TypeConverters(MapConverter.class) // Se aplica el TypeConverter para los maps
public class AvisoEntity implements Parcelable {

    // Clave primaria (id_aviso proviene del backend)
    /*
    @PrimaryKey
    @NonNull
    @SerializedName("id")
    private int id;
*/
    @PrimaryKey
    @NonNull
    @SerializedName("id_aviso")
    private int id_aviso;

    @SerializedName("id_cliente")
    private int id_cliente;

    @SerializedName("id_instalador")
    private int id_instalador;

    @ColumnInfo(name = "sincronizado", defaultValue = "0")
    private boolean sincronizado;

    @SerializedName("direccion")
    private String direccion;

    @SerializedName("poblacion")
    private String poblacion;

    @SerializedName("descripcion")
    private String descripcion;

    @SerializedName("urgente")
    private boolean urgente;

    @SerializedName("fecha_aviso")
    private String fecha_aviso;

    @SerializedName("fecha_ejecucion")
    private String fecha_ejecucion;

    @SerializedName("estado")
    private String estado;

    @SerializedName("turno")
    private String turno;
    @SerializedName("nombre")
    private String nombre_cliente;

    @SerializedName("telefono")
    private String telefono;

    @SerializedName("observaciones")
    private String observaciones;
    @SerializedName("obsInstalador")
    private String obsInstalador;

    private int orden;

    @Nullable
    private byte[] firma;

    @Nullable
    private String uriVid;

    @Nullable
    private String nombre_firma;

    @Nullable
    private String dni_firma;

    @Nullable
    private Long hora_inicio;

    @Nullable
    private Long hora_fin;

    @Nullable
    private Map<Integer, String> ayudantes; // no es necesario en la aplicación Android

    @Nullable
    @SerializedName("imagenes")
    @TypeConverters(MapConverter.class)
    private Map<Integer, String> imagenes;


    @Nullable
    @SerializedName("imagenesInst")
    @TypeConverters(MapConverter.class)
    private Map<Integer, String> imagenesInst;

    /* Constructor principal */
    public AvisoEntity(int id_aviso, int id_cliente, int id_instalador, String direccion, String poblacion, String descripcion,
                       boolean urgente, String fecha_aviso,String fecha_ejecucion, String estado, String turno) {
        this.id_aviso = id_aviso;
        this.id_cliente = id_cliente;
        this.id_instalador = id_instalador;
        this.direccion = direccion;
        this.poblacion = poblacion;
        this.descripcion = descripcion;
        this.urgente = urgente;
        this.fecha_aviso = conversorFecha(fecha_aviso);
        this.fecha_ejecucion = conversorFecha(fecha_ejecucion);
        this.estado = estado;
        this.turno = turno;
        this.ayudantes = new HashMap<>();
        this.imagenes = new HashMap<>();
        this.imagenesInst = new HashMap<>();
    }

    /* Getters y Setters */
    /*
    public int getId() {
        return id;
    }
    public void setId(int id){
        this.id = id;
    }
    */

    public int getId_aviso() {
        return id_aviso;
    }

    public void setId_aviso(int id_aviso) {
        this.id_aviso = id_aviso;
    }

    public int getId_cliente() {
        return id_cliente;
    }

    public void setId_cliente(int id_cliente) {
        this.id_cliente = id_cliente;
    }

    public int getId_instalador() {
        return id_instalador;
    }

    public void setId_instalador(int id_instalador) {
        this.id_instalador = id_instalador;
    }

    public boolean isSincronizado() {
        return sincronizado;
    }

    public void setSincronizado(boolean sincronizado) {
        this.sincronizado = sincronizado;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isUrgente() {
        return urgente;
    }

    public void setUrgente(boolean urgente) {
        this.urgente = urgente;
    }

    public String getFecha_aviso() {
        return fecha_aviso;
    }

    public void setFecha_aviso(String fecha_aviso) {
        this.fecha_aviso = fecha_aviso;
    }


    public String getFecha_ejecucion() {
        return fecha_ejecucion;
    }

    public void setFecha_ejecucion(String fecha_ejecucion) {
        this.fecha_ejecucion = fecha_ejecucion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    @Nullable
    public byte[] getFirma() {
        return firma;
    }

    public void setFirma(@Nullable byte[] firma) {
        this.firma = firma;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    @Nullable
    public String getNombre_cliente() {
        return nombre_cliente;
    }

    public void setNombre_cliente(@Nullable String nombre_cliente) {
        this.nombre_cliente = nombre_cliente;
    }

    @Nullable
    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(@Nullable String telefono) {
        this.telefono = telefono;
    }

    @Nullable
    public String getPoblacion() {
        return poblacion;
    }

    public void setPoblacion(@Nullable String poblacion) {
        this.poblacion = poblacion;
    }

    @Nullable
    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(@Nullable String observaciones) {
        this.observaciones = observaciones;
    }

    @Nullable
    public String getObsInstalador() {
        return obsInstalador;
    }

    public void setObsInstalador(@Nullable String obsInstalador) {
        this.obsInstalador = obsInstalador;
    }

    @Nullable
    public String getUriVid() {
        return uriVid;
    }

    public void setUriVid(@Nullable String uriVid) {
        this.uriVid = uriVid;
    }

    @Nullable
    public String getNombre_firma() {
        return nombre_firma;
    }

    public void setNombre_firma(@Nullable String nombre_firma) {
        this.nombre_firma = nombre_firma;
    }

    @Nullable
    public String getDni_firma() {
        return dni_firma;
    }

    public void setDni_firma(@Nullable String dni_firma) {
        this.dni_firma = dni_firma;
    }

    @Nullable
    public Long getHora_inicio() {
        return hora_inicio;
    }

    public void setHora_inicio(@Nullable Long hora_inicio) {
        this.hora_inicio = hora_inicio;
    }

    @Nullable
    public Long getHora_fin() {
        return hora_fin;
    }

    public void setHora_fin(@Nullable Long hora_fin) {
        this.hora_fin = hora_fin;
    }

    @Nullable
    public Map<Integer, String> getAyudantes() {
        return ayudantes;
    }

    public void setAyudantes(@Nullable Map<Integer, String> ayudantes) {
        this.ayudantes = ayudantes;
    }

    @Nullable
    public Map<Integer, String> getImagenes() {
        return imagenes;
    }

    public void setImagenes(@Nullable Map<Integer, String> imagenes) {
        this.imagenes = imagenes;
    }

    @Nullable
    public Map<Integer, String> getImagenesInst() {
        return imagenesInst;
    }

    public void setImagenesInst(@Nullable Map<Integer, String> imagenesInst) {
        this.imagenesInst = imagenesInst;
    }

    public long getFecha(){
        try {
            SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fecha = formato.parse(fecha_ejecucion);
            return fecha.getTime(); // milisegundos desde el epoch
        } catch (Exception e) {
            e.printStackTrace();
            return -1; // o lanza una excepción si prefieres
        }
    }

    /* Parcelable */
    @Override
    public int describeContents() {
        return 0;
    }

    protected AvisoEntity(Parcel in) {
        id_aviso = in.readInt();
        id_cliente = in.readInt();
        id_instalador = in.readInt();
        direccion = in.readString();
        descripcion = in.readString();
        urgente = in.readByte() != 0;
        fecha_aviso = in.readString();
        fecha_ejecucion = in.readString();
        estado = in.readString();
        firma = in.createByteArray();
        orden = in.readInt();
        nombre_cliente = in.readString();
        telefono = in.readString();
        poblacion = in.readString();
        observaciones = in.readString();
        uriVid = in.readString();
        nombre_firma = in.readString();
        dni_firma = in.readString();
        hora_inicio = in.readLong();
        hora_fin = in.readLong();
        ayudantes = new HashMap<>();
        in.readMap(ayudantes, String.class.getClassLoader());
        imagenes = new HashMap<>();
        in.readMap(imagenes, String.class.getClassLoader());
        imagenesInst = new HashMap<>();
        in.readMap(imagenesInst, String.class.getClassLoader());
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id_aviso);
        dest.writeInt(id_cliente);
        dest.writeInt(id_instalador);
        dest.writeString(direccion);
        dest.writeString(descripcion);
        dest.writeByte((byte) (urgente ? 1 : 0));
        dest.writeString(fecha_aviso);
        dest.writeString(fecha_ejecucion);
        dest.writeString(estado);
        dest.writeByteArray(firma);
        dest.writeInt(orden);
        dest.writeString(nombre_cliente);
        dest.writeString(telefono);
        dest.writeString(poblacion);
        dest.writeString(observaciones);
        dest.writeString(uriVid);
        dest.writeString(nombre_firma);
        dest.writeString(dni_firma);
        dest.writeLong(hora_inicio != null ? hora_inicio : -1);
        dest.writeLong(hora_fin != null ? hora_fin : -1);
        dest.writeMap(ayudantes);
        dest.writeMap(imagenes);
    }

    public static final Creator<AvisoEntity> CREATOR = new Creator<AvisoEntity>() {
        @Override
        public AvisoEntity createFromParcel(Parcel in) {
            return new AvisoEntity(in);
        }

        @Override
        public AvisoEntity[] newArray(int size) {
            return new AvisoEntity[size];
        }
    };

    /* equals y hashCode */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AvisoEntity)) return false;
        AvisoEntity that = (AvisoEntity) o;
        return id_aviso == that.id_aviso;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id_aviso);
    }

    private String conversorFecha(String timestamp) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (timestamp.contains("/")) {
                // Ya está en formato dd/MM/yyyy
                return timestamp;
            }
            DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDateTime dateTime = LocalDateTime.parse(timestamp, inputFormatter);
            return dateTime.format(outputFormatter);
        } else {
            return timestamp;
        }
    }
}