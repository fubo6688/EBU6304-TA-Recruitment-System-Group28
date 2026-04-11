package com.ta.servlet;

import com.ta.model.User;
import com.ta.util.DataManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 15 * 1024 * 1024)
public class UserServlet extends HttpServlet {
    private final DataManager dataManager = new DataManager();
    // 允许查看 TA 资料的角色集合。
    // TA 只能看自己的资料，MO 和 Admin 可以看他人的 TA 资料用于审核和管理。
    private static final Set<String> PROFILE_VIEW_ROLES = new HashSet<>();

    static {
        PROFILE_VIEW_ROLES.add("MO");
        PROFILE_VIEW_ROLES.add("Admin");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 所有 GET 接口统一输出 JSON，前端可以直接按 JSON 解析。
        resp.setContentType("application/json;charset=UTF-8");

        // 先做登录校验，未登录直接返回 401，避免后面每个分支重复判断。
        User user = requireLogin(req, resp);
        if (user == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/pending-registrations".equalsIgnoreCase(path)) {
            // Admin 才能查看待审批账号列表。
            if (!isAdmin(user)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "Admin only").toString());
                return;
            }

            // 只筛选 pending 状态的 TA/MO 账号，Admin 账号本身不进入审批列表。
            JSONArray items = new JSONArray();
            for (User item : dataManager.getAllUsers()) {
                if (!"pending".equalsIgnoreCase(item.getStatus())) {
                    continue;
                }
                if (!"TA".equalsIgnoreCase(item.getRole()) && !"MO".equalsIgnoreCase(item.getRole())) {
                    continue;
                }
                items.put(toUserJson(item));
            }

