package com.example.gestoravisos.basedatos;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.example.gestoravisos.Clases.UploadResponse;
import com.example.gestoravisos.Login.Network.ApiClient;
import com.example.gestoravisos.Login.Network.ApiService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Call;


public class RepositorioSQLite {

    private List<AvisoEntity> lista;
    private ApiService apiService;
    private int filtro;
    private int criterio;
    private boolean orden;
    private BaseDatos baseDatos;
    private static RepositorioSQLite INSTANCIA;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private MutableLiveData<List<AvisoEntity>> listaAvisosLiveData = new MutableLiveData<>();
    private Context contexto;

    private static final String BASE_URL = "http://10.0.2.2:8080";

    // Constructor
    private RepositorioSQLite(Context contexto){
        this.lista = null;
        this.filtro = 0;
        this.contexto = contexto;
        this.baseDatos = BaseDatos.getInstance(contexto);
        this.apiService = ApiClient.getInstance(contexto).getApiService();
    }

    // Pseudoconstructor
    public static RepositorioSQLite getInstance(Context contexto){
        if(INSTANCIA == null){
            INSTANCIA = new RepositorioSQLite(contexto);
        }
        INSTANCIA.setAvisos();
        return INSTANCIA;
    }

    public LiveData<List<AvisoEntity>> getListaAvisosLiveData() {
        return listaAvisosLiveData;
    }

    public void setAvisos(){
        executorService.execute(this::actualizarLista);
    }

    public void setPrioritarias(int filtro){
        this.filtro = filtro;
        Executors.newSingleThreadExecutor().execute(() -> {
            actualizarLista();
        });
    }

    public void actualizarLista(){
        executorService.execute(() -> {
            lista = baseDatos.avisoDAO().getStaticAvisos();
            Collections.sort(lista, new Comparator<AvisoEntity>() {
                @Override
                public int compare(AvisoEntity aviso1, AvisoEntity aviso2) {
                    // orden numérico
                    if (criterio == 1) {
                        return Integer.compare(aviso1.getOrden(), aviso2.getOrden());
                        // ordenado por nombre de cliente
                    } else if (criterio == 2) {
                        return aviso1.getNombre_cliente().compareTo(aviso2.getNombre_cliente());
                    } else {
                        return 0;
                    }
                }
            });
            if (!orden)
                Collections.reverse(lista);
            listaAvisosLiveData.postValue(filtrada());
        });
    }
    private List<AvisoEntity> filtrada() {
        if (filtro == 0) {
            return new ArrayList<>(lista);
        }
        return lista.stream()
                .filter(aviso -> {
                    String estado = aviso.getEstado();
                    switch (filtro) {
                        case 1:
                            return "PENDIENTE".equals(estado);
                        case 2:
                            return "PARCIAL".equals(estado);
                        case 3:
                            return "TERMINADO".equals(estado);
                        case 4:
                            return "CERRADO".equals(estado);
                        default:
                            return false;
                    }
                })
                .collect(Collectors.toList());
    }

