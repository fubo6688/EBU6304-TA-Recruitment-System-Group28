package com.ta.servlet;

import com.ta.model.User;
import com.ta.util.DataManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginServlet extends HttpServlet {
    private final DataManager dataManager = new DataManager();
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_DURATION_MILLIS = 60 * 1000L;
    private static final Map<String, LoginAttemptState> LOGIN_ATTEMPTS = new ConcurrentHashMap<>();

    private static class LoginAttemptState {
        private int failedAttempts;
        private long lockUntil;
    }

    // 处理登录态查询、角色提示与登出。
    // 同一入口通过 action 参数区分子能力，统一输出 JSON 给前端消费。
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String action = req.getParameter("action");
        if ("role-hint".equalsIgnoreCase(action)) {
            String userId = value(req.getParameter("userId"));
            if (userId.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Account is empty").toString());
                return;
            }
            User user = dataManager.getUserById(userId);
            if (user == null) {
                out.print(new JSONObject().put("success", false).put("message", "Account not found").toString());
                return;
            }
            out.print(new JSONObject()
                    .put("success", true)
                    .put("role", user.getRole())
                    .toString());
            return;
        }

        if ("logout".equalsIgnoreCase(action)) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                String userId = (String) session.getAttribute("userId");
                String userName = (String) session.getAttribute("userName");
                String role = (String) session.getAttribute("userRole");
                session.invalidate();
                dataManager.writeLog(userId, userName, role, "LOGOUT", "logout", "success");
            }
            out.print(new JSONObject().put("success", true).put("message", "Logged out").toString());
            return;
        }

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            out.print(new JSONObject().put("loggedIn", false).toString());
            return;
        }

        User user = dataManager.getUserById((String) session.getAttribute("userId"));
        if (user == null) {
            session.invalidate();
            out.print(new JSONObject().put("loggedIn", false).toString());
            return;
        }

        out.print(new JSONObject()
                .put("loggedIn", true)
                .put("user", toUserJson(user))
                .toString());
    }

    // 处理登录与注册提交。
    // 登录流程包含锁定机制、角色校验、会话写入；注册流程委托 handleRegister。
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String action = value(req.getParameter("action"));
        if ("register".equalsIgnoreCase(action)) {
            handleRegister(req, out);
            return;
        }

        String userId = value(req.getParameter("userId"));
        String password = value(req.getParameter("password"));
        String role = value(req.getParameter("role"));
        String lockKey = normalizeLockKey(userId);

        if (userId.isEmpty() || password.isEmpty()) {
            out.print(new JSONObject().put("success", false).put("message", "Account or password is empty").toString());
            return;
        }

        long remaining = getRemainingLockSeconds(lockKey);
        if (remaining > 0) {
            out.print(new JSONObject()
                    .put("success", false)
                    .put("locked", true)
                    .put("remainingSeconds", remaining)
                    .put("message", "Account locked. Please try again in " + remaining + " seconds")
                    .toString());
            return;
        }

        User existing = dataManager.getUserById(userId);
        if (existing != null && !"active".equalsIgnoreCase(existing.getStatus()) && existing.getPassword().equals(password)) {
            if ("pending".equalsIgnoreCase(existing.getStatus())) {
                out.print(new JSONObject().put("success", false).put("message", "Account pending admin approval").toString());
                return;
            }
            out.print(new JSONObject().put("success", false).put("message", "Account is not active").toString());
            return;
        }

        User user = dataManager.validateLogin(userId, password);
        if (user == null) {
            registerFailedAttempt(lockKey);
            long remainingAfterFail = getRemainingLockSeconds(lockKey);
            dataManager.writeLog(userId, "", role, "LOGIN", "login", "failed");
            if (remainingAfterFail > 0) {
                out.print(new JSONObject()
                        .put("success", false)
                        .put("locked", true)
                        .put("remainingSeconds", remainingAfterFail)
                        .put("message", "Account locked. Please try again in " + remainingAfterFail + " seconds")
                        .toString());
            } else {
                out.print(new JSONObject().put("success", false).put("message", "Invalid account or password").toString());
            }
            return;
        }

        if (!role.isEmpty() && !user.getRole().equalsIgnoreCase(role)) {
            registerFailedAttempt(lockKey);
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "LOGIN", "role mismatch", "failed");
            out.print(new JSONObject().put("success", false).put("message", "Role mismatch").toString());
            return;
        }

        clearFailedAttempts(lockKey);

        HttpSession session = req.getSession(true);
        session.setMaxInactiveInterval(30 * 60);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userName", user.getUserName());
        session.setAttribute("userRole", user.getRole());
        session.setAttribute("currentUser", user);

        dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "LOGIN", "login", "success");

        out.print(new JSONObject()
                .put("success", true)
                .put("message", "Login successful")
                .put("user", toUserJson(user))
                .toString());
    }

    // 处理用户注册：校验必填项、角色与密码复杂度，创建 pending 账号并记录日志。
    private void handleRegister(HttpServletRequest req, PrintWriter out) {
        String userId = value(req.getParameter("userId"));
        String userName = value(req.getParameter("userName"));
        String email = value(req.getParameter("email"));
        String password = value(req.getParameter("password"));
        String role = value(req.getParameter("role"));
        String qmId = value(req.getParameter("qmId"));

        if (userId.isEmpty() || userName.isEmpty() || email.isEmpty() || password.isEmpty() || role.isEmpty()) {
            out.print(new JSONObject().put("success", false).put("message", "Required fields are missing").toString());
            return;
        }

        String normalizedRole = role.toUpperCase(Locale.ROOT);
        if (!("TA".equals(normalizedRole) || "MO".equals(normalizedRole))) {
            out.print(new JSONObject().put("success", false).put("message", "Only TA or MO can register").toString());
            return;
        }

        if (!isPasswordComplex(password)) {
            out.print(new JSONObject().put("success", false)
                    .put("message", "Password must be at least 8 chars with uppercase, lowercase, digit, and letters/digits only")
                    .toString());
            return;
        }

        if (dataManager.getUserById(userId) != null) {
            out.print(new JSONObject().put("success", false).put("message", "Account already exists").toString());
            return;
        }

        User user = new User(userId, userName, email, password, normalizedRole, qmId.isEmpty() ? userId : qmId);
        user.setStatus("pending");
        dataManager.saveUser(user);
        dataManager.writeLog(userId, userName, normalizedRole, "REGISTER", "pending approval", "success");

        out.print(new JSONObject()
                .put("success", true)
                .put("message", "Registration submitted. Please wait for admin approval")
                .toString());
    }

    // 密码复杂度校验：至少 8 位，含大小写字母和数字，且仅允许字母数字。
    private boolean isPasswordComplex(String password) {
        if (password == null) {
            return false;
        }
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{8,}$");
    }

    // 将后端 User 对象转换成前端使用的标准 JSON 结构。
    private JSONObject toUserJson(User user) {
        return new JSONObject()
                .put("userId", user.getUserId())
                .put("userName", user.getUserName())
                .put("email", user.getEmail())
                .put("userRole", user.getRole())
                .put("role", user.getRole())
                .put("qmId", user.getQmId())
                .put("status", user.getStatus());
    }

    // 安全取值工具。
    private String value(String s) {
        return s == null ? "" : s.trim();
    }

    // 规范化登录锁定键，避免大小写差异导致重复状态。
    private String normalizeLockKey(String userId) {
        return value(userId).toLowerCase(Locale.ROOT);
    }

    // 读取账号锁定剩余秒数。
    // 如果已过期会自动清理状态并返回 0。
    private long getRemainingLockSeconds(String lockKey) {
        if (lockKey.isEmpty()) {
            return 0;
        }
        LoginAttemptState state = LOGIN_ATTEMPTS.get(lockKey);
        if (state == null) {
            return 0;
        }
        synchronized (state) {
            if (state.lockUntil <= 0) {
                return 0;
            }
            long now = System.currentTimeMillis();
            if (state.lockUntil <= now) {
                state.lockUntil = 0;
                state.failedAttempts = 0;
                LOGIN_ATTEMPTS.remove(lockKey, state);
                return 0;
            }
            return Math.max(1, (state.lockUntil - now + 999) / 1000);
        }
    }

    // 记录一次登录失败并在阈值达到时设置锁定时间。
    private void registerFailedAttempt(String lockKey) {
        if (lockKey.isEmpty()) {
            return;
        }

        LoginAttemptState state = LOGIN_ATTEMPTS.computeIfAbsent(lockKey, k -> new LoginAttemptState());
        synchronized (state) {
            long now = System.currentTimeMillis();
            if (state.lockUntil > now) {
                return;
            }

            state.failedAttempts += 1;
            if (state.failedAttempts >= MAX_FAILED_ATTEMPTS) {
                state.failedAttempts = 0;
                state.lockUntil = now + LOCK_DURATION_MILLIS;
            }
        }
    }

    // 登录成功后清除失败次数和锁定状态。
    private void clearFailedAttempts(String lockKey) {
        if (lockKey.isEmpty()) {
            return;
        }
        LOGIN_ATTEMPTS.remove(lockKey);
    }
}
