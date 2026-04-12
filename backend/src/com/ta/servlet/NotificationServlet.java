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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 通知中心接口。
 *
 * <p>提供通知列表读取与已读状态变更能力，供 TA/MO 页面复用。</p>
 */
public class NotificationServlet extends HttpServlet {
    private final DataManager dataManager = new DataManager();

    /**
     * 处理通知读取接口（/list）。
     */
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
            String type = value(req.getParameter("type")).toLowerCase();
            List<Map<String, String>> all = dataManager.getNotifications(user.getUserId());
            List<Map<String, String>> filtered = new ArrayList<>();

            for (Map<String, String> item : all) {
                String itemType = value(item.get("type")).toLowerCase();
                if (type.isEmpty() || "all".equals(type) || type.equals(itemType)) {
                    filtered.add(item);
                }
            }

            Collections.reverse(filtered);
            int unreadCount = 0;
            for (Map<String, String> item : filtered) {
                if (!"1".equals(value(item.get("read")))) {
                    unreadCount++;
                }
            }

            out.print(new JSONObject()
                    .put("success", true)
                    .put("notifications", new JSONArray(filtered))
                    .put("unreadCount", unreadCount)
                    .toString());
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    /**
     * 处理通知写操作接口（/read、/read-all）。
     */
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
            if (!ok) {
                out.print(new JSONObject().put("success", false).put("message", "Notification not found").toString());
                return;
            }

            out.print(new JSONObject().put("success", true).put("message", "Notification marked as read").toString());
            return;
        }

        if ("/read-all".equalsIgnoreCase(path)) {
            dataManager.markAllNotificationsRead(user.getUserId());
            out.print(new JSONObject().put("success", true).put("message", "All notifications marked as read").toString());
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    /**
     * 统一登录态校验：要求会话有效且账号 active。
     */
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

        if (!"active".equalsIgnoreCase(value(user.getStatus()))) {
            session.invalidate();
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(new JSONObject().put("success", false).put("message", "Account is inactive").toString());
            return null;
        }

        return user;
    }

    /**
     * 空值安全取值并去首尾空白。
     */
    private String value(String s) {
        return s == null ? "" : s.trim();
    }
}
