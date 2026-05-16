package com.ta.servlet;

import com.ta.util.DataManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.Map;

public class ApplicationServletProjectTest {

    public static void main(String[] args) throws Exception {
        Path dataDir = java.nio.file.Files.createTempDirectory("ta-application-test-");
        String originalDataDir = System.getProperty("ta.data.dir");

        try {
            DataManager dataManager = newDataManager(dataDir,
                    "ta002|李四|lisi@example.com|Qmta2026A|TA|20210002|active",
                    "ta_bob|Bob Li|ta_bob@example.com|Qmta2026A|TA|ta_bob|active",
                    "mo001|王老师|wangteacher@example.com|Qmta2026A|MO|M001|active",
                    "admin001|管理员|admin@example.com|Qmta2026A|Admin|ADM001|active"
            );

            Map<String, String> openPosition = dataManager.createPosition("Java TA", "CS", "1000", "Teach Java", "Java", "M001", "2", "2026-12-31");
            Map<String, String> closedPosition = dataManager.createPosition("DB TA", "CS", "1000", "Teach DB", "SQL", "M001", "1", "2026-12-31");
            dataManager.updatePositionStatus(closedPosition.get("id"), "closed");

            ApplicationServlet servlet = new ApplicationServlet();

            LoginServletProjectTest.TestResponse submitResponse = invokePost(servlet, "/submit", "ta002", Map.of(
                    "positionId", openPosition.get("id"),
                    "priority", "first"
            ));
            JSONObject submitJson = new JSONObject(submitResponse.body());
            assertTrue(submitJson.getBoolean("success"), "TA should be able to submit to open position");
            String applicationId = submitJson.getJSONObject("application").getString("id");

            LoginServletProjectTest.TestResponse closedSubmitResponse = invokePost(servlet, "/submit", "ta002", Map.of(
                    "positionId", closedPosition.get("id"),
                    "priority", "second"
            ));
            JSONObject closedSubmitJson = new JSONObject(closedSubmitResponse.body());
            assertFalse(closedSubmitJson.getBoolean("success"), "TA should not be able to submit to closed position");

            JSONObject myListJson = new JSONObject(invokeGet(servlet, "/my-list", "ta002", Map.of()).body());
            JSONArray myList = myListJson.getJSONArray("applications");
            assertEquals(1, myList.length(), "my-list should contain the submitted application only");
            assertEquals("open", myList.getJSONObject(0).getString("positionStatus"), "my-list should enrich the position status");

            JSONObject reviewListJson = new JSONObject(invokeGet(servlet, "/review-list", "mo001", Map.of()).body());
            JSONArray reviewList = reviewListJson.getJSONArray("applications");
            assertEquals(1, reviewList.length(), "MO review-list should contain own position applications");

            LoginServletProjectTest.TestResponse reviewResponse = invokePost(servlet, "/review", "mo001", Map.of(
                    "applicationId", applicationId,
                    "decision", "approved",
                    "feedback", "Good"));
            JSONObject reviewJson = new JSONObject(reviewResponse.body());
            assertTrue(reviewJson.getBoolean("success"), "MO should be able to approve own application");
            assertEquals("approved", reviewJson.getJSONObject("application").getString("status"), "application should become approved");

            DataManager refreshed = new DataManager();
            assertEquals("approved", refreshed.getApplicationById(applicationId).get("status"), "approved status should persist");
            assertEquals(1, Integer.parseInt(refreshed.getPositionById(openPosition.get("id")).get("acceptedCount")), "accepted count should increment after approval");
            assertTrue(refreshed.getNotifications("ta002").size() >= 2, "TA should receive application notifications");

            System.out.println("ApplicationServletProjectTest passed.");
        } finally {
            LoginServletProjectTest.restoreSystemProperty("ta.data.dir", originalDataDir);
            LoginServletProjectTest.deleteRecursively(dataDir);
        }
    }

    private static LoginServletProjectTest.TestResponse invokeGet(ApplicationServlet servlet, String pathInfo, String userId, Map<String, String> parameters) throws Exception {
        LoginServletProjectTest.TestResponse response = LoginServletProjectTest.response();
        servlet.doGet(LoginServletProjectTest.request(pathInfo, userId, parameters), response.proxy());
        return response;
    }

    private static LoginServletProjectTest.TestResponse invokePost(ApplicationServlet servlet, String pathInfo, String userId, Map<String, String> parameters) throws Exception {
        LoginServletProjectTest.TestResponse response = LoginServletProjectTest.response();
        servlet.doPost(LoginServletProjectTest.request(pathInfo, userId, parameters), response.proxy());
        return response;
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