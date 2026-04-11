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
import java.util.List;
import java.util.Map;

public class PositionServlet extends HttpServlet {
    private final DataManager dataManager = new DataManager();

    // 处理岗位查询接口。
    // 根据登录角色返回可见岗位：MO 仅可见本人名下岗位，其他角色可见全部。
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        User user = requireLogin(req, resp, out);
        if (user == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/list".equalsIgnoreCase(path) || "/all".equalsIgnoreCase(path) || path.isEmpty() || "/".equals(path)) {
            List<Map<String, String>> positions = dataManager.getAllPositions();
            if ("MO".equalsIgnoreCase(user.getRole())) {
                positions = filterMoPositions(positions, user);
            }
            out.print(new JSONArray(positions).toString());
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    // 处理岗位创建、更新、发布与状态变更。
    // 每个分支都包含角色校验与归属校验，防止越权操作。
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

        if ("/create".equalsIgnoreCase(path)) {
            if (!isRole(user, "MO", "Admin")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            // POST /api/position/create
            // Implements MO posting feature: validates role, resolves owner moId,
            // then delegates persistence + ID generation to DataManager.createPosition.
            String title = value(req.getParameter("title"));
            String department = value(req.getParameter("department"));
            String salary = value(req.getParameter("salary"));
            String description = value(req.getParameter("description"));
            String requirements = value(req.getParameter("requirements"));
            String openings = value(req.getParameter("openings"));
            String deadline = value(req.getParameter("deadline"));
            String moId = "Admin".equalsIgnoreCase(user.getRole()) ? value(req.getParameter("moId")) : user.getUserId();

            if (title.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Title is required").toString());
                return;
            }
            if (moId.isEmpty()) {
                moId = user.getQmId() == null ? user.getUserId() : user.getQmId();
            }

            Map<String, String> created = dataManager.createPosition(title, department, salary, description, requirements, moId, openings, deadline);
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "CREATE_POSITION", title, "success");
            out.print(new JSONObject().put("success", true).put("message", "Position created").put("position", new JSONObject(created)).toString());
            return;
        }

        if ("/update".equalsIgnoreCase(path)) {
            if (!isRole(user, "MO", "Admin")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            // POST /api/position/update
            // Permission rule: only the owning MO (matched by userId/qmId) or Admin
            // may edit the target position record.
            String positionId = value(req.getParameter("positionId"));
            if (positionId.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Missing positionId").toString());
                return;
            }

            Map<String, String> existing = dataManager.getPositionById(positionId);
            if (existing == null) {
                out.print(new JSONObject().put("success", false).put("message", "Position not found").toString());
                return;
            }
            if (!canManagePosition(user, existing)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission for this position").toString());
                return;
            }

            String title = value(req.getParameter("title"));
            String department = value(req.getParameter("department"));
            String salary = value(req.getParameter("salary"));
            String description = value(req.getParameter("description"));
            String requirements = value(req.getParameter("requirements"));
            String openings = value(req.getParameter("openings"));
            String deadline = value(req.getParameter("deadline"));

            boolean ok = dataManager.updatePosition(positionId, title, department, salary, description, requirements, openings, deadline);
            if (!ok) {
                out.print(new JSONObject().put("success", false).put("message", "Position update failed").toString());
                return;
            }

            Map<String, String> updated = dataManager.getPositionById(positionId);
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "UPDATE_POSITION", positionId, "success");
            out.print(new JSONObject().put("success", true).put("message", "Position updated").put("position", new JSONObject(updated)).toString());
            return;
        }

        if ("/status".equalsIgnoreCase(path) || "/publish".equalsIgnoreCase(path)) {
            if (!isRole(user, "MO", "Admin")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            // POST /api/position/status or /api/position/publish
            // /status = operational state change (open/closed),
            // /publish = force-open and trigger notification fan-out to active TAs.
            boolean publishAction = "/publish".equalsIgnoreCase(path);

            String positionId = value(req.getParameter("positionId"));
            String status = value(req.getParameter("status"));
            if (publishAction) {
                status = "open";
            }
            if (positionId.isEmpty() || status.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Missing positionId or status").toString());
                return;
            }

            String normalized = status.toLowerCase();
            if ("reopen".equals(normalized) || "reopening".equals(normalized)) {
                normalized = "open";
            }
            if (!("open".equals(normalized) || "closed".equals(normalized))) {
                out.print(new JSONObject().put("success", false).put("message", "Invalid status").toString());
                return;
            }

            Map<String, String> existing = dataManager.getPositionById(positionId);
            if (existing == null) {
                out.print(new JSONObject().put("success", false).put("message", "Position not found").toString());
                return;
            }
            if (!canManagePosition(user, existing)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission for this position").toString());
                return;
            }

            boolean ok = dataManager.updatePositionStatus(positionId, normalized);
            if (!ok) {
                out.print(new JSONObject().put("success", false).put("message", "Status update failed").toString());
                return;
            }

            if (publishAction) {
                Map<String, String> latest = dataManager.getPositionById(positionId);
                String title = latest == null ? positionId : value(latest.get("title"));
                int sentCount = 0;
                for (User taUser : dataManager.getAllUsers()) {
                    if (!"TA".equalsIgnoreCase(value(taUser.getRole()))) {
                        continue;
                    }
                    if (!"active".equalsIgnoreCase(value(taUser.getStatus()))) {
                        continue;
                    }
                    dataManager.saveNotification(
                            taUser.getUserId(),
                            "position",
                            "New Position Published",
                            "A position has been published: " + title);
                    sentCount++;
                }

                dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "PUBLISH_POSITION", positionId + " -> open, notified=" + sentCount, "success");
                out.print(new JSONObject()
                        .put("success", true)
                        .put("message", "Position published and notifications sent")
                        .put("status", normalized)
                        .put("notifiedCount", sentCount)
                        .toString());
            } else {
                dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "UPDATE_POSITION_STATUS", positionId + " -> " + normalized, "success");
                out.print(new JSONObject().put("success", true).put("message", "Position status updated").put("status", normalized).toString());
            }
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    // 按岗位所属 MO 过滤数据，兼容 userId/qmId 双匹配。
    private List<Map<String, String>> filterMoPositions(List<Map<String, String>> all, User user) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> p : all) {
            String moId = p.get("moId");
            if (eq(moId, user.getUserId()) || eq(moId, user.getQmId())) {
                result.add(p);
            }
        }
        return result;
    }

    // 校验登录态并返回当前用户。
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

    // 判断角色是否在允许集合中。
    private boolean isRole(User user, String... roles) {
        for (String role : roles) {
            if (role.equalsIgnoreCase(user.getRole())) {
                return true;
            }
        }
        return false;
    }

    // 安全取值工具。
    private String value(String s) {
        return s == null ? "" : s.trim();
    }

    // 忽略大小写的字符串比较。
    private boolean eq(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    // 判断当前用户是否有权限管理目标岗位。
    // 规则：Admin 全量可管；MO 仅可管理自己名下岗位。
    private boolean canManagePosition(User user, Map<String, String> position) {
        if (isRole(user, "Admin")) {
            return true;
        }
        String moId = position == null ? "" : value(position.get("moId"));
        return eq(moId, user.getUserId()) || eq(moId, user.getQmId());
    }
}
