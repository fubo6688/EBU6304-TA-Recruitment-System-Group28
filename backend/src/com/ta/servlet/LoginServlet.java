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

        String userId = value(req.getParameter("userId"));
        String password = value(req.getParameter("password"));
        String role = value(req.getParameter("role"));

        if (userId.isEmpty() || password.isEmpty()) {
            out.print(new JSONObject().put("success", false).put("message", "Account or password is empty").toString());
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
