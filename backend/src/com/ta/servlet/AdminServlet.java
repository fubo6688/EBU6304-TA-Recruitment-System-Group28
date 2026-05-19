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
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 管理员后台聚合接口。
 *
 * <p>提供 dashboard、positions、ta-workload 等只读管理视图数据，
 * 并在入口处统一校验会话与 Admin 角色。</p>
 */
public class AdminServlet extends HttpServlet {
    private final DataManager dataManager = new DataManager();

    /**
     * 处理管理员 GET 接口：dashboard、positions、ta-workload。
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // Admin 专属接口统一入口校验。
        User user = requireAdmin(req, resp, out);
        if (user == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/dashboard".equalsIgnoreCase(path) || path.isEmpty() || "/".equals(path)) {
            // 仪表盘聚合：岗位、申请、TA 工作负载。
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

            // 最新创建的岗位优先展示。
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
            // 提供原始岗位列表给管理员页面使用。
            out.print(new JSONObject().put("success", true).put("positions", new JSONArray(dataManager.getAllPositions())).toString());
            return;
        }

        if ("/ta-workload".equalsIgnoreCase(path)) {
            // 提供 TA 负载统计独立接口，便于前端按需刷新。
            out.print(new JSONObject().put("success", true).put("taWorkload", new JSONArray(dataManager.getTaWorkloadSummary())).toString());
            return;
        }

        // 导出招聘数据（CSV）
        if ("/export".equalsIgnoreCase(path)) {
            String scope = value(req.getParameter("scope")); // applications / hires / positions / workload
            String startDate = value(req.getParameter("startDate")); // yyyy-MM-dd
            String endDate = value(req.getParameter("endDate"));

            // 默认导出 applications
            if (scope.isEmpty()) scope = "applications";

            // Prepare CSV response
            resp.setContentType("text/csv;charset=UTF-8");
            String filename = "recruitment-" + scope + "-" + System.currentTimeMillis() + ".csv";
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            PrintWriter csv = resp.getWriter();

            try {
                LocalDate start = null;
                LocalDate end = null;
                try {
                    if (!startDate.isEmpty()) start = LocalDate.parse(startDate);
                } catch (DateTimeParseException ignore) {
                }
                try {
                    if (!endDate.isEmpty()) end = LocalDate.parse(endDate);
                } catch (DateTimeParseException ignore) {
                }

                // simple CSV escape
                java.util.function.Function<String, String> esc = (v) -> {
                    if (v == null) return "";
                    String s = v.replace("\"", "\"\"");
                    return "\"" + s + "\"";
                };

                if ("applications".equalsIgnoreCase(scope) || "hires".equalsIgnoreCase(scope)) {
                    csv.println("ApplicationId,PositionId,PositionTitle,UserId,UserName,MOId,Priority,Status,AppliedDate,Feedback");
                    List<Map<String, String>> apps = dataManager.getAllApplications();
                    for (Map<String, String> a : apps) {
                        String appliedDate = value(a.get("appliedDate"));
                        if (start != null || end != null) {
                            try {
                                LocalDate d = appliedDate.isEmpty() ? null : LocalDate.parse(appliedDate);
                                if (d == null) continue;
                                if (start != null && d.isBefore(start)) continue;
                                if (end != null && d.isAfter(end)) continue;
                            } catch (DateTimeParseException ex) {
                                // skip unparsable
                                continue;
                            }
                        }

                        if ("hires".equalsIgnoreCase(scope) && !"approved".equalsIgnoreCase(value(a.get("status")))) {
                            continue;
                        }

                        csv.println(String.join(",",
                                esc.apply(value(a.get("id"))),
                                esc.apply(value(a.get("positionId"))),
                                esc.apply(value(a.get("positionTitle"))),
                                esc.apply(value(a.get("userId"))),
                                esc.apply(value(a.get("userName"))),
                                esc.apply(value(a.get("moId"))),
                                esc.apply(value(a.get("priority"))),
                                esc.apply(value(a.get("status"))),
                                esc.apply(appliedDate),
                                esc.apply(value(a.get("feedback")))
                        ));
                    }
                } else if ("positions".equalsIgnoreCase(scope)) {
                    csv.println("PositionId,Title,Department,MOId,Openings,AppliedCount,AcceptedCount,Status,CreatedAt,Deadline");
                    List<Map<String, String>> positions = dataManager.getAllPositions();
                    for (Map<String, String> p : positions) {
                        String created = value(p.get("createdAt"));
                        if (start != null || end != null) {
                            try {
                                LocalDate d = created.isEmpty() ? null : LocalDate.parse(created);
                                if (d == null) continue;
                                if (start != null && d.isBefore(start)) continue;
                                if (end != null && d.isAfter(end)) continue;
                            } catch (DateTimeParseException ex) {
                                continue;
                            }
                        }
                        csv.println(String.join(",",
                                esc.apply(value(p.get("id"))),
                                esc.apply(value(p.get("title"))),
                                esc.apply(value(p.get("department"))),
                                esc.apply(value(p.get("moId"))),
                                esc.apply(value(p.get("openings"))),
                                esc.apply(value(p.get("appliedCount"))),
                                esc.apply(value(p.get("acceptedCount"))),
                                esc.apply(value(p.get("status"))),
                                esc.apply(created),
                                esc.apply(value(p.get("deadline")))
                        ));
                    }
                } else if ("workload".equalsIgnoreCase(scope)) {
                    csv.println("UserId,UserName,QMId,Status,TotalApplications,Pending,Approved,Rejected,Canceled,CurrentLoad");
                    List<Map<String, Object>> w = dataManager.getTaWorkloadSummary();
                    for (Map<String, Object> r : w) {
                        csv.println(String.join(",",
                                esc.apply(String.valueOf(r.getOrDefault("userId", ""))),
                                esc.apply(String.valueOf(r.getOrDefault("userName", ""))),
                                esc.apply(String.valueOf(r.getOrDefault("qmId", ""))),
                                esc.apply(String.valueOf(r.getOrDefault("status", ""))),
                                esc.apply(String.valueOf(r.getOrDefault("totalApplications", "0"))),
                                esc.apply(String.valueOf(r.getOrDefault("pending", "0"))),
                                esc.apply(String.valueOf(r.getOrDefault("approved", "0"))),
                                esc.apply(String.valueOf(r.getOrDefault("rejected", "0"))),
                                esc.apply(String.valueOf(r.getOrDefault("canceled", "0"))),
                                esc.apply(String.valueOf(r.getOrDefault("currentLoad", "0")))
                        ));
                    }
                } else {
                    csv.println("Unsupported scope");
                }

                // 写入操作日志
                dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "exportRecruitmentData", scope + " " + startDate + "->" + endDate, "success");
            } catch (Exception ex) {
                dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "exportRecruitmentData", scope + " " + startDate + "->" + endDate + " : " + ex.getMessage(), "failed");
                throw new ServletException(ex);
            }
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    /**
     * 统一 Admin 权限校验：未登录/停用/非管理员均拦截。
     */
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

        if (!"active".equalsIgnoreCase(value(user.getStatus()))) {
            // 停用管理员账号后，旧会话不得继续访问管理接口。
            session.invalidate();
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(new JSONObject().put("success", false).put("message", "Account is inactive").toString());
            return null;
        }

        if (!"Admin".equalsIgnoreCase(value(user.getRole()))) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print(new JSONObject().put("success", false).put("message", "Admin only").toString());
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
