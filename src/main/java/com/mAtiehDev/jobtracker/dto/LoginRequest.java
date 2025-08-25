package com.mAtiehDev.jobtracker.dto;

public class LoginRequest {
    private String emailOrUsername;
    private String password;

    // Getters and setters
    public String getEmailOrUsername() {
        return emailOrUsername;
    }
    public void setEmailOrUsername(String emailOrUsername) {
        this.emailOrUsername = emailOrUsername;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
