package com.example.gestoravisos.Clases;

public class UploadResponse {
    private String url;

    // Constructor vacío necesario para la deserialización
    public UploadResponse() {
    }

    public UploadResponse(String url) {
        this.url = url;
    }

    // Getter y Setter
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
