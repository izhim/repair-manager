package com.example.gestoravisos.Login.Data.Model;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser {

    private String userId;
    private String displayName;
    private String authToken;

    public LoggedInUser(String userId, String displayName, String authToken) {
        this.userId = userId;
        this.displayName = displayName;
        this.authToken = authToken;
    }


    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAuthToken() { return authToken; }

}