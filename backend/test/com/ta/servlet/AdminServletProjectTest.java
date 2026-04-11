package com.ta.servlet;

import com.ta.util.DataManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.Map;

public class AdminServletProjectTest {

    public static void main(String[] args) throws Exception {
        Path dataDir = java.nio.file.Files.createTempDirectory("ta-admin-test-");
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

            Map<String, String> app1 = dataManager.submitApplication("ta002", "李四", openPosition.get("id"), "first");
            Map<String, String> app2 = dataManager.submitApplication("ta_bob", "Bob Li", openPosition.get("id"), "second");
            dataManager.processApplication(app1.get("id"), "approved", "Great");
            dataManager.processApplication(app2.get("id"), "reject", "Not enough experience");

            AdminServlet servlet = new AdminServlet();

            JSONObject dashboard = new JSONObject(invokeGet(servlet, "/dashboard", "admin001").body());
            assertTrue(dashboard.getBoolean("success"), "admin dashboard should succeed");
            JSONObject summary = dashboard.getJSONObject("summary");
            assertEquals(2, summary.getInt("totalPositions"), "summary should count positions");
            assertEquals(1, summary.getInt("openPositions"), "summary should count open positions");
            assertEquals(1, summary.getInt("closedPositions"), "summary should count closed positions");
            assertEquals(2, summary.getInt("totalApplications"), "summary should count applications");
            assertEquals(0, summary.getInt("pendingApplications"), "summary should count pending applications");
            assertEquals(1, summary.getInt("approvedApplications"), "summary should count approved applications");
            assertEquals(1, summary.getInt("rejectedApplications"), "summary should count rejected applications");

            JSONArray positions = dashboard.getJSONArray("positions");
            assertEquals(2, positions.length(), "dashboard should include all positions");

            JSONObject positionsResponse = new JSONObject(invokeGet(servlet, "/positions", "admin001").body());
            assertTrue(positionsResponse.getBoolean("success"), "admin positions endpoint should succeed");
            assertEquals(2, positionsResponse.getJSONArray("positions").length(), "positions endpoint should return all positions");

            JSONObject workloadResponse = new JSONObject(invokeGet(servlet, "/ta-workload", "admin001").body());
            assertTrue(workloadResponse.getBoolean("success"), "ta workload endpoint should succeed");
            JSONArray workload = workloadResponse.getJSONArray("taWorkload");
            assertTrue(workload.length() >= 2, "ta workload should include TA users");

            System.out.println("AdminServletProjectTest passed.");
        } finally {
            LoginServletProjectTest.restoreSystemProperty("ta.data.dir", originalDataDir);
            LoginServletProjectTest.deleteRecursively(dataDir);
        }
    }

    private static LoginServletProjectTest.TestResponse invokeGet(AdminServlet servlet, String pathInfo, String userId) throws Exception {
        LoginServletProjectTest.TestResponse response = LoginServletProjectTest.response();
        servlet.doGet(LoginServletProjectTest.request(pathInfo, userId, Map.of()), response.proxy());
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

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }
}