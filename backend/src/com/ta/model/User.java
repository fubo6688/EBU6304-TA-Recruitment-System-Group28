package com.ta.model;

import java.io.Serializable;

public class User implements Serializable {
    // 用户唯一账号（登录主键）。
    private String userId;
    // 用户展示名称。
    private String userName;
    private String email;
    private String password;
    private String role; // TA, MO, Admin
    // 业务关联 ID（常用于学号/工号或负责人映射）。
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
        // 文本持久化格式：与 DataManager 的 users.txt 管道分隔协议保持一致。
        return userId + "|" + userName + "|" + email + "|" + password + "|" + role + "|" + qmId + "|" + status;
    }

    public static User fromString(String line) {
        String[] parts = line.split("\\|");
        if (parts.length >= 7) {
            // 反序列化时补回状态字段，兼容文件读写。
            User user = new User(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
            user.setStatus(parts[6]);
            return user;
        }
        return null;
    }
}
