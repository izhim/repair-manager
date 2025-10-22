package com.example.gestoravisos.Login.Network;

import com.example.gestoravisos.Clases.UploadResponse;
import com.example.gestoravisos.Login.Data.Model.LoginRequest;
import com.example.gestoravisos.Login.Data.Model.LoginResponse;
import com.example.gestoravisos.basedatos.AvisoEntity;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

// Interfaz para definir los endpoints
public interface ApiService {

    @GET("clientes")
    Call<List<Cliente>> getClientes(@Header("Authorization") String token);
    @GET("avisos-instalador")
    Call<List<AvisoEntity>> getAvisos(@Header("Authorization") String token);

    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @Multipart
    @POST("upload")
    Call<UploadResponse> uploadImage(
            @Header("Authorization") String authHeader,
            @Part("cliente_id") RequestBody clienteId,
            @Part("aviso_id") RequestBody avisoId,
            @Part("tipo") RequestBody tipo,
            @Part MultipartBody.Part image
    );

    @POST("imagenes")
    Call<Void> registrarImagen(
            @Header("Authorization") String authHeader,
            @Body Map<String, Object> imagenData
    );

    @PUT("avisos/{id_aviso}")
    Call<ResponseBody> updateAviso(
            @Path("id_aviso") int idAviso,
            @Header("Authorization") String authHeader,
            @Body Map<String, Object> avisoData
    );

    @GET("avisos-instalador/modificados")
    Call<List<AvisoEntity>> getAvisosModificados(
            @Header("Authorization") String authHeader
    );

    @Multipart
    @POST("subir-firma")
    Call<ResponseBody> subirFirma(
            @Header("Authorization") String authHeader,
            @Part("avisoId") RequestBody avisoId,
            @Part("nombreFirma") RequestBody nombreFirma,
            @Part("dniFirma") RequestBody dniFirma,
            @Part MultipartBody.Part firma
    );

    @GET("avisos/check-updates")
    Call<List<AvisoEntity>> checkForUpdates(
            @Header("Authorization") String authToken,
            @Query("last_sync") long lastSyncTime
    );


}
