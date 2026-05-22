package com.ta.model;

import java.io.Serializable;

/**
 * User domain model.
 *
 * <p>Represents a user account used across the application for session
 * handling and business logic. This model is also serialized/deserialized
 * to/from the pipe-separated `users.txt` storage format.</p>
 */
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

    /**
     * No-arg constructor used by frameworks and deserialization.
     */
    public User() {}

    /**
     * Full constructor to create a User instance.
     *
     * @param userId  unique login identifier
     * @param userName display name
     * @param email email address
     * @param password password (stored in current implementation as plain text)
     * @param role user role (TA, MO, Admin)
     * @param qmId business mapping id (e.g. student/staff number)
     */
    public User(String userId, String userName, String email, String password, String role, String qmId) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.qmId = qmId;
        this.status = "active";
    }

    /**
     * Returns the login account id.
     *
     * @return userId
     */
    public String getUserId() { return userId; }

    /**
     * Sets the login account id.
     *
     * @param userId login id
     */
    public void setUserId(String userId) { this.userId = userId; }

    /**
     * Returns the display name.
     *
     * @return userName
     */
    public String getUserName() { return userName; }

    /**
     * Sets the display name.
     *
     * @param userName display name
     */
    public void setUserName(String userName) { this.userName = userName; }

    /**
     * Returns the email address.
     *
     * @return email
     */
    public String getEmail() { return email; }

    /**
     * Sets the email address.
     *
     * @param email email address
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Returns the password.
     *
     * <p>Note: in this project passwords are stored in plain text for
     * simplicity; do not use this approach in production.</p>
     *
     * @return password
     */
    public String getPassword() { return password; }

    /**
     * Sets the password.
     *
     * @param password plaintext password
     */
    public void setPassword(String password) { this.password = password; }

    /**
     * Returns the role (TA / MO / Admin).
     *
     * @return role
     */
    public String getRole() { return role; }

    /**
     * Sets the role.
     *
     * @param role role string
     */
    public void setRole(String role) { this.role = role; }

    /**
     * Returns the business mapping id (qmId).
     *
     * @return qmId
     */
    public String getQmId() { return qmId; }

    /**
     * Sets the business mapping id (qmId).
     *
     * @param qmId mapping id
     */
    public void setQmId(String qmId) { this.qmId = qmId; }

    /**
     * Returns the account status (e.g. active, inactive, pending).
     *
     * @return status
     */
    public String getStatus() { return status; }

    /**
     * Sets the account status.
     *
     * @param status new status
     */
    public void setStatus(String status) { this.status = status; }

    /**
     * Serializes the user object into the pipe-separated line format used by users.txt.
     *
     * @return pipe-separated representation
     */
    @Override
    public String toString() {
        // 文本持久化格式：与 DataManager 的 users.txt 管道分隔协议保持一致。
        return userId + "|" + userName + "|" + email + "|" + password + "|" + role + "|" + qmId + "|" + status;
    }

    /**
     * Deserializes a User from a single pipe-separated line as stored in users.txt.
     *
     * @param line input line
     * @return User instance or null if parsing fails
     */
    public static User fromString(String line) {
        // 兼容 UTF-8 BOM 污染，避免账号字段首字符异常导致查找失败。
        String normalized = line == null ? "" : line.replace("\uFEFF", "");
        String[] parts = normalized.split("\\|", -1);
        if (parts.length >= 7) {
            // 反序列化时补回状态字段，兼容文件读写。
            User user = new User(clean(parts[0]), clean(parts[1]), clean(parts[2]), parts[3], clean(parts[4]), clean(parts[5]));
            user.setStatus(clean(parts[6]));
            return user;
        }
        return null;
    }

    /**
     * Cleans a field value by removing BOM and trimming whitespace.
     */
    private static String clean(String value) {
        return value == null ? "" : value.replace("\uFEFF", "").trim();
    }
}
