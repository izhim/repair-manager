package com.example.gestoravisos.Login.Data.Model;

public class LoginResponse {
    private String access_token;
    private String userId;  // O cualquier otra información que necesites del usuario

    public String getAccessToken() {
        return access_token;
    }

    public String getUserId() {
        return userId;
    }
}
