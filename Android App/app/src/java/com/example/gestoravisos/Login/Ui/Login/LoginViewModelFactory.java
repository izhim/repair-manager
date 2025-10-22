package com.example.gestoravisos.Login.Ui.Login;

import android.content.Context;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.example.gestoravisos.Login.Data.Source.LoginDataSource;
import com.example.gestoravisos.Login.Data.Repository.LoginRepository;

/**
 * ViewModel provider factory to instantiate LoginViewModel.
 * Required given LoginViewModel has a non-empty constructor
 */
public class LoginViewModelFactory implements ViewModelProvider.Factory {

    private Context context;

    // Constructor que recibe el contexto de la actividad o aplicación
    public LoginViewModelFactory(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            // Crear LoginDataSource pasando el contexto
            LoginDataSource dataSource = new LoginDataSource(context);

            // Crear LoginRepository pasando el LoginDataSource y el contexto
            LoginRepository repository = LoginRepository.getInstance(dataSource, context);

            // Devolver el LoginViewModel con el LoginRepository
            return (T) new LoginViewModel(repository);
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}