    public void agregarAviso(AvisoEntity aviso){
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                baseDatos.avisoDAO().insertarAviso(aviso);
                actualizarLista();
            }
        });
    }
    public void modificarAviso(AvisoEntity aviso){
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                baseDatos.avisoDAO().modificarAviso(aviso);
                actualizarLista();
            }
        });
    }

    public void eliminarAviso(AvisoEntity aviso){
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                baseDatos.avisoDAO().eliminarAviso(aviso);
            }
        });
    }

    public void eliminarDatos() {
        baseDatos.avisoDAO().eliminarDatos();
    }

    public int getFiltro() {return filtro;}
    public void setFiltro(int filtro) {this.filtro = filtro;}
    public boolean isOrden() {return orden;}
    public void setOrden(boolean orden) {this.orden = orden;}
    public int getCriterio() {return criterio;}

    public void setCriterio(int criterio) {
        this.criterio = criterio;
    }

    public void detenerHilos(){
        if (executorService != null && !executorService.isShutdown()) {
            // detenemos el ExecutorService
            executorService.shutdown();
            try {
                // esperamos un tiempo para que las tareas pendientes finalicen
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    // si quedan tareas pendientes interrumpimos el ExecutorService
                    executorService.shutdownNow();
                    // Esperamos un tiempo adicional para que las tareas interrumpidas finalicen
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        System.err.println("El ExecutorService no se ha cerrado correctamente");
                    }
                }
            } catch (InterruptedException e) {
                // Manejar la interrupción si ocurre
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void sincronizarAvisos(String token) {
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(contexto);

        // 1. Primero subimos los cambios locales al servidor
        subirCambiosLocales(token, () -> {
            // 2. Luego descargamos los avisos actualizados del servidor
            descargarAvisosActualizados(token, () -> {
                // 3. Finalmente eliminamos los datos locales que ya se sincronizaron
                eliminarDatosSincronizados();
                actualizarLista();
                // registrar última sincronización
                long nuevoLastSync = System.currentTimeMillis();
                preferencias.edit().putLong("last_sync_time", nuevoLastSync).apply();

            });
        });
    }

    private void subirCambiosLocales(String token, Runnable onComplete) {
        executorService.execute(() -> {
            try {
                // Obtener avisos locales modificados
                List<AvisoEntity> avisosModificados = baseDatos.avisoDAO().getAvisosModificados();

                for (AvisoEntity avisoLocal : avisosModificados) {
                    // Subir datos principales del aviso
                    subirDatosAviso(avisoLocal, token);

                    // Subir imágenes del instalador si existen
                    if (avisoLocal.getImagenesInst() != null && !avisoLocal.getImagenesInst().isEmpty()) {
                        subirImagenesInstalador(avisoLocal, token);
                    }

                    // Subir firma si existe
                    if (avisoLocal.getFirma() != null) {
                        subirFirma(avisoLocal, token);
                    }

                    // Marcar como sincronizado en local
                    avisoLocal.setSincronizado(true);
                    baseDatos.avisoDAO().modificarAviso(avisoLocal);
                }

                // Ejecutar callback cuando termine
                if (onComplete != null) {
                    new Handler(Looper.getMainLooper()).post(onComplete);
                }
            } catch (Exception e) {
                Log.e("SINCRONIZACION", "Error al subir cambios locales", e);
            }
        });
    }

    private void subirDatosAviso(AvisoEntity aviso, String token) throws IOException {
        Map<String, Object> avisoData = new HashMap<>();
        avisoData.put("estado", aviso.getEstado());
        if (aviso.getNombre_firma() != null) {
            avisoData.put("nombre_firma", aviso.getNombre_firma());
        }
        if (aviso.getDni_firma() != null) {
            avisoData.put("dni_firma", aviso.getDni_firma());
        }
        if (aviso.getObsInstalador() != null) {
            avisoData.put("obsInstalador", aviso.getObsInstalador());
        }

        Response<ResponseBody> response = apiService.updateAviso(
                aviso.getId_aviso(),
                "Bearer " + token,
                avisoData
        ).execute();

        if (!response.isSuccessful()) {
            String errorBody = response.errorBody() != null ?
                    response.errorBody().string() : "Sin detalles del error";
            throw new IOException("Error al actualizar aviso: " + response.code() +
                    " - " + errorBody);
        }
    }

    private void subirImagenesInstalador(AvisoEntity aviso, String token) throws IOException {
        for (Map.Entry<Integer, String> entry : aviso.getImagenesInst().entrySet()) {
            String imagePath = entry.getValue();
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {

                // Crear el cuerpo de la solicitud
                RequestBody requestFile = RequestBody.create(
                        MediaType.parse("image/*"),
                        imageFile
                );

                MultipartBody.Part body = MultipartBody.Part.createFormData(
                        "image",
                        imageFile.getName(),
                        requestFile
                );

                RequestBody clienteId = RequestBody.create(
                        MediaType.parse("text/plain"),
                        String.valueOf(aviso.getId_cliente())
                );
                RequestBody avisoId = RequestBody.create(
                        MediaType.parse("text/plain"),
                        String.valueOf(aviso.getId_aviso())
                );

                RequestBody tipo = RequestBody.create(
                        MediaType.parse("text/plain"),
                        "instalador"
                );

                // Subir la imagen
                Response<UploadResponse> response = apiService.uploadImage(
                        "Bearer " + token,
                        clienteId,
                        avisoId,
                        tipo,
                        body
                ).execute();

                if (response.isSuccessful() && response.body() != null) {
                    // Registrar la imagen en el servidor
                    registrarImagenEnServidor(aviso.getId_aviso(), response.body().getUrl(), token);

                    // Eliminar la imagen local después de subirla
                    imageFile.delete();
                } else {
                    throw new IOException("Error al subir imagen: " + response.code());
                }
            }
        }

        // Limpiar las imágenes instalador después de subirlas
        aviso.getImagenesInst().clear();
        baseDatos.avisoDAO().modificarAviso(aviso);
    }

    private void subirFirma(AvisoEntity aviso, String token) throws IOException {
        // 1. Crear archivo temporal
        File tempFile = File.createTempFile("firma_", ".png", contexto.getCacheDir());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(aviso.getFirma());
        }

        // 2. Preparar la solicitud multipart
        RequestBody requestFile = RequestBody.create(
                MediaType.parse("image/png"),
                tempFile
        );

        MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                "firma",  // Nombre debe coincidir con el del backend
                "firma_" + aviso.getId_aviso() + ".png",
                requestFile
        );

        RequestBody avisoId = RequestBody.create(
                MediaType.parse("text/plain"),
                String.valueOf(aviso.getId_aviso())
        );

        RequestBody nombreFirma = RequestBody.create(
                MediaType.parse("text/plain"),
                aviso.getNombre_firma()
        );

        RequestBody dniFirma = RequestBody.create(
                MediaType.parse("text/plain"),
                aviso.getDni_firma()
        );

        // 3. Configurar Retrofit
        Call<ResponseBody> call = apiService.subirFirma(
                "Bearer " + token,
                avisoId,
                nombreFirma,
                dniFirma,
                filePart
        );

        // 4. Ejecutar la llamada
        Response<ResponseBody> response = call.execute();

        if (!response.isSuccessful()) {
            throw new IOException("Error al subir firma: " + response.code());
        }

        // 5. Limpiar archivo temporal
        tempFile.delete();
    }

    private void registrarImagenEnServidor(int idAviso, String urlImagen, String token) throws IOException {
        Map<String, Object> imagenData = new HashMap<>();
        imagenData.put("id_aviso", idAviso);
        imagenData.put("ruta_imagen", urlImagen);

        Response<Void> response = apiService.registrarImagen(
                "Bearer " + token,
                imagenData
        ).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Error al registrar imagen: " + response.code());
        }
    }

    private void descargarAvisosActualizados(String token, Runnable onComplete) {
        apiService.getAvisos("Bearer " + token).enqueue(new Callback<List<AvisoEntity>>() {
            @Override
            public void onResponse(Call<List<AvisoEntity>> call, Response<List<AvisoEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executorService.execute(() -> {
                        baseDatos.runInTransaction(() -> {
                            List<AvisoEntity> avisosRemotos = response.body();
                            List<Integer> idsRemotos = new ArrayList<>();

                            for (AvisoEntity avisoRemoto : avisosRemotos) {
                                idsRemotos.add(avisoRemoto.getId_aviso());

                                // Descargar y guardar imágenes del cliente si no existen
                                if (avisoRemoto.getImagenes() != null) {
                                    Map<Integer, String> nuevasImagenes = new HashMap<>();

                                    for (Map.Entry<Integer, String> entry : avisoRemoto.getImagenes().entrySet()) {
                                        String rutaLocal = descargarYGuardarImagen(
                                                entry.getValue(),
                                                avisoRemoto.getId_aviso(),
                                                entry.getKey(),
                                                "cliente"
                                        );

                                        if (rutaLocal != null) {
                                            nuevasImagenes.put(entry.getKey(), rutaLocal);
                                        }
                                    }

                                    avisoRemoto.setImagenes(nuevasImagenes);
                                }

                                // Insertar o actualizar el aviso
                                AvisoEntity avisoLocal = baseDatos.avisoDAO().getAvisoById(avisoRemoto.getId_aviso());
                                if (avisoLocal == null) {
                                    baseDatos.avisoDAO().insertarAviso(avisoRemoto);
                                } else {
                                    // Solo actualizar si no tenemos cambios locales pendientes
                                    if (avisoLocal.isSincronizado()) {
                                        baseDatos.avisoDAO().modificarAviso(avisoRemoto);
                                    }
                                }
                            }

                            // Eliminar avisos que ya no están en el servidor
                            List<Integer> idsLocales = baseDatos.avisoDAO().obtenerIdsAvisos();
                            idsLocales.removeAll(idsRemotos);

                            if (!idsLocales.isEmpty()) {
                                baseDatos.avisoDAO().eliminarAvisos(idsLocales);
                            }
                        });

                        // Actualizar UI
                        actualizarLista();

                        // Ejecutar callback cuando termine
                        if (onComplete != null) {
                            new Handler(Looper.getMainLooper()).post(onComplete);
                        }
                    });
                } else {
                    Log.e("SINCRONIZACION", "Error al descargar avisos: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<AvisoEntity>> call, Throwable t) {
                Log.e("SINCRONIZACION", "Error al descargar avisos", t);
            }
        });
    }

    private void eliminarDatosSincronizados() {
        executorService.execute(() -> {
            List<AvisoEntity> todosLosAvisos = baseDatos.avisoDAO().getStaticAvisos();

            // 2. Recorrer cada aviso
            for (AvisoEntity aviso : todosLosAvisos) {
                if(aviso.isSincronizado()) {
                    eliminarImagenesDeAviso(aviso);
                    baseDatos.avisoDAO().eliminarAviso(aviso);
                }
            }
        });
    }

    private void eliminarImagenesDeAviso(AvisoEntity aviso) {
        try {
            // Eliminar imágenes del cliente
            if (aviso.getImagenes() != null && !aviso.getImagenes().isEmpty()) {
                for (String rutaImagen : aviso.getImagenes().values()) {
                    eliminarArchivoSiExiste(rutaImagen);
                }
                // Limpiar el mapa de imágenes
                aviso.getImagenes().clear();
            }

            // Eliminar imágenes del instalador
            if (aviso.getImagenesInst() != null && !aviso.getImagenesInst().isEmpty()) {
                for (String rutaImagen : aviso.getImagenesInst().values()) {
                    eliminarArchivoSiExiste(rutaImagen);
                }
                // Limpiar el mapa de imágenes
                aviso.getImagenesInst().clear();
            }

            // Eliminar firma si existe
            if (aviso.getFirma() != null) {
                aviso.setFirma(null);
            }
        } catch (Exception e) {
            Log.e("ELIMINAR_IMAGENES", "Error al eliminar imágenes del aviso " + aviso.getId_aviso(), e);
        }
    }

    private void eliminarArchivoSiExiste(String rutaArchivo) {
        if (rutaArchivo != null && !rutaArchivo.isEmpty()) {
            File archivo = new File(rutaArchivo);
            if (archivo.exists()) {
                boolean eliminado = archivo.delete();
                if (!eliminado) {
                    Log.w("ELIMINAR_ARCHIVO", "No se pudo eliminar: " + rutaArchivo);
                } else {
                    Log.d("ELIMINAR_ARCHIVO", "Archivo eliminado: " + rutaArchivo);
                }
            }
        }
    }

    public void guardarDatosFirma(int avisoId, String nombreFirma, String dniFirma, Bitmap firmaBitmap, boolean esTerminado) {
        executorService.execute(() -> {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                if (firmaBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    byte[] firmaBytes = stream.toByteArray();
                    stream.close();

                    AvisoEntity aviso = baseDatos.avisoDAO().getAvisoById(avisoId);

                    if (aviso != null) {
                        // Modificar directamente el objeto obtenido
                        aviso.setFirma(firmaBytes);
                        aviso.setNombre_firma(nombreFirma);
                        aviso.setDni_firma(dniFirma);
                        aviso.setEstado(esTerminado ? "TERMINADO" : "PARCIAL");

                        modificarAviso(aviso);
                    }
                }
            } catch (IOException e) {
                Log.e("Repositorio", "Error al procesar firma", e);
            }
        });
    }

    public void guardarObservaciones(int avisoId, String observaciones){
        executorService.execute(() -> {
            AvisoEntity aviso = baseDatos.avisoDAO().getAvisoById(avisoId);

            try {
                    if (aviso != null) {
                    // Modificar directamente el objeto obtenido
                    aviso.setObsInstalador(observaciones);
                    modificarAviso(aviso);
                }
            } catch (Exception e) {
                Log.e("Repositorio", "Error al guardar las observaciones", e);
            }
        });
    }

    // TODO PARTE DE IMAGENES
    private String descargarYGuardarImagen(String urlImagen, int idAviso, int idImagen, String origen) {
        try {
            URL url = new URL(BASE_URL + urlImagen);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e("IMAGEN", "Error al descargar imagen: " + BASE_URL + urlImagen);
                return null;
            }

            // Crear directorio interno de imágenes (ej: /data/data/tu.app.package/files/imagenes/)
            File directorio = new File(contexto.getFilesDir(), "imagenes/" + idAviso + "/" + origen);
            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            // Nombre único con ID y timestamp
            String nombreArchivo = "img_" + idImagen + "_" + System.currentTimeMillis() + ".jpg";
            File archivoImagen = new File(directorio, nombreArchivo);

            InputStream input = connection.getInputStream();
            FileOutputStream output = new FileOutputStream(archivoImagen);

            byte[] buffer = new byte[4096];
            int bytesLeidos;
            while ((bytesLeidos = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesLeidos);
            }

            output.close();
            input.close();
            connection.disconnect();

            return archivoImagen.getAbsolutePath(); // Ruta local
        } catch (IOException e) {
            Log.e("IMAGEN", "Error al guardar imagen: " + urlImagen, e);
            return null;
        }
    }



    public BaseDatos getBaseDatos() {
        return this.baseDatos;
    }

    public ExecutorService getExecutorService() {
        return this.executorService;
    }

}
