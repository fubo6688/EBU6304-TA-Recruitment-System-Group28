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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Application workflow servlet.
 *
 * <p>Handles TA submissions, MO/Admin reviews, TA "my applications" queries
 * and review list retrieval. Enforces role-based permission checks and
 * ownership validations on critical paths.</p>
 */
public class ApplicationServlet extends HttpServlet {
    private final DataManager dataManager = new DataManager();
    private static final int MAX_ACTIVE_APPLICATIONS = 5;
    private static final int MAX_APPROVED_APPLICATIONS = 3;

    // 1. 在 ApplicationServlet 顶部引入或实例化 AI 客户端
private final com.ta.util.AiResumeAnalysisClient aiClient = new com.ta.util.AiResumeAnalysisClient();

/**
 * MO 查看申请时动态触发：读取解析好的简历文本，结合岗位信息进行 AI 分析并返回结果
 */
private JSONObject performAiAnalysisForApplication(String taUserId, String positionId) {
    if (!aiClient.isConfigured()) {
        return new JSONObject().put("error", "AI client is not configured");
    }

    try {
        // A. 读取 TA 之前已经由解析服务生成好的简历 JSON 文本
        java.nio.file.Path parsedDir = dataManager.getDataDirPath().resolve("resume_parsed");
        java.nio.file.Path parsedFile = parsedDir.resolve(taUserId.replaceAll("[\\\\/:*?\"<>|]", "_") + ".json");
        
        if (!java.nio.file.Files.exists(parsedFile)) {
            return new JSONObject().put("message", "TA 简历尚未解析完成");
        }
        String parsedResumeJson = new String(java.nio.file.Files.readAllBytes(parsedFile), java.nio.charset.StandardCharsets.UTF_8);

        // B. 获取当前 MO 查看的这个岗位的详细信息，用来做匹配度评估
        Map<String, String> position = dataManager.getPositionById(positionId);
        String positionContext = (position != null) 
            ? "岗位名称: " + position.get("title") + ", 部门: " + position.get("department") + ", 岗位要求: " + position.get("requirements")
            : "普通 TA 岗位";

        // C. 组装简短的分析要求，简历正文由 AiResumeAnalysisClient 统一注入，避免重复发送。
        String prompt = "你是一位大学 TA 招聘专家。请结合岗位要求进行匹配度分析，并严格返回 JSON。"
            + "输出字段必须只有这三个：matching_score（0-100数字）、core_skills（字符串数组）、evaluation（简短文本，尽量不超过 80 字）。\n\n"
            + "[岗位要求]: " + positionContext;

        // D. 调用大模型分析
        com.ta.util.AiResumeAnalysisClient.AnalysisResult res = aiClient.analyzeResume(parsedResumeJson, prompt);
        if (res.ok && !res.body.isEmpty()) {
            // 将 AI 分析结果持久化到单独的目录中，以 "taUserId_positionId.json" 命名，避免跨岗覆盖
            java.nio.file.Path aiDir = dataManager.getDataDirPath().resolve("resume_ai_matches");
            java.nio.file.Files.createDirectories(aiDir);
            java.nio.file.Path aiFile = aiDir.resolve(taUserId.replaceAll("[\\\\/:*?\"<>|]", "_") + "_" + positionId + ".json");
            
            java.nio.file.Files.write(aiFile, res.body.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            
            return new JSONObject(res.body);
        } else {
            return new JSONObject().put("error", "AI 分析调用失败: " + res.errorMessage);
        }
    } catch (Exception e) {
        return new JSONObject().put("error", "处理异常: " + e.getMessage());
    }
}
    /**
     * 处理申请查询类 GET 接口（审核列表、我的申请）。
     */
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

// 仅回传已缓存的 AI 分析结果，不再在列表加载时自动触发分析。
List<Map<String, Object>> enrichedApps = new ArrayList<>();
for (Map<String, String> app : apps) {
    Map<String, Object> item = new java.util.LinkedHashMap<>(app);
    String taUserId = value(app.get("userId"));
    String posId = value(app.get("positionId"));

    if (!taUserId.isEmpty() && !posId.isEmpty()) {
        java.nio.file.Path aiFile = dataManager.getDataDirPath()
                .resolve("resume_ai_matches")
                .resolve(taUserId.replaceAll("[\\\\/:*?\"<>|]", "_") + "_" + posId + ".json");

        JSONObject aiResultJson = null;
        if (java.nio.file.Files.exists(aiFile)) {
            try {
                String cacheContent = new String(java.nio.file.Files.readAllBytes(aiFile), java.nio.charset.StandardCharsets.UTF_8);
                aiResultJson = new JSONObject(cacheContent);
            } catch (Exception ignore) {
            }
        }
        item.put("aiAnalysis", aiResultJson);
    } else {
        item.put("aiAnalysis", null);
    }
    enrichedApps.add(item);
}

out.print(new JSONObject().put("success", true).put("applications", new JSONArray(enrichedApps)).toString());
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

    /**
     * 处理申请写操作 POST 接口（提交、审核）。
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

        if ("/submit".equalsIgnoreCase(path)) {
            // 只有 TA 能提交申请。
            if (!isRole(user, "TA")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            Map<String, String> profile = getProfileForApply(user);
            if (!isProfileCompleteForApply(profile)) {
                out.print(new JSONObject()
                        .put("success", false)
                        .put("message", "Please complete your profile (major, email, skills, available time, resume) before applying")
                        .toString());
                return;
            }

            int approvedCount = dataManager.countApprovedApplicationsForUser(user.getUserId());
            if (approvedCount >= MAX_APPROVED_APPLICATIONS) {
                List<Map<String, String>> autoRejected = dataManager.rejectPendingApplicationsForUser(
                        user.getUserId(),
                        "",
                        "Auto rejected: reached accepted quota (max 3).");

                if (!autoRejected.isEmpty()) {
                    dataManager.saveNotification(
                            user.getUserId(),
                            "application",
                            "Pending Applications Auto Rejected",
                            autoRejected.size() + " pending application(s) were auto rejected because you already have 3 accepted positions.");

                    Set<String> notifiedMoUserIds = new LinkedHashSet<>();
                    for (Map<String, String> rejected : autoRejected) {
                        String moId = value(rejected.get("moId"));
                        notifyMoUsers(
                                moId,
                                "application",
                                "Application Auto Rejected",
                                "A pending application was auto rejected because the TA has reached the accepted quota.",
                                notifiedMoUserIds);
                    }
                }

                out.print(new JSONObject()
                        .put("success", false)
                        .put("message", "You already have 3 accepted positions and cannot submit new applications")
                        .toString());
                return;
            }

            int activeCount = dataManager.countActiveApplicationsForUser(user.getUserId());
            if (activeCount >= MAX_ACTIVE_APPLICATIONS) {
                out.print(new JSONObject()
                        .put("success", false)
                        .put("message", "You can hold at most 5 active applications (pending + approved)")
                        .toString());
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
            notifyMoUsers(value(app.get("moId")), "application", "New Application", "A new application needs review.");
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

            String taUserId = value(app.get("userId"));
            boolean tryApprove = "accept".equals(normalizedDecision)
                    || "accepted".equals(normalizedDecision)
                    || "approve".equals(normalizedDecision)
                    || "approved".equals(normalizedDecision);
            if (tryApprove && !"approved".equalsIgnoreCase(value(app.get("status")))) {
                int approvedCount = dataManager.countApprovedApplicationsForUser(taUserId);
                if (approvedCount >= MAX_APPROVED_APPLICATIONS) {
                    out.print(new JSONObject()
                            .put("success", false)
                            .put("message", "This TA already has 3 accepted positions. Cannot approve more applications")
                            .toString());
                    return;
                }
            }

            boolean ok = dataManager.processApplication(applicationId, decision, feedback);
            if (!ok) {
                out.print(new JSONObject().put("success", false).put("message", "Failed to update application").toString());
                return;
            }

            Map<String, String> updated = dataManager.getApplicationById(applicationId);
            int autoRejectedCount = 0;
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

                if ("approved".equalsIgnoreCase(status)
                        && dataManager.countApprovedApplicationsForUser(taUserId) >= MAX_APPROVED_APPLICATIONS) {
                    List<Map<String, String>> autoRejected = dataManager.rejectPendingApplicationsForUser(
                            taUserId,
                            applicationId,
                            "Auto rejected: reached accepted quota (max 3).");
                    autoRejectedCount = autoRejected.size();

                    if (!autoRejected.isEmpty()) {
                        dataManager.saveNotification(
                                taUserId,
                                "application",
                                "Pending Applications Auto Rejected",
                                autoRejected.size() + " pending application(s) were auto rejected because you already have 3 accepted positions.");

                        Set<String> notifiedMoUserIds = new LinkedHashSet<>();
                        for (Map<String, String> rejected : autoRejected) {
                            String moId = value(rejected.get("moId"));
                            notifyMoUsers(
                                    moId,
                                    "application",
                                    "Application Auto Rejected",
                                    "A pending application was auto rejected because the TA has reached the accepted quota.",
                                    notifiedMoUserIds);
                        }
                    }
                }
            }
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "REVIEW_APPLICATION", applicationId + " -> " + decision, "success");

            out.print(new JSONObject()
                    .put("success", true)
                    .put("message", "Application updated")
                    .put("application", new JSONObject(updated))
                    .put("autoRejectedCount", autoRejectedCount)
                    .toString());
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    /**
     * 过滤出当前 MO 可管理的申请集合。
     */
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

    /**
     * 判断申请是否归属于当前 MO（兼容 userId/qmId）。
     */
    private boolean isMoOwner(User user, String moId) {
        // moId 既可能存用户账号，也可能存 qmId，两者都视为归属。
        return eq(moId, user.getUserId()) || eq(moId, user.getQmId());
    }

    /**
     * 统一登录态校验：要求已登录且账号 active。
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
            // 被管理员停用后，旧会话访问会被立即失效。
            session.invalidate();
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(new JSONObject().put("success", false).put("message", "Account is inactive").toString());
            return null;
        }
        return user;
    }

    /**
     * 判断当前用户角色是否命中给定角色集合。
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
     * 忽略大小写比较两个字符串是否相等。
     */
    private boolean eq(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    /**
     * 按 moId（兼容 userId/qmId）向对应 MO 账号发送通知。
     */
    private void notifyMoUsers(String moId, String type, String title, String message) {
        notifyMoUsers(moId, type, title, message, null);
    }

    /**
     * 发送 MO 通知并可选按 userId 去重，避免同一批次重复提醒。
     */
    private void notifyMoUsers(String moId,
                               String type,
                               String title,
                               String message,
                               Set<String> dedupeUserIds) {
        String target = value(moId);
        Set<String> moUserIds = dataManager.resolveMoNotificationUserIds(target);

        if (moUserIds.isEmpty() && !target.isEmpty()) {
            // 兜底：未知标识时按原始值写入，兼容历史脏数据场景。
            if (dedupeUserIds == null || dedupeUserIds.add(target)) {
                dataManager.saveNotification(target, type, title, message);
            }
            return;
        }

        for (String moUserId : moUserIds) {
            if (moUserId.isEmpty()) {
                continue;
            }
            if (dedupeUserIds != null && !dedupeUserIds.add(moUserId)) {
                continue;
            }
            dataManager.saveNotification(moUserId, type, title, message);
        }
    }

    /**
     * 获取投递校验所需档案，兼容 userId 与 qmId 双键。
     */
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

    /**
     * 投递前档案完整性判断：基础信息、技能与可用时间、简历均需存在。
     */
    private boolean isProfileCompleteForApply(Map<String, String> profile) {
        if (profile == null) {
            return false;
        }

        String major = value(profile.get("major"));
        String email = value(profile.get("email"));
        String skills = value(profile.get("skills"));
        String availableTime = value(profile.get("availableTime"));
        String resumeFileName = value(profile.get("resumeFileName"));
        String resumeStoredName = value(profile.get("resumeStoredName"));

        return !major.isEmpty()
                && !email.isEmpty()
                && !skills.isEmpty()
                && !availableTime.isEmpty()
                && (!resumeFileName.isEmpty() || !resumeStoredName.isEmpty());
    }

    /**
     * 空值安全取值并去首尾空白。
     */
    private String value(String s) {
        return s == null ? "" : s.trim();
    }

}
