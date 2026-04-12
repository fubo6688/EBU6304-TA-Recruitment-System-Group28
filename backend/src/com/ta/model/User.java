package com.ta.model;

import java.io.Serializable;

/**
 * 用户实体模型。
 *
 * <p>既用于会话态与业务逻辑传递，也用于 users.txt 的文本持久化反序列化。</p>
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
     * 无参构造：供反射与序列化框架使用。
     */
    public User() {}

    /**
     * 全字段构造：创建用户基础实体。
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
     * 获取登录账号 ID。
     */
    public String getUserId() { return userId; }

    /**
     * 设置登录账号 ID。
     */
    public void setUserId(String userId) { this.userId = userId; }

    /**
     * 获取用户显示名称。
     */
    public String getUserName() { return userName; }

    /**
     * 设置用户显示名称。
     */
    public void setUserName(String userName) { this.userName = userName; }

    /**
     * 获取邮箱地址。
     */
    public String getEmail() { return email; }

    /**
     * 设置邮箱地址。
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * 获取登录密码（明文存储，当前实现用于本地课程项目）。
     */
    public String getPassword() { return password; }

    /**
     * 设置登录密码。
     */
    public void setPassword(String password) { this.password = password; }

    /**
     * 获取角色（TA/MO/Admin）。
     */
    public String getRole() { return role; }

    /**
     * 设置角色。
     */
    public void setRole(String role) { this.role = role; }

    /**
     * 获取业务映射 ID（qmId）。
     */
    public String getQmId() { return qmId; }

    /**
     * 设置业务映射 ID（qmId）。
     */
    public void setQmId(String qmId) { this.qmId = qmId; }

    /**
     * 获取账号状态（active/inactive/pending 等）。
     */
    public String getStatus() { return status; }

    /**
     * 设置账号状态。
     */
    public void setStatus(String status) { this.status = status; }

    /**
     * 将用户对象序列化为 users.txt 管道分隔文本。
     */
    @Override
    public String toString() {
        // 文本持久化格式：与 DataManager 的 users.txt 管道分隔协议保持一致。
        return userId + "|" + userName + "|" + email + "|" + password + "|" + role + "|" + qmId + "|" + status;
    }

    /**
     * 从 users.txt 单行文本反序列化用户对象。
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
     * 清理字段中的 BOM 与首尾空白。
     */
    private static String clean(String value) {
        return value == null ? "" : value.replace("\uFEFF", "").trim();
    }
}
