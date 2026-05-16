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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 岗位管理 Servlet。
 *
 * <p>负责岗位列表读取、创建、编辑、开关状态与发布通知，
 * 并按 MO/Admin 权限及岗位归属做访问控制。</p>
 */
public class PositionServlet extends HttpServlet {
    private final DataManager dataManager = new DataManager();

    /**
     * 处理岗位读取类 GET 接口（列表查询）。
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // 所有岗位接口都要求已登录且账号 active。
        User user = requireLogin(req, resp, out);
        if (user == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/list".equalsIgnoreCase(path) || "/all".equalsIgnoreCase(path) || path.isEmpty() || "/".equals(path)) {
            List<Map<String, String>> positions = dataManager.getAllPositions();
            // MO 只能看到自己负责（moId 或 qmId 对应）的岗位。
            if ("MO".equalsIgnoreCase(user.getRole())) {
                positions = filterMoPositions(positions, user);
            }
            out.print(new JSONArray(positions).toString());
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    /**
     * 处理岗位写操作 POST 接口（创建、更新、状态变更、发布）。
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

        if ("/create".equalsIgnoreCase(path)) {
            // 创建岗位仅 MO/Admin 可操作。
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

            if (!deadline.isEmpty()) {
                LocalDate parsedDeadline = parseIsoDate(deadline);
                if (parsedDeadline == null) {
                    out.print(new JSONObject().put("success", false).put("message", "Invalid deadline format. Use YYYY-MM-DD").toString());
                    return;
                }
                if (parsedDeadline.isBefore(LocalDate.now())) {
                    out.print(new JSONObject().put("success", false).put("message", "Deadline cannot be earlier than today").toString());
                    return;
                }
            }

            if (moId.isEmpty()) {
                // 兜底负责人：优先 qmId，避免岗位缺失责任人。
                moId = user.getQmId() == null ? user.getUserId() : user.getQmId();
            }

            Map<String, String> created = dataManager.createPosition(title, department, salary, description, requirements, moId, openings, deadline);
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
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "CREATE_POSITION", title, "success");
            out.print(new JSONObject()
                    .put("success", true)
                    .put("message", "Position created")
                    .put("position", new JSONObject(created))
                    .put("notifiedCount", sentCount)
                    .toString());
            return;
        }

        if ("/update".equalsIgnoreCase(path)) {
            // 岗位更新：先做角色校验，再校验是否属于当前 MO。
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

            String deadlineParam = value(req.getParameter("deadline"));
            if ("open".equals(normalized)) {
                LocalDate today = LocalDate.now();
                String currentDeadline = value(existing.get("deadline"));
                boolean expiredDeadline = isPastDeadline(currentDeadline, today);

                // 过期岗位 reopen/publish 前，必须提供新的有效截止日期。
                if (expiredDeadline && deadlineParam.isEmpty()) {
                    out.print(new JSONObject()
                            .put("success", false)
                            .put("message", "Deadline has passed. Please select a new deadline before reopen")
                            .toString());
                    return;
                }

                if (!deadlineParam.isEmpty()) {
                    LocalDate newDeadline = parseIsoDate(deadlineParam);
                    if (newDeadline == null) {
                        out.print(new JSONObject().put("success", false).put("message", "Invalid deadline format. Use YYYY-MM-DD").toString());
                        return;
                    }
                    if (newDeadline.isBefore(today)) {
                        out.print(new JSONObject().put("success", false).put("message", "New deadline cannot be earlier than today").toString());
                        return;
                    }

                    boolean deadlineUpdated = dataManager.updatePosition(positionId, null, null, null, null, null, null, deadlineParam);
                    if (!deadlineUpdated) {
                        out.print(new JSONObject().put("success", false).put("message", "Failed to update deadline").toString());
                        return;
                    }
                }
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
                // 发布岗位后仅通知 active 的 TA，避免给停用账号推送。
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

    /**
     * 按负责人（userId/qmId）过滤 MO 可见岗位。
     */
    private List<Map<String, String>> filterMoPositions(List<Map<String, String>> all, User user) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> p : all) {
            String moId = p.get("moId");
            // 同时兼容账号主键与 qmId 两种负责人标识。
            if (eq(moId, user.getUserId()) || eq(moId, user.getQmId())) {
                result.add(p);
            }
        }
        return result;
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
            // 被停用账号触发接口访问时立即踢出会话。
            session.invalidate();
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(new JSONObject().put("success", false).put("message", "Account is inactive").toString());
            return null;
        }
        return user;
    }

    /**
     * 判断当前用户角色是否在允许角色集合内。
     */
    private boolean isRole(User user, String... roles) {
        for (String role : roles) {
            if (role.equalsIgnoreCase(user.getRole())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 空值安全取值并去首尾空白。
     */
    private String value(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * 忽略大小写比较两个字符串是否相等。
     */
    private boolean eq(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    /**
     * 判断当前用户是否有权管理指定岗位。
     */
    private boolean canManagePosition(User user, Map<String, String> position) {
        if (isRole(user, "Admin")) {
            return true;
        }
        String moId = position == null ? "" : value(position.get("moId"));
        return eq(moId, user.getUserId()) || eq(moId, user.getQmId());
    }

    /**
     * 判断截止日期是否早于今天。
     */
    private boolean isPastDeadline(String deadline, LocalDate today) {
        LocalDate d = parseIsoDate(deadline);
        return d != null && d.isBefore(today);
    }

    /**
     * 解析 ISO 日期字符串（yyyy-MM-dd），非法返回 null。
     */
    private LocalDate parseIsoDate(String raw) {
        String value = value(raw);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignore) {
            return null;
        }
    }
}
