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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class AdminServlet extends HttpServlet {
    private final DataManager dataManager = new DataManager();

    // 统一处理管理员 GET 接口：根据 path 分发看板、岗位列表、工作量等查询能力。
    // 实现方式是先做管理员权限校验，再按子路径组织并返回 JSON 结构。
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        User user = requireAdmin(req, resp, out);
        if (user == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/dashboard".equalsIgnoreCase(path) || path.isEmpty() || "/".equals(path)) {
            List<Map<String, String>> positions = dataManager.getAllPositions();
            List<Map<String, Object>> taWorkload = dataManager.getTaWorkloadSummary();

            List<Map<String, String>> applications = dataManager.getAllApplications();
            int pending = 0;
            int approved = 0;
            int rejected = 0;
            for (Map<String, String> app : applications) {
                String status = value(app.get("status")).toLowerCase();
                if ("approved".equals(status)) {
                    approved++;
                } else if ("rejected".equals(status)) {
                    rejected++;
                } else if ("canceled".equals(status)) {
                    // skip
                } else {
                    pending++;
                }
            }

            int openPositions = 0;
            int closedPositions = 0;
            for (Map<String, String> p : positions) {
                if ("closed".equalsIgnoreCase(value(p.get("status")))) {
                    closedPositions++;
                } else {
                    openPositions++;
                }
            }

            positions.sort(Comparator.comparing((Map<String, String> p) -> value(p.get("createdAt"))).reversed());

            JSONObject summary = new JSONObject();
            summary.put("totalPositions", positions.size());
            summary.put("openPositions", openPositions);
            summary.put("closedPositions", closedPositions);
            summary.put("totalApplications", applications.size());
            summary.put("pendingApplications", pending);
            summary.put("approvedApplications", approved);
            summary.put("rejectedApplications", rejected);

            out.print(new JSONObject()
                    .put("success", true)
                    .put("summary", summary)
                    .put("positions", new JSONArray(positions))
                    .put("taWorkload", new JSONArray(taWorkload))
                    .toString());
            return;
        }

        if ("/positions".equalsIgnoreCase(path)) {
            out.print(new JSONObject().put("success", true).put("positions", new JSONArray(dataManager.getAllPositions())).toString());
            return;
        }

        if ("/ta-workload".equalsIgnoreCase(path)) {
            out.print(new JSONObject().put("success", true).put("taWorkload", new JSONArray(dataManager.getTaWorkloadSummary())).toString());
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    // 校验当前会话是否为 Admin。
    // 若未登录、账号不存在或角色不符，会直接写入错误响应并返回 null。
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

        if (!"Admin".equalsIgnoreCase(value(user.getRole()))) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print(new JSONObject().put("success", false).put("message", "Admin only").toString());
            return null;
        }

        return user;
    }

    // 安全取值工具：把 null 统一转为空字符串并去除首尾空白，减少空指针与比较分支。
    private String value(String s) {
        return s == null ? "" : s.trim();
    }
}
