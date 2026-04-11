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

public class ApplicationServlet extends HttpServlet {
    private final DataManager dataManager = new DataManager();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // 所有申请相关接口都需要登录且账号处于 active。
        User user = requireLogin(req, resp, out);
        if (user == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();

        if ("/review-list".equalsIgnoreCase(path)) {
            // 审核列表仅 MO/Admin 可见。
            if (!isRole(user, "MO", "Admin")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            String positionId = value(req.getParameter("positionId"));
            List<Map<String, String>> apps;
            if (!positionId.isEmpty()) {
                apps = dataManager.getApplicationsByPosition(positionId);
                if ("MO".equalsIgnoreCase(user.getRole())) {
                    // MO 只能看到自己岗位下的申请。
                    apps = filterMoApplications(apps, user);
                }
            } else if ("MO".equalsIgnoreCase(user.getRole())) {
                apps = filterMoApplications(dataManager.getAllApplications(), user);
            } else {
                apps = dataManager.getAllApplications();
            }

            apps.removeIf(a -> "canceled".equalsIgnoreCase(value(a.get("status"))));

            out.print(new JSONObject().put("success", true).put("applications", new JSONArray(apps)).toString());
            return;
        }

        if ("/my-list".equalsIgnoreCase(path)) {
            // TA 查看自己的投递，Admin 可按 userId 查看任意 TA。
            if (!isRole(user, "TA", "Admin")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            String targetUserId = value(req.getParameter("userId"));
            if (targetUserId.isEmpty()) {
                targetUserId = user.getUserId();
            }
            if (!"Admin".equalsIgnoreCase(user.getRole()) && !targetUserId.equalsIgnoreCase(user.getUserId())) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            List<Map<String, String>> apps = dataManager.getApplicationsByUser(targetUserId);
            List<Map<String, String>> enriched = new ArrayList<>();
            // Build TA "my applications" view model:
            // attach position status/deadline/department to each application record
            // so frontend can render status table directly from one response.
            for (Map<String, String> app : apps) {
                Map<String, String> item = new java.util.LinkedHashMap<>(app);
                // 回填岗位状态/截止日期，便于前端直接展示申请上下文。
                Map<String, String> p = dataManager.getPositionById(value(app.get("positionId")));
                if (p != null) {
                    item.put("positionStatus", value(p.get("status")));
                    item.put("positionDeadline", value(p.get("deadline")));
                    item.put("positionDepartment", value(p.get("department")));
                }
                enriched.add(item);
            }

            out.print(new JSONObject().put("success", true).put("applications", new JSONArray(enriched)).toString());
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

        if ("/submit".equalsIgnoreCase(path)) {
            // 只有 TA 能提交申请。
            if (!isRole(user, "TA")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            // Submission rule for TA flow:
            // only allow apply when target position exists and is not closed,
            // then create pending application + related notifications.
            String positionId = value(req.getParameter("positionId"));
            String priority = value(req.getParameter("priority"));
            if (positionId.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Missing positionId").toString());
                return;
            }

            Map<String, String> position = dataManager.getPositionById(positionId);
            if (position == null) {
                out.print(new JSONObject().put("success", false).put("message", "Position does not exist").toString());
                return;
            }
            // 岗位一旦 closed，禁止继续提交。
            String positionStatus = value(position.get("status")).toLowerCase();
            if ("closed".equals(positionStatus)) {
                out.print(new JSONObject().put("success", false).put("message", "Position is closed and cannot be applied").toString());
                return;
            }

            Map<String, String> app = dataManager.submitApplication(user.getUserId(), user.getUserName(), positionId, priority);
            if (app == null) {
                out.print(new JSONObject().put("success", false).put("message", "Position does not exist").toString());
                return;
            }

            // 提交后双向通知：TA 收到提交成功，MO 收到待审核提醒。
            dataManager.saveNotification(user.getUserId(), "application", "Application Submitted", "Your application was submitted.");
            String moId = app.get("moId");
            if (moId != null && !moId.isEmpty()) {
                dataManager.saveNotification(moId, "application", "New Application", "A new application needs review.");
            }
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "SUBMIT_APPLICATION", positionId, "success");

            out.print(new JSONObject().put("success", true).put("message", "Application submitted").put("application", new JSONObject(app)).toString());
            return;
        }

        if ("/review".equalsIgnoreCase(path)) {
            // 审核仅 MO/Admin，且 MO 只能处理自己负责的申请。
            if (!isRole(user, "MO", "Admin")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            String applicationId = value(req.getParameter("applicationId"));
            String decision = value(req.getParameter("decision"));
            String feedback = value(req.getParameter("feedback"));
            if (applicationId.isEmpty() || decision.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Missing applicationId or decision").toString());
                return;
            }

            Map<String, String> app = dataManager.getApplicationById(applicationId);
            if (app == null) {
                out.print(new JSONObject().put("success", false).put("message", "Application not found").toString());
                return;
            }
            if (!isRole(user, "Admin") && !isMoOwner(user, app.get("moId"))) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission for this application").toString());
                return;
            }

            // 兼容前端不同表达（accept/approve/reject...），最终落到 approved/rejected。
            String normalizedDecision = decision.toLowerCase();
            if (!("accept".equals(normalizedDecision) || "accepted".equals(normalizedDecision) || "approve".equals(normalizedDecision)
                    || "approved".equals(normalizedDecision) || "reject".equals(normalizedDecision) || "rejected".equals(normalizedDecision))) {
                out.print(new JSONObject().put("success", false).put("message", "Invalid decision").toString());
                return;
            }

            boolean ok = dataManager.processApplication(applicationId, decision, feedback);
            if (!ok) {
                out.print(new JSONObject().put("success", false).put("message", "Failed to update application").toString());
                return;
            }

            Map<String, String> updated = dataManager.getApplicationById(applicationId);
            String taUserId = value(app.get("userId"));
            if (!taUserId.isEmpty()) {
                // 按最终状态生成面向 TA 的结果通知文案。
                String actionText = "Application reviewed";
                String status = value(updated == null ? "" : updated.get("status"));
                if ("approved".equalsIgnoreCase(status)) {
                    actionText = "Your application has been accepted";
                } else if ("rejected".equalsIgnoreCase(status)) {
                    actionText = "Your application has been rejected";
                }
                dataManager.saveNotification(taUserId, "application", "Application Result", actionText);
            }
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "REVIEW_APPLICATION", applicationId + " -> " + decision, "success");

            out.print(new JSONObject().put("success", true).put("message", "Application updated").put("application", new JSONObject(updated)).toString());
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    private List<Map<String, String>> filterMoApplications(List<Map<String, String>> all, User user) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> app : all) {
            String moId = app.get("moId");
            if (isMoOwner(user, moId)) {
                result.add(app);
            }
        }
        return result;
    }

    private boolean isMoOwner(User user, String moId) {
        // moId 既可能存用户账号，也可能存 qmId，两者都视为归属。
        return eq(moId, user.getUserId()) || eq(moId, user.getQmId());
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
        if (!"active".equalsIgnoreCase(value(user.getStatus()))) {
            // 被管理员停用后，旧会话访问会被立即失效。
            session.invalidate();
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(new JSONObject().put("success", false).put("message", "Account is inactive").toString());
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

    private boolean eq(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private Map<String, String> getProfileForApply(User user) {
        if (user == null) {
            return null;
        }
        Map<String, String> profile = dataManager.getProfile(value(user.getUserId()));
        if (profile != null) {
            return profile;
        }

        // 兼容 userId/qmId 双标识场景。
        String qmId = value(user.getQmId());
        if (!qmId.isEmpty() && !qmId.equalsIgnoreCase(value(user.getUserId()))) {
            return dataManager.getProfile(qmId);
        }
        return null;
    }

    private boolean isProfileCompleteForApply(Map<String, String> profile) {
        if (profile == null) {
            return false;
        }

        String grade = value(profile.get("grade"));
        String major = value(profile.get("major"));
        String gpa = value(profile.get("gpa"));
        String email = value(profile.get("email"));
        String skills = value(profile.get("skills"));
        String availableTime = value(profile.get("availableTime"));
        String resumeFileName = value(profile.get("resumeFileName"));
        String resumeStoredName = value(profile.get("resumeStoredName"));

        return !grade.isEmpty()
                && !major.isEmpty()
                && !gpa.isEmpty()
                && !email.isEmpty()
                && !skills.isEmpty()
                && !availableTime.isEmpty()
                && (!resumeFileName.isEmpty() || !resumeStoredName.isEmpty());
    }

    private String value(String s) {
        return s == null ? "" : s.trim();
    }

}
