package com.example.gestoravisos.Login.Data.Repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurePreferences {
    private static SecurePreferences instance;
    private SharedPreferences sharedPreferences;

    private SecurePreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    "auth",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e("SecurePreferences", "Error initializing EncryptedSharedPreferences", e);
        }
    }

    public static synchronized SecurePreferences getInstance(Context context) {
        if (instance == null) {
            instance = new SecurePreferences(context);
        }
        return instance;
    }

    public void saveToken(String token) {
        if (token == null) {
            Log.e("SecurePreferences", "Token es null al intentar guardar");
            return;
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("auth_token", token);
        editor.apply();
        Log.d("SecurePreferences", "Token guardado: " + token); // Verificar el token
    }

    public String getToken() {
        return sharedPreferences.getString("auth_token", null);
    }

    public void clearToken() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("auth_token");
        editor.apply();
        Log.d("SecurePreferences", "Token eliminado"); // Verificar la eliminación
    }

    public void saveUserId(String userId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_id", userId);
        editor.apply();
    }

    public String getUserId() {
        return sharedPreferences.getString("user_id", null);
    }

    public void clearUserId() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("user_id");
        editor.apply();
    }

    public void saveUsername(String username) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.apply();
    }

    public String getUsername() {
        return sharedPreferences.getString("username", null);
    }

    public void clearUsername() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("username");
        editor.apply();
    }

    public void saveCredentials(String username, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putString("password", password); // Se cifra automáticamente
        editor.apply();
    }

    public void savePassword(String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("password", password);
        editor.apply();
    }

    public String getPassword() {
        return sharedPreferences.getString("secure_password", null);
    }

    public void clearCredentials() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("secure_username");
        editor.remove("secure_password");
        editor.apply();
    }

    // Método adicional para solo actualizar la contraseña
    public void updatePassword(String newPassword) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("secure_password", newPassword);
        editor.apply();
    }

    public void saveServerData(String dbName, String ip, String port, String user, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("db_name", dbName);
        editor.putString("db_ip", ip);
        editor.putString("db_port", port);
        editor.putString("db_user", user);
        editor.putString("db_password", password);
        editor.apply();
    }

    public String[] getServerData() {
        return new String[]{
                sharedPreferences.getString("db_name", "bd"),
                sharedPreferences.getString("db_ip", "10.0.2.2"),
                sharedPreferences.getString("db_port", "5000"),
                sharedPreferences.getString("db_user", "usuario"),
                sharedPreferences.getString("db_password", "")
        };
    }



}
