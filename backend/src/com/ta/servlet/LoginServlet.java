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

/**
 * 认证入口 Servlet。
 *
 * <p>负责登录、登出、会话查询、角色提示与 TA/MO 自注册，
 * 并内置失败锁定与注册唯一性约束（role + qmId）。</p>
 */
public class LoginServlet extends HttpServlet {
    private final DataManager dataManager = new DataManager();
    // 登录失败策略：3 次失败后锁定 60 秒（按账号维度）。
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_DURATION_MILLIS = 60 * 1000L;
    private static final Map<String, LoginAttemptState> LOGIN_ATTEMPTS = new ConcurrentHashMap<>();

    private static class LoginAttemptState {
        private int failedAttempts;
        private long lockUntil;
    }

    /**
     * 处理会话查询、角色提示与登出请求。
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // 兼容前端登录页的角色提示能力（输入账号后先猜测角色）。
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

        // 主动登出：清会话并记录日志。
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

        // 默认分支：查询当前会话状态，用于前端页面守卫。
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

        if (!"active".equalsIgnoreCase(user.getStatus())) {
            session.invalidate();
            out.print(new JSONObject().put("loggedIn", false).toString());
            return;
        }

        out.print(new JSONObject()
                .put("loggedIn", true)
                .put("user", toUserJson(user))
                .toString());
    }

    /**
     * 处理登录与注册提交。
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // register 与 login 共享入口，根据 action 分流。
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

        // 若当前仍在锁定窗口内，直接返回剩余秒数。
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

        // 账号存在但非 active：给出明确提示（pending 与 inactive 区分）。
        User existing = dataManager.getUserById(userId);
        if (existing != null && !"active".equalsIgnoreCase(existing.getStatus()) && existing.getPassword().equals(password)) {
            if ("pending".equalsIgnoreCase(existing.getStatus())) {
                out.print(new JSONObject().put("success", false).put("message", "Account pending admin approval").toString());
                return;
            }
            out.print(new JSONObject().put("success", false).put("message", "Account is not active").toString());
            return;
        }

        // 统一认证失败处理：记录失败次数、可能触发锁定、写审计日志。
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

        // 若登录页指定了角色，则进行角色一致性校验。
        if (!role.isEmpty() && !user.getRole().equalsIgnoreCase(role)) {
            registerFailedAttempt(lockKey);
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "LOGIN", "role mismatch", "failed");
            out.print(new JSONObject().put("success", false).put("message", "Role mismatch").toString());
            return;
        }

        // 登录成功后清空失败记录，避免历史失败影响后续正常登录。
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

    /**
     * 处理 TA/MO 自注册流程。
     */
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

        // 自注册仅允许 TA/MO，Admin 不允许走此入口。
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

        String effectiveQmId = qmId.isEmpty() ? userId : qmId;
        if (existsSameRoleAndQmId(normalizedRole, effectiveQmId)) {
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", "Another account with same role and QM ID already exists")
                    .toString());
            return;
        }

        // 新注册默认进入 pending，等待管理员审批。
        User user = new User(userId, userName, email, password, normalizedRole, effectiveQmId);
        user.setStatus("pending");
        dataManager.saveUser(user);
        dataManager.writeLog(userId, userName, normalizedRole, "REGISTER", "pending approval", "success");

        out.print(new JSONObject()
                .put("success", true)
                .put("message", "Registration submitted. Please wait for admin approval")
                .toString());
    }

    /**
     * 校验密码复杂度是否满足策略。
     */
    private boolean isPasswordComplex(String password) {
        if (password == null) {
            return false;
        }
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{8,}$");
    }

    /**
     * 检查是否已存在同 role + qmId 的账号，避免同人多账号。
     */
    private boolean existsSameRoleAndQmId(String role, String qmId) {
        String normalizedRole = value(role).toUpperCase(Locale.ROOT);
        String normalizedQmId = value(qmId).toLowerCase(Locale.ROOT);
        if (normalizedRole.isEmpty() || normalizedQmId.isEmpty()) {
            return false;
        }

        for (User item : dataManager.getAllUsers()) {
            String itemRole = value(item.getRole()).toUpperCase(Locale.ROOT);
            String itemQmId = value(item.getQmId()).toLowerCase(Locale.ROOT);
            if (normalizedRole.equals(itemRole) && normalizedQmId.equals(itemQmId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将用户实体转为前端使用的标准 JSON 结构。
     */
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

    /**
     * 空值安全取值并去首尾空白。
     */
    private String value(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * 归一化登录锁定键（按账号小写）。
     */
    private String normalizeLockKey(String userId) {
        return value(userId).toLowerCase(Locale.ROOT);
    }

    /**
     * 查询账号剩余锁定秒数；锁到期后会顺便清理内存态。
     */
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
                // 锁定时间到期后重置状态，避免内存中残留无效锁信息。
                state.lockUntil = 0;
                state.failedAttempts = 0;
                LOGIN_ATTEMPTS.remove(lockKey, state);
                return 0;
            }
            return Math.max(1, (state.lockUntil - now + 999) / 1000);
        }
    }

    /**
     * 记录一次登录失败并在达到阈值后写入锁定时窗。
     */
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
                // 达到阈值后开始锁定，并将失败计数归零用于下一轮统计。
                state.failedAttempts = 0;
                state.lockUntil = now + LOCK_DURATION_MILLIS;
            }
        }
    }

    /**
     * 清空指定账号的失败与锁定状态。
     */
    private void clearFailedAttempts(String lockKey) {
        if (lockKey.isEmpty()) {
            return;
        }
        LOGIN_ATTEMPTS.remove(lockKey);
    }
}
