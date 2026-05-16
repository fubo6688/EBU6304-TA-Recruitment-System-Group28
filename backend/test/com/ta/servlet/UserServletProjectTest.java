package com.ta.servlet;

import com.ta.model.User;
import com.ta.util.DataManager;
import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.file.Path;
import java.util.Map;

public class UserServletProjectTest {

    public static void main(String[] args) throws Exception {
        Path dataDir = java.nio.file.Files.createTempDirectory("ta-user-test-");
        String originalDataDir = System.getProperty("ta.data.dir");

        try {
            DataManager dataManager = newDataManager(dataDir,
                    "ta002|李四|lisi@example.com|Qmta2026A|TA|20210002|active",
                    "mo001|王老师|wangteacher@example.com|Qmta2026A|MO|M001|active",
                    "admin001|管理员|admin@example.com|Qmta2026A|Admin|ADM001|active",
                    "ta_pending|Pending TA|pending@example.com|Qmta2026A|TA|ta_pending|pending",
                    "mo_pending|Pending MO|pending-mo@example.com|Qmta2026A|MO|mo_pending|pending"
            );
            dataManager.saveProfile("ta002", "3", "Computer Science", "3.8", "lisi@example.com", "Java,SQL", "resume.pdf", "ta002.pdf", "Mon-Fri", "");

            UserServlet servlet = new UserServlet();

            JSONObject profile = new JSONObject(invokeGet(servlet, "/profile", "ta002", Map.of()).body());
            assertEquals("ta002", profile.getString("userId"), "profile endpoint should return the logged in user");
            assertEquals("Computer Science", profile.getString("major"), "profile should include saved major");
            assertEquals("Java,SQL", profile.getString("skills"), "profile should include saved skills");

            LoginServletProjectTest.TestResponse updateResponse = invokePost(servlet, "/profile", "ta002", Map.of(
                    "userName", "Li Si",
                    "email", "li.si@example.com",
                    "grade", "4",
                    "major", "Software Engineering",
                    "gpa", "3.9",
                    "skills", "Java,Testing",
                    "availableTime", "Weekends",
                    "resumeFileName", "resume-updated.pdf"
            ));
            JSONObject updateJson = new JSONObject(updateResponse.body());
            assertTrue(updateJson.getBoolean("success"), "profile update should succeed");

            DataManager refreshed = new DataManager();
            Map<String, String> updatedProfile = refreshed.getProfile("ta002");
            assertEquals("Software Engineering", updatedProfile.get("major"), "updated profile major should persist");
            User updatedUser = refreshed.getUserById("ta002");
            assertEquals("Li Si", updatedUser.getUserName(), "updated user name should persist");
            assertEquals("li.si@example.com", updatedUser.getEmail(), "updated email should persist");

            LoginServletProjectTest.TestResponse passwordResponse = invokePost(servlet, "/password", "ta002", Map.of(
                    "oldPassword", "Qmta2026A",
                    "newPassword", "NewPass123"
            ));
            JSONObject passwordJson = new JSONObject(passwordResponse.body());
            assertTrue(passwordJson.getBoolean("success"), "password change should succeed");
            assertEquals("NewPass123", new DataManager().getUserById("ta002").getPassword(), "new password should persist");

            JSONObject pendingRegistrations = new JSONObject(invokeGet(servlet, "/pending-registrations", "admin001", Map.of()).body());
            assertTrue(pendingRegistrations.getBoolean("success"), "admin should see pending registrations");
            JSONArray users = pendingRegistrations.getJSONArray("users");
            assertTrue(users.length() >= 2, "pending registrations should include TA and MO accounts");

            LoginServletProjectTest.TestResponse approveResponse = invokePost(servlet, "/approve-registration", "admin001", Map.of(
                    "userId", "ta_pending",
                    "decision", "approve"
            ));
            JSONObject approveJson = new JSONObject(approveResponse.body());
            assertTrue(approveJson.getBoolean("success"), "admin approval should succeed");
            assertEquals("active", new DataManager().getUserById("ta_pending").getStatus(), "approved registration should become active");

            JSONObject taProfile = new JSONObject(invokeGet(servlet, "/ta-profile", "mo001", Map.of("userId", "ta002")).body());
            assertTrue(taProfile.getBoolean("success"), "MO should be able to view TA profile");
            assertEquals("ta002", taProfile.getJSONObject("profile").getString("userId"), "ta-profile should return the requested TA account");
            assertTrue(taProfile.getJSONObject("profile").getString("resumePreviewUrl").contains("/api/user/resume?userId=ta002"),
                    "ta-profile should expose a resume preview URL");

            System.out.println("UserServletProjectTest passed.");
        } finally {
            LoginServletProjectTest.restoreSystemProperty("ta.data.dir", originalDataDir);
            LoginServletProjectTest.deleteRecursively(dataDir);
        }
    }

    private static LoginServletProjectTest.TestResponse invokeGet(UserServlet servlet, String pathInfo, String userId, Map<String, String> parameters) throws Exception {
        LoginServletProjectTest.TestResponse response = LoginServletProjectTest.response();
        servlet.doGet(request(pathInfo, userId, parameters), response.proxy());
        return response;
    }

    private static LoginServletProjectTest.TestResponse invokePost(UserServlet servlet, String pathInfo, String userId, Map<String, String> parameters) throws Exception {
        LoginServletProjectTest.TestResponse response = LoginServletProjectTest.response();
        servlet.doPost(request(pathInfo, userId, parameters), response.proxy());
        return response;
    }

    private static HttpServletRequest request(String pathInfo, String userId, Map<String, String> parameters) {
        return LoginServletProjectTest.request(pathInfo, userId, parameters);
    }

    private static DataManager newDataManager(Path dataDir, String... userLines) throws Exception {
        java.nio.file.Files.createDirectories(dataDir);
        java.nio.file.Files.write(dataDir.resolve("users.txt"), java.util.List.of(userLines), java.nio.charset.StandardCharsets.UTF_8);
        System.setProperty("ta.data.dir", dataDir.toString());
        return new DataManager();
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }
}