            resp.getWriter().print(new JSONObject().put("success", true).put("users", items).toString());
            return;
        }

        if ("/avatar".equalsIgnoreCase(path)) {
            // 头像读取：默认读当前登录用户；如果传了 userId，则尝试读取指定用户头像。
            String targetUserId = value(req.getParameter("userId"));
            if (targetUserId.isEmpty()) {
                targetUserId = user.getUserId();
            }

            // 先校验权限，防止普通 TA 读取别人的头像。
            if (!canViewProfile(user, targetUserId)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            // 先查 profile，再根据 profile 中保存的头像文件名定位磁盘文件。
            ResolvedProfile resolved = resolveProfileByAnyId(targetUserId);
            Map<String, String> profile = resolved.profile;
            if (profile == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "Profile not found").toString());
                return;
            }

            String storedName = value(profile.get("avatarStoredName"));
            if (storedName.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "Avatar not found").toString());
                return;
            }

            Path file = resolveAvatarFile(resolved.resolvedUserId, storedName);
            if (!Files.exists(file)) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "Avatar file missing").toString());
                return;
            }

            String contentType = Files.probeContentType(file);
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream";
            }
            resp.setContentType(contentType);
            resp.setContentLengthLong(Files.size(file));

            try (InputStream in = Files.newInputStream(file); OutputStream os = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
                os.flush();
            }
            return;
        }

        if ("/resume".equalsIgnoreCase(path)) {
            // 简历读取逻辑和头像类似：先校验权限，再定位 PDF 文件并流式返回。
            String targetUserId = value(req.getParameter("userId"));
            if (targetUserId.isEmpty()) {
                targetUserId = user.getUserId();
            }

            // TA 只能看自己的简历；MO/Admin 可以用于审核查看。
            if (!canViewProfile(user, targetUserId)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            // 兼容旧数据和新数据：有的记录存原文件名，有的记录存固定文件名。
            ResolvedProfile resolved = resolveProfileByAnyId(targetUserId);
            Map<String, String> profile = resolved.profile;
            if (profile == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "Profile not found").toString());
                return;
            }

            String storedName = value(profile.get("resumeStoredName"));
            String originalName = value(profile.get("resumeFileName"));
            if (storedName.isEmpty()) {
                storedName = originalName;
            }
            if (storedName.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "Resume not found").toString());
                return;
            }

            Path file = resolveResumeFile(resolved.resolvedUserId, storedName, originalName);
            if (!Files.exists(file)) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "Resume file missing").toString());
                return;
            }

            String downloadName = originalName.isEmpty() ? storedName : originalName;
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition", "inline; filename=\"" + sanitizeFileName(downloadName) + "\"");
            resp.setContentLengthLong(Files.size(file));

            try (InputStream in = Files.newInputStream(file); OutputStream os = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
                os.flush();
            }
            return;
        }
        if ("/profile".equalsIgnoreCase(path) || path.isEmpty() || "/".equals(path)) {
            // GET /api/user/profile
            // 返回当前登录用户的“账号信息 + 资料信息”合并对象。
            // TA 资料页会用这个接口回填表单，方便断点继续填写。
            JSONObject result = toUserJson(user);
            Map<String, String> profile = dataManager.getProfile(user.getUserId());
            if (profile != null) {
                result.put("grade", profile.get("grade"));
                result.put("major", profile.get("major"));
                result.put("gpa", profile.get("gpa"));
                result.put("skills", profile.get("skills"));
                result.put("resumeFileName", profile.get("resumeFileName"));
                result.put("availableTime", profile.get("availableTime"));
                result.put("profileUpdatedAt", profile.get("updatedAt"));
                String avatarStoredName = value(profile.get("avatarStoredName"));
                result.put("avatarUrl", buildAvatarUrl(req, user.getUserId(), avatarStoredName));
                if (result.optString("email").isEmpty() && profile.get("email") != null) {
                    result.put("email", profile.get("email"));
                }
            }
            resp.getWriter().print(result.toString());
            return;
        }

        if ("/ta-profile".equalsIgnoreCase(path)) {
            // GET /api/user/ta-profile?userId=...
            // 这是跨账号查看接口，专门给 MO/Admin 审核 TA 时使用。
            // 返回 TA 资料和简历预览地址，前端可以直接打开查看。
            String targetUserId = value(req.getParameter("userId"));
            if (targetUserId.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "Missing userId").toString());
                return;
            }
            if (!canViewProfile(user, targetUserId)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

            User targetUser = dataManager.getUserById(targetUserId);
            ResolvedProfile resolved = resolveProfileByAnyId(targetUserId);
            if (targetUser == null) {
                User mapped = findUserByQmId(targetUserId);
                if (mapped != null) {
                    targetUser = mapped;
                }
            }

            if (targetUser == null && resolved.profile == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "User not found").toString());
                return;
            }

            JSONObject result = targetUser == null
                    ? new JSONObject().put("userId", targetUserId).put("userName", targetUserId).put("role", "TA").put("userRole", "TA")
                    : toUserJson(targetUser);
            Map<String, String> profile = resolved.profile;
            if (profile != null) {
                result.put("grade", profile.get("grade"));
                result.put("major", profile.get("major"));
                result.put("gpa", profile.get("gpa"));
                result.put("skills", profile.get("skills"));
                result.put("resumeFileName", profile.get("resumeFileName"));
                result.put("availableTime", profile.get("availableTime"));
                result.put("profileUpdatedAt", profile.get("updatedAt"));
                String avatarStoredName = value(profile.get("avatarStoredName"));
                result.put("avatarUrl", buildAvatarUrl(req, targetUserId, avatarStoredName));
                if (result.optString("email").isEmpty() && profile.get("email") != null) {
                    result.put("email", profile.get("email"));
                }
            }
            result.put("resumePreviewUrl", req.getContextPath() + "/api/user/resume?userId=" + targetUserId);
            resp.getWriter().print(new JSONObject().put("success", true).put("profile", result).toString());
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.getWriter().print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 所有 POST 接口统一输出 JSON，前端处理时不用区分文本和二进制。
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // POST 接口同样先检查登录态。
        User user = requireLogin(req, resp);
        if (user == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/approve-registration".equalsIgnoreCase(path)) {
            // Admin 审批注册请求：approve -> active，reject -> inactive。
            if (!isAdmin(user)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(new JSONObject().put("success", false).put("message", "Admin only").toString());
                return;
            }

            // targetUserId 是待审批账号；decision 只允许 approve / reject。
            String targetUserId = value(req.getParameter("userId"));
            String decision = value(req.getParameter("decision"));
            if (targetUserId.isEmpty() || decision.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Missing userId or decision").toString());
                return;
            }

            User target = dataManager.getUserById(targetUserId);
            if (target == null) {
                out.print(new JSONObject().put("success", false).put("message", "Target user not found").toString());
                return;
            }
            if (!"pending".equalsIgnoreCase(target.getStatus())) {
                out.print(new JSONObject().put("success", false).put("message", "Target user is not pending").toString());
                return;
            }

            String nextStatus;
            if ("approve".equalsIgnoreCase(decision) || "approved".equalsIgnoreCase(decision)) {
                nextStatus = "active";
            } else if ("reject".equalsIgnoreCase(decision) || "rejected".equalsIgnoreCase(decision)) {
                nextStatus = "inactive";
            } else {
                out.print(new JSONObject().put("success", false).put("message", "Invalid decision").toString());
                return;
            }

            target.setStatus(nextStatus);
            dataManager.saveUser(target);
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "APPROVE_REGISTRATION", targetUserId + " -> " + nextStatus, "success");
            out.print(new JSONObject().put("success", true).put("message", "Registration updated").put("status", nextStatus).toString());
            return;
        }

        if ("/profile".equalsIgnoreCase(path) || path.isEmpty() || "/".equals(path)) {
            req.setCharacterEncoding("UTF-8");
            // POST /api/user/profile
            // 这是 TA 资料页的提交入口，支持 multipart 表单。
            // 如果某些字段这次没填，代码会自动沿用旧值，避免编辑一步丢一步。
            String userName = value(req.getParameter("userName"));
            String email = value(req.getParameter("email"));
            String grade = value(req.getParameter("grade"));
            String major = value(req.getParameter("major"));
            String gpa = value(req.getParameter("gpa"));
            String skills = value(req.getParameter("skills"));
            String availableTime = value(req.getParameter("availableTime"));
            String resumeFileName = value(req.getParameter("resumeFileName"));

            Map<String, String> oldProfile = dataManager.getProfile(user.getUserId());
            String resumeStoredName = oldProfile == null ? "" : value(oldProfile.get("resumeStoredName"));
            String avatarStoredName = oldProfile == null ? "" : value(oldProfile.get("avatarStoredName"));

            // 如果资料已经存在，就把空字段补回旧值，更新时不会把已有内容覆盖掉。
            if (oldProfile != null) {
                if (grade.isEmpty()) {
                    grade = value(oldProfile.get("grade"));
                }
                if (major.isEmpty()) {
                    major = value(oldProfile.get("major"));
                }
                if (gpa.isEmpty()) {
                    gpa = value(oldProfile.get("gpa"));
                }
                if (email.isEmpty()) {
                    email = value(oldProfile.get("email"));
                }
                if (skills.isEmpty()) {
                    skills = value(oldProfile.get("skills"));
                }
                if (resumeFileName.isEmpty()) {
                    resumeFileName = value(oldProfile.get("resumeFileName"));
                }
                if (availableTime.isEmpty()) {
                    availableTime = value(oldProfile.get("availableTime"));
                }
            }

            String contentType = req.getContentType();
            if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
                try {
                    // 头像上传：只允许图片，且限制 5MB，避免上传错误文件或过大文件。
                    Part avatarPart = req.getPart("avatarFile");
                    if (avatarPart != null && avatarPart.getSize() > 0) {
                        if (avatarPart.getSize() > 5L * 1024L * 1024L) {
                            out.print(new JSONObject().put("success", false).put("message", "Avatar image size cannot exceed 5MB").toString());
                            return;
                        }

                        String avatarContentType = value(avatarPart.getContentType()).toLowerCase(Locale.ROOT);
                        if (!avatarContentType.startsWith("image/")) {
                            out.print(new JSONObject().put("success", false).put("message", "Only image avatar is allowed").toString());
                            return;
                        }

                        String submittedAvatar = sanitizeFileName(value(avatarPart.getSubmittedFileName()));
                        String avatarExt = guessAvatarExtension(submittedAvatar, avatarContentType);
                        Path avatarDir = dataManager.getDataDirPath().resolve("avatars");
                        Files.createDirectories(avatarDir);

                        String storedAvatar = user.getUserId() + "_" + System.currentTimeMillis() + avatarExt;
                        Path targetAvatar = avatarDir.resolve(storedAvatar);
                        try (InputStream in = avatarPart.getInputStream()) {
                            Files.copy(in, targetAvatar, StandardCopyOption.REPLACE_EXISTING);
                        }
                        avatarStoredName = storedAvatar;
                    }

                    // 简历上传：只允许 PDF，并且固定存成 userId.pdf，方便覆盖更新和后续预览。
                    Part part = req.getPart("resumeFile");
                    if (part != null && part.getSize() > 0) {
                        // 存储策略：
                        // - 真实磁盘文件名统一改为 {userId}.pdf，保证每次上传都覆盖旧简历；
                        // - 原始文件名保留在数据库中，供前端展示。
                        String submitted = value(part.getSubmittedFileName());
                        String lower = submitted.toLowerCase();
                        if (!lower.endsWith(".pdf")) {
                            out.print(new JSONObject().put("success", false).put("message", "Only PDF resume is allowed").toString());
                            return;
                        }

                        Path resumeDir = dataManager.getDataDirPath().resolve("resumes");
                        Files.createDirectories(resumeDir);
                        String safeOriginal = sanitizeFileName(submitted);
                        String stored = sanitizeFileName(user.getUserId()) + ".pdf";
                        Path target = resumeDir.resolve(stored);

                        try (InputStream in = part.getInputStream()) {
                            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                        }

                        resumeFileName = safeOriginal;
                        resumeStoredName = stored;
                    }
                } catch (Exception e) {
                    out.print(new JSONObject().put("success", false).put("message", "Resume upload failed").toString());
                    return;
                }
            }

            if (!userName.isEmpty()) {
                user.setUserName(userName);
            }
            if (!email.isEmpty()) {
                user.setEmail(email);
            }
            dataManager.saveUser(user);
            dataManager.saveProfile(
                    user.getUserId(),
                    grade,
                    major,
                    gpa,
                    email.isEmpty() ? user.getEmail() : email,
                    skills,
                    resumeFileName,
                    resumeStoredName,
                        availableTime,
                        avatarStoredName);
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "UPDATE_PROFILE", "profile", "success");

            HttpSession session = req.getSession(false);
            if (session != null) {
                session.setAttribute("userName", user.getUserName());
                session.setAttribute("currentUser", user);
            }

            out.print(new JSONObject()
                    .put("success", true)
                    .put("message", "Profile updated")
                    .put("resumeFileName", resumeFileName)
                    .put("availableTime", availableTime)
                    .put("avatarUrl", buildAvatarUrl(req, user.getUserId(), avatarStoredName))
                    .put("user", toUserJson(user))
                    .toString());
            return;
        }

        if ("/password".equalsIgnoreCase(path)) {
            // 修改密码：必须先校验旧密码，再校验新密码复杂度。
            String oldPassword = value(req.getParameter("oldPassword"));
            String newPassword = value(req.getParameter("newPassword"));
            if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                out.print(new JSONObject().put("success", false).put("message", "Password is empty").toString());
                return;
            }
            if (!oldPassword.equals(user.getPassword())) {
                dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "CHANGE_PASSWORD", "wrong old password", "failed");
                out.print(new JSONObject().put("success", false).put("message", "Old password is incorrect").toString());
                return;
            }
            if (!isPasswordComplex(newPassword)) {
                out.print(new JSONObject().put("success", false)
                        .put("message", "Password must be at least 8 chars with uppercase, lowercase, digit, and letters/digits only")
                        .toString());
                return;
            }
            user.setPassword(newPassword);
            dataManager.saveUser(user);
            dataManager.writeLog(user.getUserId(), user.getUserName(), user.getRole(), "CHANGE_PASSWORD", "password", "success");
            out.print(new JSONObject().put("success", true).put("message", "Password updated").toString());
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print(new JSONObject().put("success", false).put("message", "Unsupported endpoint").toString());
    }

    private User requireLogin(HttpServletRequest req, HttpServletResponse resp) {
        // 统一登录校验：没有 session 或 session 里没有 userId，就视为未登录。
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            try {
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "Not logged in").toString());
            } catch (IOException ignore) {
            }
            return null;
        }
        String userId = String.valueOf(session.getAttribute("userId"));
        User user = dataManager.getUserById(userId);
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            try {
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "User not found").toString());
            } catch (IOException ignore) {
            }
            return null;
        }
        return user;
    }

    private JSONObject toUserJson(User user) {
        // 把 User 对象转换成前端更容易直接消费的 JSON 结构。
        return new JSONObject()
                .put("userId", user.getUserId())
                .put("userName", user.getUserName())
                .put("email", user.getEmail())
                .put("role", user.getRole())
                .put("userRole", user.getRole())
                .put("qmId", user.getQmId())
                .put("status", user.getStatus());
    }

    private String value(String s) {
        return s == null ? "" : s.trim();
    }

    private boolean isAdmin(User user) {
        return user != null && "Admin".equalsIgnoreCase(value(user.getRole()));
    }

    private boolean isPasswordComplex(String password) {
        if (password == null) {
            return false;
        }
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{8,}$");
    }

    private boolean canViewProfile(User currentUser, String targetUserId) {
        // 权限规则：
        // 1. 自己永远可以看自己的资料；
        // 2. MO/Admin 可以查看别人的 TA 资料，用于审核和管理。
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            return false;
        }
        if (targetUserId.equalsIgnoreCase(currentUser.getUserId())) {
            return true;
        }
        return PROFILE_VIEW_ROLES.contains(currentUser.getRole());
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "resume.pdf";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String guessAvatarExtension(String fileName, String contentType) {
        String name = value(fileName).toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) return ".png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return ".jpg";
        if (name.endsWith(".gif")) return ".gif";
        if (name.endsWith(".webp")) return ".webp";

        String type = value(contentType).toLowerCase(Locale.ROOT);
        if (type.contains("png")) return ".png";
        if (type.contains("jpeg") || type.contains("jpg")) return ".jpg";
        if (type.contains("gif")) return ".gif";
        if (type.contains("webp")) return ".webp";
        return ".png";
    }

    private String buildAvatarUrl(HttpServletRequest req, String userId, String avatarStoredName) {
        if (value(avatarStoredName).isEmpty()) {
            return "";
        }
        // 这里不直接返回磁盘路径，而是返回后端接口地址，方便浏览器安全访问头像。
        return req.getContextPath() + "/api/user/avatar?userId=" + userId + "&t=" + System.currentTimeMillis();
    }

    private Path resolveResumeFile(String userId, String storedName, String originalName) {
        // 简历文件兼容查找逻辑：
        // 先按固定命名 {userId}.pdf 找，再按数据库记录的文件名找，最后再尝试历史遗留路径。
        Path dataDir = dataManager.getDataDirPath();
        Path resumesDir = dataDir.resolve("resumes");
        Path parentDir = dataDir.getParent();
        String fixedName = sanitizeFileName(userId) + ".pdf";

        List<Path> candidates = new ArrayList<>();
        candidates.add(resumesDir.resolve(fixedName));
        candidates.add(dataDir.resolve(fixedName));
        if (parentDir != null) {
            candidates.add(parentDir.resolve("resumes").resolve(fixedName));
        }
        if (!storedName.isEmpty()) {
            candidates.add(resumesDir.resolve(storedName));
            candidates.add(dataDir.resolve(storedName));
            if (parentDir != null) {
                candidates.add(parentDir.resolve("resumes").resolve(storedName));
            }
        }
        if (!originalName.isEmpty() && !originalName.equals(storedName)) {
            candidates.add(resumesDir.resolve(originalName));
            candidates.add(dataDir.resolve(originalName));
            if (parentDir != null) {
                candidates.add(parentDir.resolve("resumes").resolve(originalName));
            }
        }

        for (Path p : candidates) {
            if (Files.exists(p) && Files.isRegularFile(p)) {
                return p;
            }
        }

        // 如果上述路径都没找到，就退回到目录里最新生成的该用户简历。
        if (Files.exists(resumesDir) && Files.isDirectory(resumesDir)) {
            Path latest = null;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(resumesDir, userId + "_*.pdf")) {
                for (Path p : stream) {
                    if (latest == null || Files.getLastModifiedTime(p).toMillis() > Files.getLastModifiedTime(latest).toMillis()) {
                        latest = p;
                    }
                }
            } catch (Exception ignore) {
            }
            if (latest != null) {
                return latest;
            }
        }

        return resumesDir.resolve(storedName.isEmpty() ? originalName : storedName);
    }

    private Path resolveAvatarFile(String userId, String storedName) {
        // 头像文件查找逻辑和简历类似：先找数据库记录的文件名，再找该用户最新一张历史头像。
        Path dataDir = dataManager.getDataDirPath();
        Path avatarsDir = dataDir.resolve("avatars");

        if (!storedName.isEmpty()) {
            Path direct = avatarsDir.resolve(storedName);
            if (Files.exists(direct) && Files.isRegularFile(direct)) {
                return direct;
            }
            Path legacy = dataDir.resolve(storedName);
            if (Files.exists(legacy) && Files.isRegularFile(legacy)) {
                return legacy;
            }
        }

        if (Files.exists(avatarsDir) && Files.isDirectory(avatarsDir)) {
            Path latest = null;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(avatarsDir, userId + "_*")) {
                for (Path p : stream) {
                    if (!Files.isRegularFile(p)) {
                        continue;
                    }
                    if (latest == null || Files.getLastModifiedTime(p).toMillis() > Files.getLastModifiedTime(latest).toMillis()) {
                        latest = p;
                    }
                }
            } catch (Exception ignore) {
            }
            if (latest != null) {
                return latest;
            }
        }

        return avatarsDir.resolve(storedName);
    }

    private ResolvedProfile resolveProfileByAnyId(String targetUserId) {
        // 兼容“用户 ID / QM ID”两种查找方式。
        // 有些页面传 userId，有些页面传 qmId，所以这里做一层统一解析。
        String id = value(targetUserId);
        if (id.isEmpty()) {
            return new ResolvedProfile("", null);
        }

        Map<String, String> direct = dataManager.getProfile(id);
        if (direct != null) {
            return new ResolvedProfile(id, direct);
        }

        User user = dataManager.getUserById(id);
        if (user != null) {
            String qmId = value(user.getQmId());
            if (!qmId.isEmpty() && !qmId.equalsIgnoreCase(id)) {
                Map<String, String> qmProfile = dataManager.getProfile(qmId);
                if (qmProfile != null) {
                    return new ResolvedProfile(qmId, qmProfile);
                }
            }
        }

        User byQm = findUserByQmId(id);
        if (byQm != null) {
            Map<String, String> userProfile = dataManager.getProfile(byQm.getUserId());
            if (userProfile != null) {
                return new ResolvedProfile(byQm.getUserId(), userProfile);
            }
            String qmId = value(byQm.getQmId());
            if (!qmId.isEmpty()) {
                Map<String, String> qmProfile = dataManager.getProfile(qmId);
                if (qmProfile != null) {
                    return new ResolvedProfile(qmId, qmProfile);
                }
            }
        }

        return new ResolvedProfile(id, null);
    }

    private User findUserByQmId(String qmId) {
        // 通过 QM ID 反向查找用户，用于兼容旧数据和某些审核场景。
        String target = value(qmId);
        if (target.isEmpty()) {
            return null;
        }
        for (User item : dataManager.getAllUsers()) {
            if (target.equalsIgnoreCase(value(item.getQmId()))) {
                return item;
            }
        }
        return null;
    }

    private static class ResolvedProfile {
        private final String resolvedUserId;
        private final Map<String, String> profile;

        private ResolvedProfile(String resolvedUserId, Map<String, String> profile) {
            this.resolvedUserId = resolvedUserId;
            this.profile = profile;
        }
    }
}
