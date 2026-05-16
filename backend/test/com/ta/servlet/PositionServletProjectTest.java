package com.ta.servlet;

import com.ta.util.DataManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class PositionServletProjectTest {

    public static void main(String[] args) throws Exception {
        Path dataDir = Files.createTempDirectory("ta-position-test-");
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

            PositionServlet servlet = new PositionServlet();

            JSONArray list = new JSONArray(invokeGet(servlet, "/list", "ta002", Map.of()).body());
            assertTrue(list.length() >= 2, "TA should see project positions in the list endpoint");

            LoginServletProjectTest.TestResponse createResponse = invokePost(servlet, "/create", "mo001", Map.of(
                    "title", "Software Testing TA",
                    "department", "CS",
                    "salary", "1200",
                    "description", "Testing support",
                    "requirements", "JUnit",
                    "openings", "3",
                    "deadline", "2026-11-30"
            ));
            JSONObject createJson = new JSONObject(createResponse.body());
            assertTrue(createJson.getBoolean("success"), "MO should be able to create a position");
            String createdPositionId = createJson.getJSONObject("position").getString("id");

            LoginServletProjectTest.TestResponse updateResponse = invokePost(servlet, "/update", "mo001", Map.of(
                    "positionId", createdPositionId,
                    "title", "Software Testing Assistant",
                    "openings", "4",
                    "deadline", "2026-12-15"
            ));
            JSONObject updateJson = new JSONObject(updateResponse.body());
            assertTrue(updateJson.getBoolean("success"), "MO should be able to update own position");
            assertEquals("Software Testing Assistant", updateJson.getJSONObject("position").getString("title"), "updated title should be returned");

            LoginServletProjectTest.TestResponse statusResponse = invokePost(servlet, "/status", "mo001", Map.of(
                    "positionId", createdPositionId,
                    "status", "closed"
            ));
            JSONObject statusJson = new JSONObject(statusResponse.body());
            assertTrue(statusJson.getBoolean("success"), "MO should be able to close own position");
            assertEquals("closed", statusJson.getString("status"), "status endpoint should return the new status");

            LoginServletProjectTest.TestResponse publishResponse = invokePost(servlet, "/publish", "mo001", Map.of(
                    "positionId", openPosition.get("id")
            ));
            JSONObject publishJson = new JSONObject(publishResponse.body());
            assertTrue(publishJson.getBoolean("success"), "publish should succeed for own position");
            assertEquals(2, publishJson.getInt("notifiedCount"), "publish should notify all active TA users");

            assertTrue(Files.exists(dataDir.resolve("ta002_notifications.txt")), "publish should write TA notifications");
            assertTrue(Files.exists(dataDir.resolve("ta_bob_notifications.txt")), "publish should write TA notifications for each active TA");

            System.out.println("PositionServletProjectTest passed.");
        } finally {
            LoginServletProjectTest.restoreSystemProperty("ta.data.dir", originalDataDir);
            LoginServletProjectTest.deleteRecursively(dataDir);
        }
    }

    private static LoginServletProjectTest.TestResponse invokeGet(PositionServlet servlet, String pathInfo, String userId, Map<String, String> parameters) throws Exception {
        LoginServletProjectTest.TestResponse response = LoginServletProjectTest.response();
        servlet.doGet(LoginServletProjectTest.request(pathInfo, userId, parameters), response.proxy());
        return response;
    }

    private static LoginServletProjectTest.TestResponse invokePost(PositionServlet servlet, String pathInfo, String userId, Map<String, String> parameters) throws Exception {
        LoginServletProjectTest.TestResponse response = LoginServletProjectTest.response();
        servlet.doPost(LoginServletProjectTest.request(pathInfo, userId, parameters), response.proxy());
        return response;
    }

    private static DataManager newDataManager(Path dataDir, String... userLines) throws Exception {
        Files.createDirectories(dataDir);
        Files.write(dataDir.resolve("users.txt"), java.util.List.of(userLines), java.nio.charset.StandardCharsets.UTF_8);
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