package com.example.gestoravisos.Login.Data.Repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.example.gestoravisos.Login.Network.ApiCallback;
import com.example.gestoravisos.Login.Data.Model.LoggedInUser;
import com.example.gestoravisos.Login.Data.Source.LoginDataSource;


/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
public class LoginRepository {
    private static volatile LoginRepository instance;
    private LoginDataSource dataSource;
    private LoggedInUser user = null;
    private SecurePreferences securePreferences;
    private Context context;

    private LoginRepository(LoginDataSource dataSource, Context context) {
        this.dataSource = dataSource;
        this.securePreferences = SecurePreferences.getInstance(context);
        this.context = context;

        // Intentar cargar el usuario desde SecurePreferences al iniciar la app
        String token = securePreferences.getToken();
        String userId = securePreferences.getUserId();
        String username = securePreferences.getUsername();

        if (token != null && userId != null && username != null) {
            this.user = new LoggedInUser(userId, username, token);
        }
    }

    public static LoginRepository getInstance(LoginDataSource dataSource, Context context) {
        if (instance == null) {
            instance = new LoginRepository(dataSource, context);
        }
        return instance;
    }

    public void login(String username, String password, final ApiCallback<LoggedInUser> callback) {
        dataSource.login(username, password, new ApiCallback<LoggedInUser>() {
            @Override
            public void onSuccess(LoggedInUser result) {
                if (result.getAuthToken() == null) {
                    Log.e("LoginRepository", "Token es null en el resultado");
                    callback.onError("Token es null");
                    return;
                }

                // Guardar todos los datos en SecurePreferences
                securePreferences.saveToken(result.getAuthToken());
                securePreferences.saveUserId(result.getUserId());
                securePreferences.saveUsername(result.getDisplayName());
                securePreferences.savePassword(password);
                securePreferences.saveCredentials(username, password);

                SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = preferencias.edit();
                editor.putString("usuario", username);
                editor.putString("password", password);
                editor.apply();


                user = result;
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                Log.e("LoginRepository", "Error en el login: " + error);
                callback.onError(error);
            }
        });
    }

    public void logout() {
        user = null;
        dataSource.logout();

        // Eliminar los datos del usuario de SecurePreferences
        securePreferences.clearToken();
        securePreferences.clearUserId();
        securePreferences.clearUsername();
    }

    private void setLoggedInUser(LoggedInUser user) {
        this.user = user;

        // Guardar datos de sesión en SecurePreferences
        securePreferences.saveToken(user.getAuthToken());
        securePreferences.saveUserId(user.getUserId());
        securePreferences.saveUsername(user.getDisplayName());

    }

    public String getAuthToken() {
        String token = securePreferences.getToken();
        Log.d("LoginRepository", "Token recuperado: " + token); // Verificar el token
        return token;
    }
}