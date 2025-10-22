package com.example.gestoravisos.Login.Network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.example.gestoravisos.Login.Data.Model.LoginRequest;
import com.example.gestoravisos.Login.Data.Model.LoginResponse;

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Modelos de datos
class Cliente {
    int id_cliente;
    String nombre;
    String direccion;
    String poblacion;
    String telefono;
    String observaciones;
}



/*
     CLASE RESPONSABLE DE GESTIONAR LAS SOLICITUDES HTTP USANDO RETROFIT Y OKHTTP
     PERMITE OBTENER DATOS DE CLIENTES Y AVISOS Y REALIZAR LA AUTENTICACIÓN
 */
public class ApiClient {
    private String base_url;
    private static ApiClient instance;
    private ApiService apiService;
    private Context context;

    private ApiClient(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String ip = prefs.getString("db_ip", "10.0.2.2"); // Valor por defecto para emulador
        String puerto = prefs.getString("db_port", "5000");

        base_url = "http://" + ip + ":" + puerto + "/";
        // Agregar interceptor para logs HTTP
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(base_url)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static ApiClient getInstance(Context context) {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) {
                    instance = new ApiClient(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }

    // Obtener lista de clientes
    public void obtenerClientes(String token, final ApiCallback<List<Cliente>> callback) {
        apiService.getClientes("Bearer " + token).enqueue(new Callback<List<Cliente>>() {
            @Override
            public void onResponse(Call<List<Cliente>> call, Response<List<Cliente>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Cuerpo vacío";
                        Log.e("ApiClient", "Error en obtenerClientes - Código: " + response.code() + " - Cuerpo: " + errorBody);
                        callback.onError("Error en la respuesta: " + response.code() + " - " + errorBody);
                    } catch (IOException e) {
                        Log.e("ApiClient", "Error al leer el cuerpo del error", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Cliente>> call, Throwable t) {
                Log.e("ApiClient", "Error de conexión en obtenerClientes: " + t.getMessage(), t);
                callback.onError("Error de conexión: " + t.getMessage());
            }
        });
    }

    // Obtener lista de avisos
    public void obtenerAvisos(String token, final ApiCallback<List<com.example.gestoravisos.basedatos.AvisoEntity>> callback) {
        apiService.getAvisos("Bearer " + token).enqueue(new Callback<List<com.example.gestoravisos.basedatos.AvisoEntity>>() {
            @Override
            public void onResponse(Call<List<com.example.gestoravisos.basedatos.AvisoEntity>> call, Response<List<com.example.gestoravisos.basedatos.AvisoEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Cuerpo vacío";
                        Log.e("ApiClient", "Error en obtenerAvisos - Código: " + response.code() + " - Cuerpo: " + errorBody);
                        callback.onError("Error en la respuesta: " + response.code() + " - " + errorBody);
                    } catch (IOException e) {
                        Log.e("ApiClient", "Error al leer el cuerpo del error", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<com.example.gestoravisos.basedatos.AvisoEntity>> call, Throwable t) {
                Log.e("ApiClient", "Error de conexión en obtenerAvisos: " + t.getMessage(), t);
                callback.onError("Error de conexión: " + t.getMessage());
            }
        });
    }

    // Login
    public void login(String username, String password, final ApiCallback<LoginResponse> callback) {
        LoginRequest request = new LoginRequest(username, password);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Cuerpo vacío";
                        Log.e("ApiClient", "Error en login - Código: " + response.code() + " - Cuerpo: " + errorBody);
                        callback.onError("Error en la respuesta: " + response.code() + " - " + errorBody);
                    } catch (IOException e) {
                        Log.e("ApiClient", "Error al leer el cuerpo del error", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e("ApiClient", "Error de conexión en login: " + t.getMessage(), t);
                callback.onError("Error de conexión: " + t.getMessage());
            }
        });
    }

    public static void recreateInstance(Context context) {
        synchronized (ApiClient.class) {
            instance = null; // Elimina la instancia existente
            getInstance(context); // Crea una nueva
        }
    }

}