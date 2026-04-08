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

public class LoginServlet extends HttpServlet {
    private final DataManager dataManager = new DataManager();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String action = req.getParameter("action");
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

        if (userId.isEmpty() || password.isEmpty()) {
            out.print(new JSONObject().put("success", false).put("message", "Account or password is empty").toString());
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
            dataManager.writeLog(userId, "", role, "LOGIN", "login", "failed");
            out.print(new JSONObject().put("success", false).put("message", "Invalid account or password").toString());
            return;
        }

        if (!role.isEmpty() && !user.getRole().equalsIgnoreCase(role)) {
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "LOGIN", "role mismatch", "failed");
            out.print(new JSONObject().put("success", false).put("message", "Role mismatch").toString());
            return;
        }

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

    private void handleRegister(HttpServletRequest req, PrintWriter out) {
        String userId = value(req.getParameter("userId"));
        String userName = value(req.getParameter("userName"));
        String email = value(req.getParameter("email"));
        String password = value(req.getParameter("password"));
        String role = value(req.getParameter("role"));
        String qmId = value(req.getParameter("qmId"));
        String major = value(req.getParameter("major"));
        String skills = normalizeSkills(value(req.getParameter("skills")));
        String availableTime = normalizeAvailableTime(value(req.getParameter("availableTime")));

        if (userId.isEmpty() || userName.isEmpty() || email.isEmpty() || password.isEmpty() || role.isEmpty()) {
            out.print(new JSONObject().put("success", false).put("message", "Required fields are missing").toString());
            return;
        }

        String normalizedRole = role.toUpperCase(Locale.ROOT);
        if (!("TA".equals(normalizedRole) || "MO".equals(normalizedRole))) {
            out.print(new JSONObject().put("success", false).put("message", "Only TA or MO can register").toString());
            return;
        }

        if ("TA".equals(normalizedRole) && (major.isEmpty() || skills.isEmpty() || availableTime.isEmpty())) {
            out.print(new JSONObject().put("success", false).put("message", "TA registration requires major, skills and available time").toString());
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
        if ("TA".equals(normalizedRole)) {
            dataManager.saveProfile(userId, "", major, "", email, skills, "", "", availableTime, "");
        }
        dataManager.writeLog(userId, userName, normalizedRole, "REGISTER", "pending approval", "success");

        out.print(new JSONObject()
                .put("success", true)
                .put("message", "Registration submitted. Please wait for admin approval")
                .toString());
    }

    private boolean isPasswordComplex(String password) {
        if (password == null) {
            return false;
        }
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{8,}$");
    }

    private String normalizeSkills(String raw) {
        if (raw == null) {
            return "";
        }
        String[] parts = raw.split("[,;]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            String item = value(part);
            if (item.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(item);
        }
        return sb.toString();
    }

    private String normalizeAvailableTime(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.replace("\n", ";").replace("\r", ";").replace(",", ";");
        String[] parts = normalized.split(";");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            String item = value(part);
            if (item.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(item);
        }
        return sb.toString();
    }

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

    private String value(String s) {
        return s == null ? "" : s.trim();
    }
}
