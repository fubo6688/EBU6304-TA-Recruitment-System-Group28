package com.ta.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DataManagerProjectTest {

    public static void main(String[] args) throws Exception {
        Path dataDir = Files.createTempDirectory("ta-data-manager-test-");
        String originalDataDir = System.getProperty("ta.data.dir");

        try {
            DataManager dataManager = newDataManager(dataDir,
                    "ta002|李四|lisi@example.com|Qmta2026A|TA|20210002|active",
                    "mo001|王老师|wangteacher@example.com|Qmta2026A|MO|M001|active"
            );

            assertEquals(dataDir.toAbsolutePath().normalize().toString(),
                    dataManager.getDataDirPath().toAbsolutePath().normalize().toString(),
                    "data manager should use the configured data directory");

            dataManager.saveNotification("ta002", "system", "Welcome", "First message");
            dataManager.saveNotificationIfAbsent("ta002", "system", "Deduped", "Second message", "dedupe-1");
            dataManager.saveNotificationIfAbsent("ta002", "system", "Deduped", "Second message", "dedupe-1");
            dataManager.saveNotificationIfAbsent("ta002", "system", "NoKey", "Third message", "");

            List<Map<String, String>> notifications = dataManager.getNotifications("ta002");
            assertEquals(3, notifications.size(), "empty dedupe key should still save and duplicate key should be ignored");
            String dedupeId = notifications.get(1).get("id");
            assertTrue(dataManager.markNotificationRead("ta002", dedupeId), "markNotificationRead should find the notification");

            Map<String, String> marked = findById(dataManager.getNotifications("ta002"), dedupeId);
            assertEquals("1", marked.get("read"), "marked notification should persist as read");

            dataManager.markAllNotificationsRead("ta002");
            for (Map<String, String> item : dataManager.getNotifications("ta002")) {
                assertEquals("1", item.get("read"), "markAllNotificationsRead should update every notification");
            }

            Files.writeString(dataDir.resolve("ta002_notifications.txt"), "Legacy notification line" + System.lineSeparator(), StandardCharsets.UTF_8);
            List<Map<String, String>> legacyNotifications = new DataManager().getNotifications("ta002");
            Map<String, String> legacyItem = legacyNotifications.get(0);
            assertEquals("system", legacyItem.get("type"), "legacy notifications should be mapped to system type");
            assertEquals("系统通知", legacyItem.get("title"), "legacy notifications should be given the default title");
            assertEquals("Legacy notification line", legacyItem.get("message"), "legacy notification text should be preserved");

            dataManager.saveProfile("ta002", "4", "Software Engineering", "3.9", "lisi@example.com",
                    "Java,Testing", "resume.pdf", "stored-resume.pdf", "Weekends", "avatar.png", "2 years");
            Map<String, String> profile = dataManager.getProfile("ta002");
            assertEquals("Software Engineering", profile.get("major"), "profile major should persist");
            assertEquals("Java,Testing", profile.get("skills"), "profile skills should persist");
            assertEquals("stored-resume.pdf", profile.get("resumeStoredName"), "profile resume stored name should persist");

            assertTrue(dataManager.validateLogin("ta002", "Qmta2026A") != null, "validateLogin should accept correct credentials");
            assertTrue(dataManager.validateLogin("ta002", "wrong") == null, "validateLogin should reject incorrect credentials");

            System.out.println("DataManagerProjectTest passed.");
        } finally {
            restoreSystemProperty("ta.data.dir", originalDataDir);
            deleteRecursively(dataDir);
        }
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
        throw new AssertionError("item not found: " + id);
    }

    private static void restoreSystemProperty(String key, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, originalValue);
        }
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }

        try {
            Files.walk(root)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
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