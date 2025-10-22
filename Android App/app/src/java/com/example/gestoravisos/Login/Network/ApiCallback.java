package com.example.gestoravisos.Login.Network;

// Interfaz de callback para manejar respuestas
public interface ApiCallback<T> {
    void onSuccess(T result);
    void onError(String error);
}
