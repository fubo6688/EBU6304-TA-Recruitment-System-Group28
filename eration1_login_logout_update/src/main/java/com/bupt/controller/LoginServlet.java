package com.bupt.controller;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    // 用于存储用户失败次数和锁定状态的 Map
    private static final String LOCK_MAP_KEY = "userLockMap";

    // 锁定时长（毫秒）：5 分钟
    private static final long LOCK_DURATION_MS = 5 * 60 * 1000;
    private static final int MAX_ATTEMPTS = 3;

    @Override
    public void init() throws ServletException {
        // 初始化上下文中的锁定 Map
        getServletContext().setAttribute(LOCK_MAP_KEY, new HashMap<String, LoginAttempt>());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 1. 获取参数
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // --- 需求 1：拒绝空用户名/密码 ---
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            response.sendRedirect("login.jsp?error=empty");
            return;
        }

        username = username.trim();
        password = password.trim();

        // --- 需求 3：检查账户是否被锁定 (包含自动解锁逻辑) ---
        Map<String, LoginAttempt> lockMap = (Map<String, LoginAttempt>) getServletContext().getAttribute(LOCK_MAP_KEY);
        LoginAttempt attempt = lockMap.get(username);

        // 如果用户有记录且处于锁定状态
        if (attempt != null && attempt.isLocked()) {
            // 【核心修复】检查锁定时间是否已过
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - attempt.getLastFailedTime();
            
            if (elapsedTime > LOCK_DURATION_MS) {
                // 时间到了，自动解锁
                attempt.setLocked(false);
                attempt.setFailedAttempts(0); // 重置尝试次数 (这里使用了修复后的方法)
                lockMap.put(username, attempt);
            } else {
                // 时间未到，拒绝登录
                response.sendRedirect("login.jsp?error=locked");
                return;
            }
        }

        // --- 需求 2：读取 CSV 验证 ---
        boolean isAuthenticated = false;
        String userRole = null;
        String userDisplayName = null;

        // 尝试从 WEB-INF 目录读取
        String filePath = "/WEB-INF/users.csv";
        try (InputStream is = getServletContext().getResourceAsStream(filePath);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {

            if (is == null) {
                throw new FileNotFoundException("CSV file not found in classpath: " + filePath);
            }

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.toLowerCase().startsWith("username")) {
                    continue; // 跳过空行、注释和表头
                }

                String[] parts = line.split(",", 4);
                if (parts.length >= 4) {
                    String csvUser = parts[0].trim();
                    String csvPass = parts[1].trim();

                    // 匹配用户名和密码
                    if (username.equals(csvUser) && password.equals(csvPass)) {
                        isAuthenticated = true;
                        userRole = parts[2].trim();
                        userDisplayName = parts[3].trim();
                        break;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("login.jsp?error=system");
            return;
        }

        // --- 处理认证结果 ---
        if (isAuthenticated) {
            // 登录成功：重置失败次数
            lockMap.remove(username);

            HttpSession session = request.getSession();
            session.setAttribute("username", username);
            session.setAttribute("role", userRole);
            session.setAttribute("name", userDisplayName);

            response.sendRedirect("index.jsp");

        } else {
            // 登录失败：记录失败次数
            if (attempt == null) {
                attempt = new LoginAttempt();
            }

            attempt.incrementFailedAttempts();
            attempt.setLastFailedTime(System.currentTimeMillis());
            attempt.setLocked(false); // 确保非锁定状态

            // 检查是否达到最大尝试次数
            if (attempt.getFailedAttempts() >= MAX_ATTEMPTS) {
                attempt.setLocked(true);
                lockMap.put(username, attempt);
                // 账号已锁定
                response.sendRedirect("login.jsp?error=locked");
            } else {
                // 账号未锁定，计算剩余次数
                lockMap.put(username, attempt);
                int remaining = MAX_ATTEMPTS - attempt.getFailedAttempts();
                // 重定向并携带剩余次数参数
                response.sendRedirect("login.jsp?error=invalid&attempts=" + remaining);
            }
        }
    }

    // 内部类：用于存储登录尝试信息
    private static class LoginAttempt {
        private int failedAttempts = 0;
        private long lastFailedTime = 0;
        private boolean locked = false;

        public void incrementFailedAttempts() {
            this.failedAttempts++;
        }

        public int getFailedAttempts() {
            return failedAttempts;
        }

        // 【修复点】添加了这个缺失的 Setter 方法
        public void setFailedAttempts(int failedAttempts) {
            this.failedAttempts = failedAttempts;
        }

        public long getLastFailedTime() {
            return lastFailedTime;
        }

        public void setLastFailedTime(long lastFailedTime) {
            this.lastFailedTime = lastFailedTime;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }
    }
}