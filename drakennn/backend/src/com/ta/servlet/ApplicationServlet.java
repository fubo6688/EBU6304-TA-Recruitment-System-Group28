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

        User user = requireLogin(req, resp, out);
        if (user == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();

        if ("/review-list".equalsIgnoreCase(path)) {
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

    private String value(String s) {
        return s == null ? "" : s.trim();
    }

}
