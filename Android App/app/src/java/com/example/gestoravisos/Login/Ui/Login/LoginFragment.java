package com.example.gestoravisos.Login.Ui.Login;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gestoravisos.Activities.Preferencias;
import com.example.gestoravisos.Clases.PollingWorker;
import com.example.gestoravisos.Login.Data.Repository.SecurePreferences;
import com.example.gestoravisos.Login.Data.Source.LoginDataSource;
import com.example.gestoravisos.Login.Network.ApiClient;
import com.example.gestoravisos.R;
import com.example.gestoravisos.databinding.FragmentLoginBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.TimeUnit;


public class LoginFragment extends Fragment {

    private LoginViewModel loginViewModel;
    private FragmentLoginBinding binding;
    private SecurePreferences securePreferences;
    private SharedPreferences sharedPreferences;

    private View dialogView;
    private static final String DIALOG_STATE = "dialog_visible";
    private boolean shouldShowDialog = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentLoginBinding.inflate(inflater, container, false);
        Log.e("ATENCION", getString(R.string.configurar_bd));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // Inicializar SecurePreferences
        securePreferences = SecurePreferences.getInstance(requireContext());


        // Pasamos el contexto
        LoginViewModelFactory factory = new LoginViewModelFactory(requireActivity().getApplication());
        loginViewModel = new ViewModelProvider(this, factory).get(LoginViewModel.class);

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final Button loginButton = binding.login;
        final ProgressBar loadingProgressBar = binding.loading;

        // Cargar el nombre de usuario guardado
        loadSavedUsername(usernameEditText);
        loadSavedPassword(passwordEditText);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Observar cambios en el estado del formulario
        loginViewModel.getLoginFormState().observe(getViewLifecycleOwner(), new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
                loginButton.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null) {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null) {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });

        // Observar cambios en el resultado del login
        loginViewModel.getLoginResult().observe(getViewLifecycleOwner(), new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
                loadingProgressBar.setVisibility(View.GONE);
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    updateUiWithUser(loginResult.getSuccess());
                }
            }
        });

        // Escuchar cambios en los campos de texto
        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Ignorar
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Ignorar
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };

        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);

        // Configurar el botón de inicio de sesión
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    loginViewModel.login(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString(),getContext());
                }
                return false;
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                loginViewModel.login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString(), getContext());
            }
        });

        // Configurar el enlace de configuración de BD
        binding.tvConfigDb.setOnClickListener(v -> {
            showConfirmationDialog();
        });
    }

    private void loadSavedUsername(EditText usernameEditText) {
        // Obtener el nombre de usuario almacenado usando SecurePreferences
        String username = securePreferences.getUsername();

        // Si hay un nombre de usuario almacenado, cargarlo en el campo correspondiente
        if (username != null) {
            usernameEditText.setText(username);
        }
    }

    private void loadSavedPassword(EditText passwordEditText) {
        String password = securePreferences.getPassword();
        if (password != null) {
            passwordEditText.setText(password);
        }
    }



    private void showConfirmationDialog() {
        // 1. Usar MaterialAlertDialogBuilder para mejor apariencia
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(),
                R.style.CustomAlertDialogTheme);

        builder.setTitle(R.string.configurar_bd)
                .setMessage(R.string.confirmar_edicion_bd)
                .setIcon(R.drawable.ic_warning)

                // 3. Personalizar botones
                .setPositiveButton(R.string.editar, (dialog, which) -> {
                    showDatabaseConfigDialog();
                })
                .setNegativeButton(R.string.cancelar, null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // 5. Cambiar color del texto y botones
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));

            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
        });

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background);
        }
    }

    public void showDatabaseConfigDialog() {
        dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_config_bd, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(),
                R.style.CustomAlertDialogTheme);

        // Referencias a los EditText
        EditText etDbIp = dialogView.findViewById(R.id.et_db_ip);
        EditText etDbPort = dialogView.findViewById(R.id.et_db_port);

        // Establecer valores actuales desde SharedPreferences
        etDbIp.setText(sharedPreferences.getString("db_ip", "10.0.2.2"));
        etDbPort.setText(sharedPreferences.getString("db_port", "5000"));

        AlertDialog currentDialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.configurar_bd)
                .setView(dialogView)
                .setPositiveButton(R.string.guardar, (dialog, which) -> {
                    // Guardar en SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("db_ip", etDbIp.getText().toString());
                    editor.putString("db_port", etDbPort.getText().toString());
                    editor.apply();

                    // Reiniciar ApiClient con nueva configuración
                    ApiClient.recreateInstance(getContext());

                    // Forzar cierre de sesión
                    LoginDataSource loginDataSource = new LoginDataSource(getContext());
                    loginDataSource.logout();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        currentDialog.show();
    }


    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        // Mostrar mensaje de bienvenida
        if (getContext() != null && getContext().getApplicationContext() != null) {
            Toast.makeText(getContext().getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
        }

        // Guardar las credenciales usando SecurePreferences
        securePreferences.saveUsername(model.getDisplayName());

        configurarPolling();
        // Navegación al siguiente fragmento
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.listaTrabajosFragment);
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        if (getContext() != null && getContext().getApplicationContext() != null) {
            Toast.makeText(
                    getContext().getApplicationContext(),
                    errorString,
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Mostrar diálogo si estaba visible antes de rotación
        if (shouldShowDialog) {
            shouldShowDialog = false;
            showDatabaseConfigDialog();
        }
        if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) requireActivity()).getSupportActionBar().hide();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) requireActivity()).getSupportActionBar().show();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void configurarPolling() {
        // Definir las restricciones (opcional)
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Crear la solicitud de trabajo único (para pruebas)
        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(PollingWorker.class)
                        .setConstraints(constraints)
                        .setInitialDelay(1, TimeUnit.SECONDS) // puedes poner 0 para que sea inmediato
                        .build();

        // Encolar el trabajo único
        WorkManager.getInstance(getContext()).enqueueUniqueWork(
                "polling_work",
                ExistingWorkPolicy.REPLACE, // Reemplaza si ya existe
                workRequest
        );
    }


}