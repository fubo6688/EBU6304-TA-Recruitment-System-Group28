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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminServlet extends HttpServlet {
    private final DataManager dataManager = new DataManager();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        User user = requireAdmin(req, resp, out);
        if (user == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/analytics".equalsIgnoreCase(path)) {
            Map<String, Object> summary = dataManager.getAnalyticsSummary();
            List<Map<String, String>> apps = dataManager.getAllApplications();
            List<Map<String, Object>> approvedDetails = new ArrayList<>();
            for (Map<String, String> app : apps) {
                if (!"approved".equalsIgnoreCase(value(app.get("status")))) {
                    continue;
                }
                Map<String, String> profile = dataManager.getProfile(value(app.get("userId")));
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("userId", value(app.get("userId")));
                detail.put("userName", value(app.get("userName")));
                detail.put("positionTitle", value(app.get("positionTitle")));
                detail.put("moId", value(app.get("moId")));
                detail.put("appliedDate", value(app.get("appliedDate")));
                detail.put("major", profile == null ? "" : value(profile.get("major")));
                approvedDetails.add(detail);
            }

            JSONObject result = new JSONObject(summary);
            result.put("success", true);
            result.put("approvedDetails", new JSONArray(approvedDetails));
            out.print(result.toString());
            return;
        }

        if ("/export".equalsIgnoreCase(path)) {
            String dataType = normalizeDataType(value(req.getParameter("dataType")));
            String startDate = value(req.getParameter("startDate"));
            String endDate = value(req.getParameter("endDate"));
            String role = value(req.getParameter("role"));
            String format = value(req.getParameter("format")).toLowerCase();

            ExportPayload payload = buildExportPayload(dataType, startDate, endDate, role);

            if ("csv".equals(format)) {
                String csv = toCsv(payload.columns, payload.rows);
                String fileName = "ta_export_" + dataType + "_" + System.currentTimeMillis() + ".csv";
                resp.setContentType("text/csv;charset=UTF-8");
                resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                out.print(csv);
                return;
            }

            List<Map<String, String>> preview = payload.rows;
            if (preview.size() > 10) {
                preview = preview.subList(0, 10);
            }

            out.print(new JSONObject()
                    .put("success", true)
                    .put("dataType", dataType)
                    .put("columns", new JSONArray(payload.columns))
                    .put("rows", new JSONArray(preview))
                    .put("total", payload.rows.size())
                    .toString());
            return;
        }

        if ("/users".equalsIgnoreCase(path)) {
            List<User> users = dataManager.getAllUsers();
            JSONArray array = new JSONArray();
            for (User u : users) {
                array.put(new JSONObject()
                        .put("userId", u.getUserId())
                        .put("userName", u.getUserName())
                        .put("email", u.getEmail())
                        .put("role", u.getRole())
                        .put("qmId", u.getQmId())
                        .put("status", u.getStatus()));
            }
            out.print(new JSONObject().put("success", true).put("users", array).toString());
            return;
        }

        if ("/logs".equalsIgnoreCase(path)) {
            List<Map<String, String>> logs = dataManager.getLogs();
            out.print(new JSONObject().put("success", true).put("logs", new JSONArray(logs)).toString());
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

        User admin = requireAdmin(req, resp, out);
        if (admin == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/user-status".equalsIgnoreCase(path)) {
            String userId = value(req.getParameter("userId"));
            String status = value(req.getParameter("status"));
            if (userId.isEmpty() || status.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Missing parameters").toString());
                return;
            }

            User user = dataManager.getUserById(userId);
            if (user == null) {
                out.print(new JSONObject().put("success", false).put("message", "User not found").toString());
                return;
            }

            user.setStatus(status);
            dataManager.saveUser(user);
            dataManager.writeLog(admin.getUserId(), admin.getUserName(), admin.getRole(), "UPDATE_USER_STATUS", userId + "->" + status, "success");
            out.print(new JSONObject().put("success", true).put("message", "User status updated").toString());
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    private User requireAdmin(HttpServletRequest req, HttpServletResponse resp, PrintWriter out) {
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
        if (!"Admin".equalsIgnoreCase(user.getRole())) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
            return null;
        }
        return user;
    }

    private String value(String s) {
        return s == null ? "" : s.trim();
    }

    private String normalizeDataType(String dataType) {
        String t = dataType.toLowerCase();
        if ("applications".equals(t) || "hired".equals(t) || "users".equals(t) || "logs".equals(t)) {
            return t;
        }
        return "positions";
    }

    private ExportPayload buildExportPayload(String dataType, String startDate, String endDate, String role) {
        List<String> columns = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();

        if ("positions".equals(dataType)) {
            columns.add("id");
            columns.add("title");
            columns.add("department");
            columns.add("moId");
            columns.add("openings");
            columns.add("appliedCount");
            columns.add("acceptedCount");
            columns.add("status");
            columns.add("createdAt");
            columns.add("deadline");

            for (Map<String, String> p : dataManager.getAllPositions()) {
                String createdAt = value(p.get("createdAt"));
                if (!inDateRange(createdAt, startDate, endDate, false)) {
                    continue;
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (String c : columns) {
                    row.put(c, value(p.get(c)));
                }
                rows.add(row);
            }
            return new ExportPayload(columns, rows);
        }

        if ("applications".equals(dataType)) {
            columns.add("id");
            columns.add("positionId");
            columns.add("positionTitle");
            columns.add("userId");
            columns.add("userName");
            columns.add("moId");
            columns.add("priority");
            columns.add("status");
            columns.add("appliedDate");
            columns.add("feedback");

            for (Map<String, String> a : dataManager.getAllApplications()) {
                if (!inDateRange(value(a.get("appliedDate")), startDate, endDate, false)) {
                    continue;
                }
                if (!role.isEmpty()) {
                    User u = dataManager.getUserById(value(a.get("userId")));
                    if (u == null || !role.equalsIgnoreCase(value(u.getRole()))) {
                        continue;
                    }
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (String c : columns) {
                    row.put(c, value(a.get(c)));
                }
                rows.add(row);
            }
            return new ExportPayload(columns, rows);
        }

        if ("hired".equals(dataType)) {
            columns.add("userId");
            columns.add("userName");
            columns.add("major");
            columns.add("positionId");
            columns.add("positionTitle");
            columns.add("moId");
            columns.add("appliedDate");

            for (Map<String, String> a : dataManager.getAllApplications()) {
                if (!"approved".equalsIgnoreCase(value(a.get("status")))) {
                    continue;
                }
                if (!inDateRange(value(a.get("appliedDate")), startDate, endDate, false)) {
                    continue;
                }
                User user = dataManager.getUserById(value(a.get("userId")));
                if (!role.isEmpty() && (user == null || !role.equalsIgnoreCase(value(user.getRole())))) {
                    continue;
                }
                Map<String, String> profile = dataManager.getProfile(value(a.get("userId")));
                Map<String, String> row = new LinkedHashMap<>();
                row.put("userId", value(a.get("userId")));
                row.put("userName", value(a.get("userName")));
                row.put("major", profile == null ? "" : value(profile.get("major")));
                row.put("positionId", value(a.get("positionId")));
                row.put("positionTitle", value(a.get("positionTitle")));
                row.put("moId", value(a.get("moId")));
                row.put("appliedDate", value(a.get("appliedDate")));
                rows.add(row);
            }
            return new ExportPayload(columns, rows);
        }

        if ("users".equals(dataType)) {
            columns.add("userId");
            columns.add("qmId");
            columns.add("userName");
            columns.add("email");
            columns.add("role");
            columns.add("status");

            for (User u : dataManager.getAllUsers()) {
                if (!role.isEmpty() && !role.equalsIgnoreCase(value(u.getRole()))) {
                    continue;
                }
                Map<String, String> row = new LinkedHashMap<>();
                row.put("userId", value(u.getUserId()));
                row.put("qmId", value(u.getQmId()));
                row.put("userName", value(u.getUserName()));
                row.put("email", value(u.getEmail()));
                row.put("role", value(u.getRole()));
                row.put("status", value(u.getStatus()));
                rows.add(row);
            }
            return new ExportPayload(columns, rows);
        }

        columns.add("id");
        columns.add("time");
        columns.add("userId");
        columns.add("userName");
        columns.add("role");
        columns.add("action");
        columns.add("detail");
        columns.add("result");
        for (Map<String, String> log : dataManager.getLogs()) {
            if (!inDateRange(value(log.get("time")), startDate, endDate, true)) {
                continue;
            }
            if (!role.isEmpty() && !role.equalsIgnoreCase(value(log.get("role")))) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (String c : columns) {
                row.put(c, value(log.get(c)));
            }
            rows.add(row);
        }
        return new ExportPayload(columns, rows);
    }

    private boolean inDateRange(String rawDate, String startDate, String endDate, boolean includeTime) {
        if ((startDate == null || startDate.isEmpty()) && (endDate == null || endDate.isEmpty())) {
            return true;
        }
        LocalDate date = parseDate(rawDate, includeTime);
        if (date == null) {
            return false;
        }
        LocalDate start = parseDate(startDate, false);
        LocalDate end = parseDate(endDate, false);
        if (start != null && date.isBefore(start)) {
            return false;
        }
        if (end != null && date.isAfter(end)) {
            return false;
        }
        return true;
    }

    private LocalDate parseDate(String text, boolean mayContainTime) {
        String value = value(text);
        if (value.isEmpty()) {
            return null;
        }

        try {
            if (mayContainTime && value.length() >= 19) {
                return LocalDateTime.parse(value.substring(0, 19), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toLocalDate();
            }
            if (value.length() >= 10) {
                return LocalDate.parse(value.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
        } catch (DateTimeParseException ignore) {
        }
        return null;
    }

    private String toCsv(List<String> columns, List<Map<String, String>> rows) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeCsv(columns.get(i)));
        }
        sb.append('\n');

        for (Map<String, String> row : rows) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(escapeCsv(value(row.get(columns.get(i)))));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        String v = value == null ? "" : value;
        boolean needsQuote = v.contains(",") || v.contains("\n") || v.contains("\r") || v.contains("\"");
        if (!needsQuote) {
            return v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static class ExportPayload {
        private final List<String> columns;
        private final List<Map<String, String>> rows;

        private ExportPayload(List<String> columns, List<Map<String, String>> rows) {
            this.columns = columns;
            this.rows = rows;
        }
    }
}
