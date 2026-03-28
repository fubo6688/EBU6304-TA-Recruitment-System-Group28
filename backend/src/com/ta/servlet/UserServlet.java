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
    private static final Set<String> PROFILE_VIEW_ROLES = new HashSet<>();

    static {
        PROFILE_VIEW_ROLES.add("MO");
        PROFILE_VIEW_ROLES.add("Admin");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        User user = requireLogin(req, resp);
        if (user == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/avatar".equalsIgnoreCase(path)) {
            String targetUserId = value(req.getParameter("userId"));
            if (targetUserId.isEmpty()) {
                targetUserId = user.getUserId();
            }

            if (!canViewProfile(user, targetUserId)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

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
            String targetUserId = value(req.getParameter("userId"));
            if (targetUserId.isEmpty()) {
                targetUserId = user.getUserId();
            }

            if (!canViewProfile(user, targetUserId)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().print(new JSONObject().put("success", false).put("message", "No permission").toString());
                return;
            }

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
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        User user = requireLogin(req, resp);
        if (user == null) {
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/profile".equalsIgnoreCase(path) || path.isEmpty() || "/".equals(path)) {
            req.setCharacterEncoding("UTF-8");
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

                    Part part = req.getPart("resumeFile");
                    if (part != null && part.getSize() > 0) {
                        String submitted = value(part.getSubmittedFileName());
                        String lower = submitted.toLowerCase();
                        if (!lower.endsWith(".pdf")) {
                            out.print(new JSONObject().put("success", false).put("message", "Only PDF resume is allowed").toString());
                            return;
                        }

                        Path resumeDir = dataManager.getDataDirPath().resolve("resumes");
                        Files.createDirectories(resumeDir);
                        String safeOriginal = sanitizeFileName(submitted);
                        String stored = user.getUserId() + "_" + System.currentTimeMillis() + ".pdf";
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

    private boolean canViewProfile(User currentUser, String targetUserId) {
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
        return req.getContextPath() + "/api/user/avatar?userId=" + userId + "&t=" + System.currentTimeMillis();
    }

    private Path resolveResumeFile(String userId, String storedName, String originalName) {
        Path dataDir = dataManager.getDataDirPath();
        Path resumesDir = dataDir.resolve("resumes");

        List<Path> candidates = new ArrayList<>();
        if (!storedName.isEmpty()) {
            candidates.add(resumesDir.resolve(storedName));
            candidates.add(dataDir.resolve(storedName));
        }
        if (!originalName.isEmpty() && !originalName.equals(storedName)) {
            candidates.add(resumesDir.resolve(originalName));
            candidates.add(dataDir.resolve(originalName));
        }

        for (Path p : candidates) {
            if (Files.exists(p) && Files.isRegularFile(p)) {
                return p;
            }
        }

        // Fallback: pick latest generated resume for this user.
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
