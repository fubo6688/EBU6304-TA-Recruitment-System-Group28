package com.bupt.model;

public abstract class User {
    private String userId;
    private String username;
    private String password;
    private String email;

    // 构造函数
    protected User(String userId, String username, String password, String email) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }

    public boolean login(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }

    public void logout() {
        // 这里可以添加一些清理用户会话的逻辑
    }

    public String modifyPassword(String newPassword) {
        this.password = newPassword;
        return "Password updated successfully.";
    }

    public String saveToText() {
        // 将用户数据拼接为 CSV 格式的一行
        return userId + "," + username + "," + password + "," + email;
    }

    public String loadFromText(String text) {
        // 从 CSV 格式的一行解析用户数据
        String[] parts = text.split(",");       //此处代码为自动生成，需要根据实际情况进行调整
        if (parts.length == 4) {
            this.userId = parts[0];
            this.username = parts[1];
            this.password = parts[2];
            this.email = parts[3];
            return "User loaded successfully.";
        } else {
            return "Invalid user data format.";
        }
    }

}
