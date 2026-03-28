package com.ta.servlet;

import com.ta.model.User;
import com.ta.util.DataManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class NotificationServlet extends HttpServlet {
    private final DataManager dataManager = new DataManager();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        User user = requireLogin(req, resp, out);
        if (user == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/list".equalsIgnoreCase(path) || path.isEmpty() || "/".equals(path)) {
            List<Map<String, String>> list = dataManager.getNotifications(user.getUserId());
            if (list.isEmpty() && "Admin".equalsIgnoreCase(user.getRole())) {
                dataManager.saveNotification(user.getUserId(), "system", "Admin Center", "Welcome to the admin notification center.");
                dataManager.saveNotification(user.getUserId(), "system", "Tips", "Use Notification Center to send messages to users by role or globally.");
                list = dataManager.getNotifications(user.getUserId());
            }
            out.print(new JSONArray(list).toString());
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        User user = requireLogin(req, resp, out);
        if (user == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();

        if ("/read".equalsIgnoreCase(path)) {
            String notificationId = value(req.getParameter("notificationId"));
            if (notificationId.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Missing notificationId").toString());
                return;
            }
            boolean ok = dataManager.markNotificationRead(user.getUserId(), notificationId);
            out.print(new JSONObject().put("success", ok).put("message", ok ? "Notification marked read" : "Notification not found").toString());
            return;
        }

        if ("/read-all".equalsIgnoreCase(path)) {
            dataManager.markAllNotificationsRead(user.getUserId());
            out.print(new JSONObject().put("success", true).put("message", "All notifications marked read").toString());
            return;
        }

        if ("/create".equalsIgnoreCase(path)) {
            if (!isRole(user, "MO", "Admin")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            String targetType = value(req.getParameter("targetType")).toLowerCase();
            String userId = value(req.getParameter("userId"));
            String role = value(req.getParameter("role"));
            String all = value(req.getParameter("all"));
            String type = value(req.getParameter("type"));
            String title = value(req.getParameter("title"));
            String message = value(req.getParameter("message"));

            if (message.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Missing parameters").toString());
                return;
            }

            int count = 0;
            String notifyType = type.isEmpty() ? "system" : type;
            String notifyTitle = title.isEmpty() ? "Notification" : title;

            if ("all".equals(targetType) || "1".equals(all)) {
                if (!isRole(user, "Admin")) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print(new JSONObject().put("success", false).put("message", "No permission for broadcast").toString());
                    return;
                }
                for (User u : dataManager.getAllUsers()) {
                    dataManager.saveNotification(u.getUserId(), notifyType, notifyTitle, message);
                    count++;
                }
            } else if ("role".equals(targetType)) {
                if (!isRole(user, "Admin")) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print(new JSONObject().put("success", false).put("message", "No permission for role broadcast").toString());
                    return;
                }
                if (role.isEmpty()) {
                    out.print(new JSONObject().put("success", false).put("message", "Missing role").toString());
                    return;
                }
                for (User u : dataManager.getAllUsers()) {
                    if (role.equalsIgnoreCase(value(u.getRole()))) {
                        dataManager.saveNotification(u.getUserId(), notifyType, notifyTitle, message);
                        count++;
                    }
                }
            } else {
                if (userId.isEmpty()) {
                    out.print(new JSONObject().put("success", false).put("message", "Missing userId").toString());
                    return;
                }
                dataManager.saveNotification(userId, notifyType, notifyTitle, message);
                count = 1;
            }

            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "CREATE_NOTIFICATION", "targetType=" + targetType + ", userId=" + userId + ", role=" + role + ", count=" + count, "success");
            out.print(new JSONObject().put("success", true).put("count", count).put("message", "Notification created").toString());
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    private User requireLogin(HttpServletRequest req, HttpServletResponse resp, PrintWriter out) {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(new JSONObject().put("success", false).put("message", "Not logged in").toString());
            return null;
        }
        User user = dataManager.getUserById(String.valueOf(session.getAttribute("userId")));
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(new JSONObject().put("success", false).put("message", "User not found").toString());
            return null;
        }
        return user;
    }

    private boolean isRole(User user, String... roles) {
        for (String role : roles) {
            if (role.equalsIgnoreCase(user.getRole())) {
                return true;
            }
        }
        return false;
    }

    private String value(String s) {
        return s == null ? "" : s.trim();
    }
}
