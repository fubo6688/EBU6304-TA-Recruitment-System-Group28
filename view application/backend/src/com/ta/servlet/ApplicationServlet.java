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
        if ("/list".equalsIgnoreCase(path) || path.isEmpty() || "/".equals(path)) {
            List<Map<String, String>> apps;
            if ("TA".equalsIgnoreCase(user.getRole())) {
                apps = filterTaApplications(dataManager.getAllApplications(), user);
            } else if ("MO".equalsIgnoreCase(user.getRole())) {
                apps = filterMoApplications(dataManager.getAllApplications(), user);
            } else {
                apps = dataManager.getAllApplications();
            }
            out.print(new JSONArray(apps).toString());
            return;
        }

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

        if ("/cancel".equalsIgnoreCase(path)) {
            String applicationId = value(req.getParameter("applicationId"));
            if (applicationId.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Missing applicationId").toString());
                return;
            }

            Map<String, String> app = dataManager.getApplicationById(applicationId);
            if (app == null) {
                out.print(new JSONObject().put("success", false).put("message", "Application not found").toString());
                return;
            }
            if ("TA".equalsIgnoreCase(user.getRole()) && !eq(user.getUserId(), app.get("userId"))) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            String status = value(app.get("status")).toLowerCase();
            if (!"pending".equals(status)) {
                out.print(new JSONObject().put("success", false).put("message", "Only pending applications can be deleted").toString());
                return;
            }

            boolean ok = dataManager.cancelApplication(applicationId);
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "CANCEL_APPLICATION", applicationId, ok ? "success" : "failed");
            out.print(new JSONObject().put("success", ok).put("message", ok ? "Application canceled" : "Cancel failed").toString());
            return;
        }

        if ("/process".equalsIgnoreCase(path)) {
            if (!isRole(user, "MO", "Admin")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            String applicationId = value(req.getParameter("applicationId"));
            String decision = value(req.getParameter("decision"));
            String feedback = value(req.getParameter("feedback"));
            if (applicationId.isEmpty() || decision.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Missing parameters").toString());
                return;
            }

            Map<String, String> app = dataManager.getApplicationById(applicationId);
            if (app == null) {
                out.print(new JSONObject().put("success", false).put("message", "Application not found").toString());
                return;
            }

            if ("MO".equalsIgnoreCase(user.getRole()) && !eq(app.get("moId"), user.getUserId()) && !eq(app.get("moId"), user.getQmId())) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            String oldStatus = value(app.get("status")).toLowerCase();
            String targetStatus = mapDecisionToStatus(decision);
            if (("approved".equals(oldStatus) || "rejected".equals(oldStatus)) && !oldStatus.equals(targetStatus)) {
                out.print(new JSONObject()
                        .put("success", false)
                        .put("message", "Final decision already made and cannot be changed")
                        .toString());
                return;
            }

            boolean ok = dataManager.processApplication(applicationId, decision, feedback);
            if (ok) {
                String applicantId = app.get("userId");
                String msg = "approved".equalsIgnoreCase(decision) || "accept".equalsIgnoreCase(decision) || "accepted".equalsIgnoreCase(decision)
                        ? "Your application was approved."
                        : "Your application was rejected.";
                dataManager.saveNotification(applicantId, "application", "Application Update", msg + (feedback.isEmpty() ? "" : " Feedback: " + feedback));
            }
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "PROCESS_APPLICATION", applicationId + "->" + decision, ok ? "success" : "failed");
            out.print(new JSONObject().put("success", ok).put("message", ok ? "Application processed" : "Process failed").toString());
            return;
        }

        if ("/priority".equalsIgnoreCase(path)) {
            String applicationId = value(req.getParameter("applicationId"));
            String priority = value(req.getParameter("priority"));
            if (applicationId.isEmpty() || priority.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Missing parameters").toString());
                return;
            }

            Map<String, String> app = dataManager.getApplicationById(applicationId);
            if (app == null) {
                out.print(new JSONObject().put("success", false).put("message", "Application not found").toString());
                return;
            }
            if ("TA".equalsIgnoreCase(user.getRole()) && !eq(user.getUserId(), app.get("userId"))) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            boolean ok = dataManager.updateApplicationPriority(applicationId, priority);
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "UPDATE_APPLICATION_PRIORITY", applicationId + "->" + priority, ok ? "success" : "failed");
            out.print(new JSONObject().put("success", ok).put("message", ok ? "Priority updated" : "Update failed").toString());
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    private List<Map<String, String>> filterMoApplications(List<Map<String, String>> all, User user) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> app : all) {
            String moId = app.get("moId");
            if (eq(moId, user.getUserId()) || eq(moId, user.getQmId())) {
                result.add(app);
            }
        }
        return result;
    }

    private List<Map<String, String>> filterTaApplications(List<Map<String, String>> all, User user) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> app : all) {
            String uid = app.get("userId");
            if (eq(uid, user.getUserId()) || eq(uid, user.getQmId())) {
                result.add(app);
            }
        }
        return result;
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

    private String mapDecisionToStatus(String decision) {
        if ("approved".equalsIgnoreCase(decision) || "accept".equalsIgnoreCase(decision) || "accepted".equalsIgnoreCase(decision)) {
            return "approved";
        }
        return "rejected";
    }
}
