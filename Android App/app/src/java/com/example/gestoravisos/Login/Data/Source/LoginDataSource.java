package com.example.gestoravisos.Login.Data.Source;

import android.content.Context;

import com.example.gestoravisos.Login.Data.Repository.SecurePreferences;
import com.example.gestoravisos.Login.Network.ApiCallback;
import com.example.gestoravisos.Login.Network.ApiClient;
import com.example.gestoravisos.Login.Network.ApiService;
import com.example.gestoravisos.Login.Data.Model.LoginRequest;
import com.example.gestoravisos.Login.Data.Model.LoginResponse;
import com.example.gestoravisos.Login.Data.Model.LoggedInUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/*
    CLASE PARA EL MANEJO DE LA LÓGICA PARA AUTENTICAR AL USUARIO EN EL SERVIDOR,
    GUARDAR SU TOKEN DE AUTENTICACIÓN JWT Y GESTIONARLO DURANTE LA SESIÓN
 */
public class LoginDataSource {
    private ApiService apiService;
    private SecurePreferences securePreferences;
    private Context context;

    public LoginDataSource(Context context) {
        this.context = context;
        this.apiService = ApiClient.getInstance(context).getApiService();
        this.securePreferences = SecurePreferences.getInstance(context);
    }

    public void login(String username, String password, final ApiCallback<LoggedInUser> callback) {
        try {
            this.apiService = ApiClient.getInstance(context).getApiService();
            LoginRequest loginRequest = new LoginRequest(username, password);
            Call<LoginResponse> call = apiService.login(loginRequest);

            if (call == null) {
                callback.onError("La llamada a la API es null");
                return;
            }

            call.enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful()) {
                        LoginResponse loginResponse = response.body();
                        String token = loginResponse.getAccessToken();
                        String userId = loginResponse.getUserId();

                        // Guardar el token en SecurePreferences
                        securePreferences.saveToken(token);

                        // Crear un LoggedInUser con el token y userId
                        LoggedInUser loggedInUser = new LoggedInUser(userId, username, token);

                        // Llamar al callback con el éxito
                        callback.onSuccess(loggedInUser);
                    } else {
                        callback.onError("Error al autenticar usuario: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    callback.onError("Error de conexión: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Error al conectar con el servidor: " + e.getMessage());
        }
    }

    public void logout() {
        // Eliminar el token de SecurePreferences al cerrar sesión
        securePreferences.clearToken();
    }
}