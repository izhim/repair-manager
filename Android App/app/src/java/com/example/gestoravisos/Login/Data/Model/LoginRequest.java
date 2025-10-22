package com.example.gestoravisos.Login.Data.Model;

import java.util.Arrays;

public class LoginRequest {
    private String nombre;
    private String password;

    // Constructor, getters y setters
    public LoginRequest(String nombre, String password) {
        this.nombre = nombre;
        this.password = password;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPassword() {
        return password;
    }

    public void clearPassword() {
        if (password != null) {
            Arrays.fill(password.toCharArray(), '\0'); // Sobrescribe la contraseña en memoria
        }
    }
}
