package com.example.gestoravisos.Login.Ui.Login;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;

import com.example.gestoravisos.Login.Data.Source.LoginDataSource;
import com.example.gestoravisos.Login.Network.ApiCallback;
import com.example.gestoravisos.Login.Data.Repository.LoginRepository;
import com.example.gestoravisos.Login.Data.Model.LoggedInUser;
import com.example.gestoravisos.R;
import com.example.gestoravisos.ViewModels.AvisoViewModel;
import com.example.gestoravisos.basedatos.BaseDatos;
import com.example.gestoravisos.basedatos.RepositorioSQLite;

import java.util.concurrent.Executors;

public class LoginViewModel extends ViewModel {

    private MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private LoginRepository loginRepository;

    public LoginViewModel(LoginRepository loginRepository) {

        this.loginRepository = loginRepository;
    }



    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    // Modificado para usar un callback
    public void login(String username, String password, Context contexto) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(contexto);
        String savedUser = sharedPreferences.getString("usuario", null);
        String savedPass = sharedPreferences.getString("password", null);

        // 1. Si hay credenciales guardadas
        if (savedUser != null && savedPass != null) {
            if (savedUser.equals(username) && savedPass.equals(password)) {
                // Coinciden con las almacenadas
                loginResult.setValue(new LoginResult(new LoggedInUserView(username)));
                return;
            }
        }

        // 2. Si no hay credenciales guardadas o no coinciden, hacer login al servidor
        loginRepository.login(username, password, new ApiCallback<LoggedInUser>() {
            @Override
            public void onSuccess(LoggedInUser result) {
                loginResult.setValue(new LoginResult(new LoggedInUserView(result.getDisplayName())));

                // Guardar las credenciales en SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("usuario", username);
                editor.putString("password", password);

                editor.apply();

                // Si el contexto permite acceso al ViewModel
                if (contexto instanceof ViewModelStoreOwner) {
                    onLoginSuccess(result, (ViewModelStoreOwner) contexto, contexto);
                    AvisoViewModel avisoViewModel = new ViewModelProvider((ViewModelStoreOwner) contexto).get(AvisoViewModel.class);
                    BaseDatos baseDatos = BaseDatos.getInstance(contexto);
                    Executors.newSingleThreadExecutor().execute(() -> {
                        // Eliminar datos en background
                        baseDatos.avisoDAO().eliminarDatos();

                        // Luego sincronizar en el hilo principal (UI thread)
                        new Handler(Looper.getMainLooper()).post(() -> {
                            avisoViewModel.sincronizarAvisos(contexto);
                        });
                    });
                } else {
                    Log.e("LoginViewModel", "El contexto no implementa ViewModelStoreOwner");
                }
            }

            @Override
            public void onError(String error) {
                loginResult.setValue(new LoginResult(R.string.login_failed));
            }
        });
    }

    public void loginDataChanged(String username, String password) {
        if (!isUserNameValid(username)) {
            loginFormState.setValue(new LoginFormState(R.string.invalid_username, null));
        } else if (!isPasswordValid(password)) {
            loginFormState.setValue(new LoginFormState(null, R.string.invalid_password));
        } else {
            loginFormState.setValue(new LoginFormState(true));
        }
    }

    public void logout() {
        loginRepository.logout();
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username) {
        if (username == null) {
            return false;
        }
        if (username.contains("@")) {
            return Patterns.EMAIL_ADDRESS.matcher(username).matches();
        } else {
            return !username.trim().isEmpty();
        }
    }

    private boolean isPasswordValid(String password) {
        return password != null && password.trim().length() > 5;
    }

    public void onLoginSuccess(LoggedInUser user, ViewModelStoreOwner owner, Context context) {
        AvisoViewModel avisoViewModel = new ViewModelProvider(owner).get(AvisoViewModel.class);
        avisoViewModel.sincronizarAvisos(context);
    }
}