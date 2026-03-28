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

        if ("/status".equalsIgnoreCase(path)) {
            if (!isRole(user, "MO", "Admin")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            String positionId = value(req.getParameter("positionId"));
            String status = value(req.getParameter("status"));
            if (positionId.isEmpty() || status.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Missing parameters").toString());
                return;
            }

            boolean ok = dataManager.updatePositionStatus(positionId, status);
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "UPDATE_POSITION_STATUS", positionId + "->" + status, ok ? "success" : "failed");
            out.print(new JSONObject().put("success", ok).put("message", ok ? "Status updated" : "Position not found").toString());
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

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

    private boolean eq(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }
}
