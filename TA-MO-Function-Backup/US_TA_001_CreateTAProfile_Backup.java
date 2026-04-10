/*
 * Backup file for user story: US-TA-001 Create TA Profile
 * Purpose: collect copied core backend code related to TA profile create/view/edit.
 * Source files:
 * - backend/src/com/ta/servlet/UserServlet.java
 * - backend/src/com/ta/util/DataManager.java
 */
public class US_TA_001_CreateTAProfile_Backup {

    /*
     * Copied from UserServlet#doGet (profile + ta-profile endpoints)
     * File: backend/src/com/ta/servlet/UserServlet.java
     *
     * if ("/profile".equalsIgnoreCase(path) || path.isEmpty() || "/".equals(path)) {
     *     JSONObject result = toUserJson(user);
     *     Map<String, String> profile = dataManager.getProfile(user.getUserId());
     *     if (profile != null) {
     *         result.put("grade", profile.get("grade"));
     *         result.put("major", profile.get("major"));
     *         result.put("gpa", profile.get("gpa"));
     *         result.put("skills", profile.get("skills"));
     *         result.put("resumeFileName", profile.get("resumeFileName"));
     *         result.put("availableTime", profile.get("availableTime"));
     *         result.put("profileUpdatedAt", profile.get("updatedAt"));
     *     }
     *     resp.getWriter().print(result.toString());
     *     return;
     * }
     *
     * if ("/ta-profile".equalsIgnoreCase(path)) {
     *     String targetUserId = value(req.getParameter("userId"));
     *     ...
     *     result.put("resumePreviewUrl", req.getContextPath() + "/api/user/resume?userId=" + targetUserId);
     *     resp.getWriter().print(new JSONObject().put("success", true).put("profile", result).toString());
     *     return;
     * }
     */

    /*
     * Copied from UserServlet#doPost (/profile endpoint)
     * File: backend/src/com/ta/servlet/UserServlet.java
     *
     * if ("/profile".equalsIgnoreCase(path) || path.isEmpty() || "/".equals(path)) {
     *     req.setCharacterEncoding("UTF-8");
     *     String userName = value(req.getParameter("userName"));
     *     String email = value(req.getParameter("email"));
     *     String grade = value(req.getParameter("grade"));
     *     String major = value(req.getParameter("major"));
     *     String gpa = value(req.getParameter("gpa"));
     *     String skills = value(req.getParameter("skills"));
     *     String availableTime = value(req.getParameter("availableTime"));
     *     String resumeFileName = value(req.getParameter("resumeFileName"));
     *
     *     Map<String, String> oldProfile = dataManager.getProfile(user.getUserId());
     *     String resumeStoredName = oldProfile == null ? "" : value(oldProfile.get("resumeStoredName"));
     *     ...
     *
     *     Part part = req.getPart("resumeFile");
     *     if (part != null && part.getSize() > 0) {
     *         String submitted = value(part.getSubmittedFileName());
     *         if (!submitted.toLowerCase().endsWith(".pdf")) {
     *             out.print(new JSONObject().put("success", false).put("message", "Only PDF resume is allowed").toString());
     *             return;
     *         }
     *         Path resumeDir = dataManager.getDataDirPath().resolve("resumes");
     *         Files.createDirectories(resumeDir);
     *         String safeOriginal = sanitizeFileName(submitted);
     *         String stored = sanitizeFileName(user.getUserId()) + ".pdf";
     *         Path target = resumeDir.resolve(stored);
     *         try (InputStream in = part.getInputStream()) {
     *             Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
     *         }
     *         resumeFileName = safeOriginal;
     *         resumeStoredName = stored;
     *     }
     *
     *     dataManager.saveProfile(
     *         user.getUserId(), grade, major, gpa,
     *         email.isEmpty() ? user.getEmail() : email,
     *         skills, resumeFileName, resumeStoredName,
     *         availableTime, avatarStoredName);
     * }
     */

    /*
     * Copied from DataManager (profile persistence)
     * File: backend/src/com/ta/util/DataManager.java
     *
     * public synchronized void saveProfile(String userId,
     *                                      String grade,
     *                                      String major,
     *                                      String gpa,
     *                                      String email,
     *                                      String skills,
     *                                      String resumeFileName,
     *                                      String resumeStoredName,
     *                                      String availableTime,
     *                                      String avatarStoredName) {
     *     // upsert by userId in profiles.txt
     * }
     *
     * public synchronized Map<String, String> getProfile(String userId) {
     *     // read user profile from profiles.txt with backward-compatible parsing
     * }
     */
}
