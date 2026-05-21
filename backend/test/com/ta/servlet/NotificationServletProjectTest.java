package com.ta.servlet;

import com.ta.util.DataManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class NotificationServletProjectTest {

    public static void main(String[] args) throws Exception {
        Path dataDir = Files.createTempDirectory("ta-notification-test-");
        String originalDataDir = System.getProperty("ta.data.dir");

        try {
            DataManager dataManager = newDataManager(dataDir,
                    "ta002|李四|lisi@example.com|Qmta2026A|TA|20210002|active",
                    "ta_inactive|Inactive TA|inactive@example.com|Qmta2026A|TA|ta_inactive|inactive",
                    "mo001|王老师|wangteacher@example.com|Qmta2026A|MO|M001|active"
            );

            dataManager.saveNotification("ta002", "system", "Welcome", "First message");
            dataManager.saveNotification("ta002", "application", "Application Update", "Second message");

            NotificationServlet servlet = new NotificationServlet();

            JSONObject listAll = new JSONObject(invokeGet(servlet, "/list", "ta002", Map.of()).body());
            assertTrue(listAll.getBoolean("success"), "notification list should succeed");
            JSONArray notifications = listAll.getJSONArray("notifications");
            assertEquals(2, notifications.length(), "notification list should return both items");
            assertEquals("application", notifications.getJSONObject(0).getString("type"), "list should return newest notification first");
            assertEquals(2, listAll.getInt("unreadCount"), "unread count should include both unread notifications");

            String latestId = notifications.getJSONObject(0).getString("id");
            JSONObject readResponse = new JSONObject(invokePost(servlet, "/read", "ta002", Map.of("notificationId", latestId)).body());
            assertTrue(readResponse.getBoolean("success"), "mark read should succeed");

            List<Map<String, String>> persistedAfterRead = new DataManager().getNotifications("ta002");
            Map<String, String> latestNotification = findById(persistedAfterRead, latestId);
            assertEquals("1", latestNotification.get("read"), "mark read should persist the read flag");

            JSONObject filtered = new JSONObject(invokeGet(servlet, "/list", "ta002", Map.of("type", "system")).body());
            assertTrue(filtered.getBoolean("success"), "filtered list should succeed");
            assertEquals(1, filtered.getJSONArray("notifications").length(), "type filter should narrow the list");

            JSONObject readAllResponse = new JSONObject(invokePost(servlet, "/read-all", "ta002", Map.of()).body());
            assertTrue(readAllResponse.getBoolean("success"), "mark all read should succeed");

            List<Map<String, String>> persistedAfterReadAll = new DataManager().getNotifications("ta002");
            for (Map<String, String> item : persistedAfterReadAll) {
                assertEquals("1", item.get("read"), "all notifications should be marked read");
            }

            JSONObject unauthorized = new JSONObject(invokeGet(servlet, "/list", null, Map.of()).body());
            assertFalse(unauthorized.getBoolean("success"), "anonymous requests should be rejected");

            JSONObject missingUser = new JSONObject(invokeGet(servlet, "/list", "ghost-user", Map.of()).body());
            assertFalse(missingUser.getBoolean("success"), "unknown users should be rejected");
            assertEquals("User not found", missingUser.getString("message"), "missing user message should match");

            JSONObject inactiveUser = new JSONObject(invokeGet(servlet, "/list", "ta_inactive", Map.of()).body());
            assertFalse(inactiveUser.getBoolean("success"), "inactive users should be rejected");
            assertEquals("Account is inactive", inactiveUser.getString("message"), "inactive account message should match");

            JSONObject missingNotificationId = new JSONObject(invokePost(servlet, "/read", "ta002", Map.of()).body());
            assertFalse(missingNotificationId.getBoolean("success"), "missing notificationId should fail");
            assertEquals("Missing notificationId", missingNotificationId.getString("message"), "missing id message should match");

            JSONObject notFoundNotification = new JSONObject(invokePost(servlet, "/read", "ta002", Map.of("notificationId", "does-not-exist")).body());
            assertFalse(notFoundNotification.getBoolean("success"), "unknown notification should fail");
            assertEquals("Notification not found", notFoundNotification.getString("message"), "unknown notification message should match");

            JSONObject unsupportedGet = new JSONObject(invokeGet(servlet, "/unknown", "ta002", Map.of()).body());
            assertFalse(unsupportedGet.getBoolean("success"), "unsupported GET endpoint should fail");
            assertEquals("Unsupported endpoint", unsupportedGet.getString("message"), "unsupported GET message should match");

            JSONObject unsupportedPost = new JSONObject(invokePost(servlet, "/unknown", "ta002", Map.of()).body());
            assertFalse(unsupportedPost.getBoolean("success"), "unsupported POST endpoint should fail");
            assertEquals("Unsupported endpoint", unsupportedPost.getString("message"), "unsupported POST message should match");

            System.out.println("NotificationServletProjectTest passed.");
        } finally {
            LoginServletProjectTest.restoreSystemProperty("ta.data.dir", originalDataDir);
            LoginServletProjectTest.deleteRecursively(dataDir);
        }
    }

    private static LoginServletProjectTest.TestResponse invokeGet(NotificationServlet servlet,
                                                                  String pathInfo,
                                                                  String userId,
                                                                  Map<String, String> parameters) throws Exception {
        LoginServletProjectTest.TestResponse response = LoginServletProjectTest.response();
        servlet.doGet(LoginServletProjectTest.request(pathInfo, userId, parameters), response.proxy());
        return response;
    }

    private static LoginServletProjectTest.TestResponse invokePost(NotificationServlet servlet,
                                                                   String pathInfo,
                                                                   String userId,
                                                                   Map<String, String> parameters) throws Exception {
        LoginServletProjectTest.TestResponse response = LoginServletProjectTest.response();
        servlet.doPost(LoginServletProjectTest.request(pathInfo, userId, parameters), response.proxy());
        return response;
    }

    private static DataManager newDataManager(Path dataDir, String... userLines) throws Exception {
        Files.createDirectories(dataDir);
        Files.write(dataDir.resolve("users.txt"), List.of(userLines), StandardCharsets.UTF_8);
        System.setProperty("ta.data.dir", dataDir.toString());
        return new DataManager();
    }

    private static Map<String, String> findById(List<Map<String, String>> items, String id) {
        for (Map<String, String> item : items) {
            if (id.equals(item.get("id"))) {
                return item;
            }
        }
        throw new AssertionError("notification not found: " + id);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }
}