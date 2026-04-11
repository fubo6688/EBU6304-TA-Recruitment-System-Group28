package com.ta.model;

import java.io.Serializable;

public class User implements Serializable {
    private String userId;
    private String userName;
    private String email;
    private String password;
    private String role; // TA, MO, Admin
    private String qmId;
    private String status; // active, inactive

    public User() {}

    public User(String userId, String userName, String email, String password, String role, String qmId) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.qmId = qmId;
        this.status = "active";
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getQmId() { return qmId; }
    public void setQmId(String qmId) { this.qmId = qmId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return userId + "|" + userName + "|" + email + "|" + password + "|" + role + "|" + qmId + "|" + status;
    }

    public static User fromString(String line) {
        String[] parts = line.split("\\|");
        if (parts.length >= 7) {
            User user = new User(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
            user.setStatus(parts[6]);
            return user;
        }
        return null;
    }
}
