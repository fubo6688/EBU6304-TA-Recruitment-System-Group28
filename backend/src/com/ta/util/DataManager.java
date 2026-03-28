package com.ta.util;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import com.ta.model.User;

public class DataManager {
    private final Path dataDir;

    private static final String USERS_FILE = "users.txt";
    private static final String POSITIONS_FILE = "positions.txt";
    private static final String APPLICATIONS_FILE = "applications.txt";
    private static final String LOGS_FILE = "logs.txt";
    private static final String PROFILES_FILE = "profiles.txt";

    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public DataManager() {
        this.dataDir = resolveDataDir();
        ensureDataDirAndDefaults();
    }

    private Path resolveDataDir() {
        String custom = System.getProperty("ta.data.dir");
        if (custom == null || custom.trim().isEmpty()) {
            custom = System.getenv("TA_DATA_DIR");
        }
        if (custom != null && !custom.trim().isEmpty()) {
            return Paths.get(custom.trim());
        }

        Path appData = resolveDataDirFromCodeSource();
        if (appData != null) {
            return appData;
        }

        Path backendData = Paths.get("backend", "data");
        if (Files.exists(backendData) || Files.exists(backendData.getParent())) {
            return backendData;
        }

        return Paths.get("data");
    }

    private Path resolveDataDirFromCodeSource() {
        try {
            if (DataManager.class.getProtectionDomain() == null ||
                    DataManager.class.getProtectionDomain().getCodeSource() == null ||
                    DataManager.class.getProtectionDomain().getCodeSource().getLocation() == null) {
                return null;
            }

            Path codePath = Paths.get(DataManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isDirectory(codePath)) {
                return null;
            }

            // Typical exploded webapp path: <webapp>/WEB-INF/classes
            Path classesDir = codePath.normalize();
            Path webInf = classesDir.getParent();
            if (webInf != null && "WEB-INF".equalsIgnoreCase(String.valueOf(webInf.getFileName()))) {
                Path webAppRoot = webInf.getParent();
                if (webAppRoot != null) {
                    return webAppRoot.resolve("data");
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private void ensureDataDirAndDefaults() {
        try {
            Files.createDirectories(dataDir);
            ensureFile(USERS_FILE);
            ensureFile(POSITIONS_FILE);
            ensureFile(APPLICATIONS_FILE);
            ensureFile(LOGS_FILE);
            ensureFile(PROFILES_FILE);
            if (getAllUsers().isEmpty()) {
                initDefaultUsers();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ensureFile(String fileName) throws IOException {
        Path file = resolve(fileName);
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
    }

    private Path resolve(String fileName) {
        return dataDir.resolve(fileName);
    }

    public Path getDataDirPath() {
        return dataDir;
    }

    private List<String> readLinesSafe(Path path) {
        try {
            if (!Files.exists(path)) {
                return new ArrayList<>();
            }
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void writeLinesSafe(Path path, List<String> lines) {
        try {
            Files.write(path, lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendLineSafe(Path path, String line) {
        try {
            Files.write(path, Collections.singletonList(line), StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String nowDateTime() {
        synchronized (DATE_TIME_FORMAT) {
            return DATE_TIME_FORMAT.format(new Date());
        }
    }

    private String todayDate() {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(new Date());
        }
    }

    private String nextId(String prefix) {
        return prefix + System.currentTimeMillis();
    }

    public synchronized List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        for (String line : readLinesSafe(resolve(USERS_FILE))) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            User user = User.fromString(line);
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    public synchronized User getUserById(String userId) {
        return getAllUsers().stream()
                .filter(u -> u.getUserId().equalsIgnoreCase(userId))
                .findFirst()
                .orElse(null);
    }

    public synchronized User validateLogin(String userId, String password) {
        User user = getUserById(userId);
        if (user != null && "active".equalsIgnoreCase(user.getStatus()) && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public synchronized void saveUser(User user) {
        List<User> users = getAllUsers();
        users.removeIf(u -> u.getUserId().equalsIgnoreCase(user.getUserId()));
        users.add(user);
        saveAllUsers(users);
    }

    private synchronized void saveAllUsers(List<User> users) {
        List<String> lines = users.stream().map(User::toString).collect(Collectors.toList());
        writeLinesSafe(resolve(USERS_FILE), lines);
    }

    private void initDefaultUsers() {
        List<User> users = new ArrayList<>();
        users.add(new User("ta001", "张三", "zhangsan@example.com", "123456", "TA", "20210001"));
        users.add(new User("ta002", "李四", "lisi@example.com", "123456", "TA", "20210002"));
        users.add(new User("mo001", "王老师", "wangteacher@example.com", "123456", "MO", "M001"));
        users.add(new User("admin001", "管理员", "admin@example.com", "admin123", "Admin", "ADM001"));
        users.add(new User("20210001", "张三", "zhangsan@example.com", "123456", "TA", "20210001"));
        users.add(new User("20210002", "李四", "lisi@example.com", "123456", "TA", "20210002"));
        users.add(new User("M001", "王老师", "wangteacher@example.com", "123456", "MO", "M001"));
        users.add(new User("ADM001", "管理员", "admin@example.com", "admin123", "Admin", "ADM001"));
        saveAllUsers(users);
    }

    public synchronized List<Map<String, String>> getAllPositions() {
        List<Map<String, String>> positions = new ArrayList<>();
        for (String line : readLinesSafe(resolve(POSITIONS_FILE))) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] p = line.split("\\|", -1);
            if (p.length < 13) {
                continue;
            }
            Map<String, String> item = new LinkedHashMap<>();
            item.put("id", p[0]);
            item.put("title", p[1]);
            item.put("department", p[2]);
            item.put("salary", p[3]);
            item.put("description", p[4]);
            item.put("requirements", p[5]);
            item.put("moId", p[6]);
            item.put("openings", p[7]);
            item.put("appliedCount", p[8]);
            item.put("acceptedCount", p[9]);
            item.put("status", p[10]);
            item.put("createdAt", p[11]);
            item.put("deadline", p[12]);
            positions.add(item);
        }
        return positions;
    }

    public synchronized Map<String, String> getPositionById(String positionId) {
        for (Map<String, String> item : getAllPositions()) {
            if (positionId.equals(item.get("id"))) {
                return item;
            }
        }
        return null;
    }

    public synchronized Map<String, String> createPosition(String title,
                                                           String department,
                                                           String salary,
                                                           String description,
                                                           String requirements,
                                                           String moId,
                                                           String openings,
                                                           String deadline) {
        String id = nextId("pos");
        String line = String.join("|",
                id,
                safe(title),
                safe(department),
                safe(salary),
                safe(description),
                safe(requirements),
                safe(moId),
                safe(openings.isEmpty() ? "1" : openings),
                "0",
                "0",
                "open",
                todayDate(),
                safe(deadline.isEmpty() ? todayDate() : deadline));
        appendLineSafe(resolve(POSITIONS_FILE), line);
        return getPositionById(id);
    }

    public synchronized boolean updatePositionStatus(String positionId, String status) {
        List<Map<String, String>> positions = getAllPositions();
        boolean found = false;
        List<String> lines = new ArrayList<>();
        for (Map<String, String> p : positions) {
            if (positionId.equals(p.get("id"))) {
                p.put("status", status);
                found = true;
            }
            lines.add(toPositionLine(p));
        }
        if (found) {
            writeLinesSafe(resolve(POSITIONS_FILE), lines);
        }
        return found;
    }

    private String toPositionLine(Map<String, String> p) {
        return String.join("|",
                safe(p.get("id")),
                safe(p.get("title")),
                safe(p.get("department")),
                safe(p.get("salary")),
                safe(p.get("description")),
                safe(p.get("requirements")),
                safe(p.get("moId")),
                safe(p.get("openings")),
                safe(p.get("appliedCount")),
                safe(p.get("acceptedCount")),
                safe(p.get("status")),
                safe(p.get("createdAt")),
                safe(p.get("deadline")));
    }

    public synchronized List<Map<String, String>> getAllApplications() {
        List<Map<String, String>> apps = new ArrayList<>();
        for (String line : readLinesSafe(resolve(APPLICATIONS_FILE))) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] p = line.split("\\|", -1);
            if (p.length < 10) {
                continue;
            }
            Map<String, String> item = new LinkedHashMap<>();
            item.put("id", p[0]);
            item.put("positionId", p[1]);
            item.put("positionTitle", p[2]);
            item.put("userId", p[3]);
            item.put("userName", p[4]);
            item.put("moId", p[5]);
            item.put("priority", p[6]);
            item.put("status", p[7]);
            item.put("appliedDate", p[8]);
            item.put("feedback", p[9]);
            apps.add(item);
        }
        return apps;
    }

    public synchronized List<Map<String, String>> getApplicationsByUser(String userId) {
        return getAllApplications().stream()
                .filter(a -> userId.equals(a.get("userId")))
                .collect(Collectors.toList());
    }

    public synchronized List<Map<String, String>> getApplicationsByPosition(String positionId) {
        return getAllApplications().stream()
                .filter(a -> positionId.equals(a.get("positionId")))
                .collect(Collectors.toList());
    }

    public synchronized Map<String, String> getApplicationById(String applicationId) {
        for (Map<String, String> app : getAllApplications()) {
            if (applicationId.equals(app.get("id"))) {
                return app;
            }
        }
        return null;
    }

    public synchronized Map<String, String> submitApplication(String userId,
                                                               String userName,
                                                               String positionId,
                                                               String priority) {
        Map<String, String> position = getPositionById(positionId);
        if (position == null) {
            return null;
        }

        for (Map<String, String> app : getAllApplications()) {
            if (positionId.equals(app.get("positionId")) && userId.equals(app.get("userId")) && !"canceled".equalsIgnoreCase(app.get("status"))) {
                return app;
            }
        }

        String id = nextId("app");
        String moId = safe(position.get("moId"));
        String line = String.join("|",
                id,
                safe(positionId),
                safe(position.get("title")),
                safe(userId),
                safe(userName),
                moId,
                safe(priority.isEmpty() ? "first" : priority),
                "pending",
                todayDate(),
                "");
        appendLineSafe(resolve(APPLICATIONS_FILE), line);

        incrementPositionCounter(positionId, "appliedCount", 1);
        return getApplicationById(id);
    }

    public synchronized boolean updateApplicationPriority(String applicationId, String priority) {
        List<Map<String, String>> apps = getAllApplications();
        boolean found = false;
        List<String> lines = new ArrayList<>();
        for (Map<String, String> app : apps) {
            if (applicationId.equals(app.get("id"))) {
                app.put("priority", priority);
                found = true;
            }
            lines.add(toApplicationLine(app));
        }
        if (found) {
            writeLinesSafe(resolve(APPLICATIONS_FILE), lines);
        }
        return found;
    }

    public synchronized boolean cancelApplication(String applicationId) {
        return updateApplicationStatus(applicationId, "canceled", "申请已撤回");
    }

    public synchronized boolean processApplication(String applicationId, String decision, String feedback) {
        String status = "rejected";
        if ("approved".equalsIgnoreCase(decision) || "accept".equalsIgnoreCase(decision) || "accepted".equalsIgnoreCase(decision)) {
            status = "approved";
        }
        return updateApplicationStatus(applicationId, status, feedback);
    }

    public synchronized boolean updateApplicationStatus(String applicationId, String status, String feedback) {
        List<Map<String, String>> apps = getAllApplications();
        boolean found = false;
        String positionId = null;
        String oldStatus = null;
        List<String> lines = new ArrayList<>();
        for (Map<String, String> app : apps) {
            if (applicationId.equals(app.get("id"))) {
                oldStatus = app.get("status");
                app.put("status", status);
                app.put("feedback", safe(feedback));
                positionId = app.get("positionId");
                found = true;
            }
            lines.add(toApplicationLine(app));
        }
        if (found) {
            writeLinesSafe(resolve(APPLICATIONS_FILE), lines);
            if ("approved".equalsIgnoreCase(status) && !"approved".equalsIgnoreCase(oldStatus) && positionId != null) {
                incrementPositionCounter(positionId, "acceptedCount", 1);
            }
            if ("canceled".equalsIgnoreCase(status) && !"canceled".equalsIgnoreCase(oldStatus) && positionId != null) {
                incrementPositionCounter(positionId, "appliedCount", -1);
                if ("approved".equalsIgnoreCase(oldStatus)) {
                    incrementPositionCounter(positionId, "acceptedCount", -1);
                }
            }
        }
        return found;
    }

    private String toApplicationLine(Map<String, String> app) {
        return String.join("|",
                safe(app.get("id")),
                safe(app.get("positionId")),
                safe(app.get("positionTitle")),
                safe(app.get("userId")),
                safe(app.get("userName")),
                safe(app.get("moId")),
                safe(app.get("priority")),
                safe(app.get("status")),
                safe(app.get("appliedDate")),
                safe(app.get("feedback")));
    }

    private void incrementPositionCounter(String positionId, String field, int delta) {
        List<Map<String, String>> positions = getAllPositions();
        List<String> lines = new ArrayList<>();
        for (Map<String, String> p : positions) {
            if (positionId.equals(p.get("id"))) {
                int current = parseIntSafe(p.get(field), 0);
                p.put(field, String.valueOf(Math.max(0, current + delta)));
            }
            lines.add(toPositionLine(p));
        }
        writeLinesSafe(resolve(POSITIONS_FILE), lines);
    }

    public synchronized void saveNotification(String userId, String notification) {
        saveNotification(userId, "system", "系统通知", notification);
    }

    public synchronized void saveNotification(String userId, String type, String title, String message) {
        String line = String.join("|",
                nextId("ntf"),
                safe(type),
                safe(title),
                safe(message),
                nowDateTime(),
                "0");
        appendLineSafe(resolve(userId + "_notifications.txt"), line);
    }

    public synchronized List<Map<String, String>> getNotifications(String userId) {
        List<Map<String, String>> notifications = new ArrayList<>();
        Path file = resolve(userId + "_notifications.txt");
        for (String line : readLinesSafe(file)) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] p = line.split("\\|", -1);
            Map<String, String> item = new LinkedHashMap<>();
            if (p.length >= 6) {
                item.put("id", p[0]);
                item.put("type", p[1]);
                item.put("title", p[2]);
                item.put("message", p[3]);
                item.put("time", p[4]);
                item.put("read", p[5]);
            } else {
                item.put("id", nextId("legacy"));
                item.put("type", "system");
                item.put("title", "系统通知");
                item.put("message", line);
                item.put("time", nowDateTime());
                item.put("read", "0");
            }
            notifications.add(item);
        }
        return notifications;
    }

    public synchronized void markAllNotificationsRead(String userId) {
        List<Map<String, String>> list = getNotifications(userId);
        List<String> lines = new ArrayList<>();
        for (Map<String, String> item : list) {
            item.put("read", "1");
            lines.add(toNotificationLine(item));
        }
        writeLinesSafe(resolve(userId + "_notifications.txt"), lines);
    }

    public synchronized boolean markNotificationRead(String userId, String notificationId) {
        List<Map<String, String>> list = getNotifications(userId);
        boolean found = false;
        List<String> lines = new ArrayList<>();
        for (Map<String, String> item : list) {
            if (notificationId.equals(item.get("id"))) {
                item.put("read", "1");
                found = true;
            }
            lines.add(toNotificationLine(item));
        }
        if (found) {
            writeLinesSafe(resolve(userId + "_notifications.txt"), lines);
        }
        return found;
    }

    private String toNotificationLine(Map<String, String> n) {
        return String.join("|",
                safe(n.get("id")),
                safe(n.get("type")),
                safe(n.get("title")),
                safe(n.get("message")),
                safe(n.get("time")),
                safe(n.get("read")));
    }

    public synchronized void saveProfile(String userId,
                                         String grade,
                                         String major,
                                         String gpa,
                                         String email,
                                         String skills,
                                         String resumeFileName,
                                         String resumeStoredName,
                                         String availableTime,
                                         String avatarStoredName) {
        List<String> lines = readLinesSafe(resolve(PROFILES_FILE));
        List<String> updated = new ArrayList<>();
        boolean replaced = false;
        for (String line : lines) {
            String[] p = line.split("\\|", -1);
            if (p.length > 0 && userId.equals(p[0])) {
                updated.add(String.join("|",
                        userId,
                        safe(grade),
                        safe(major),
                        safe(gpa),
                        safe(email),
                        safe(skills),
                        safe(resumeFileName),
                        safe(resumeStoredName),
                        safe(availableTime),
                        safe(avatarStoredName),
                        nowDateTime()));
                replaced = true;
            } else {
                updated.add(line);
            }
        }
        if (!replaced) {
            updated.add(String.join("|",
                    userId,
                    safe(grade),
                    safe(major),
                    safe(gpa),
                    safe(email),
                    safe(skills),
                    safe(resumeFileName),
                    safe(resumeStoredName),
                    safe(availableTime),
                    safe(avatarStoredName),
                    nowDateTime()));
        }
        writeLinesSafe(resolve(PROFILES_FILE), updated);
    }

    public synchronized Map<String, String> getProfile(String userId) {
        for (String line : readLinesSafe(resolve(PROFILES_FILE))) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 8 && userId.equals(p[0])) {
                Map<String, String> result = new LinkedHashMap<>();
                result.put("userId", p[0]);
                result.put("grade", p[1]);
                result.put("major", p[2]);
                result.put("gpa", p[3]);
                result.put("email", p[4]);
                result.put("skills", p[5]);
                result.put("resumeFileName", p[6]);
                if (p.length >= 11) {
                    result.put("resumeStoredName", p[7]);
                    result.put("availableTime", p[8]);
                    result.put("avatarStoredName", p[9]);
                    result.put("updatedAt", p[10]);
                } else if (p.length >= 10) {
                    result.put("resumeStoredName", p[7]);
                    result.put("availableTime", p[8]);
                    result.put("avatarStoredName", "");
                    result.put("updatedAt", p[9]);
                } else {
                    result.put("resumeStoredName", p[6]);
                    result.put("availableTime", "");
                    result.put("avatarStoredName", "");
                    result.put("updatedAt", p[7]);
                }
                return result;
            }
        }
        return null;
    }

    public synchronized void writeLog(String userId,
                                      String userName,
                                      String role,
                                      String action,
                                      String detail,
                                      String result) {
        String line = String.join("|",
                nextId("log"),
                nowDateTime(),
                safe(userId),
                safe(userName),
                safe(role),
                safe(action),
                safe(detail),
                safe(result));
        appendLineSafe(resolve(LOGS_FILE), line);
    }

    public synchronized List<Map<String, String>> getLogs() {
        List<Map<String, String>> logs = new ArrayList<>();
        for (String line : readLinesSafe(resolve(LOGS_FILE))) {
            String[] p = line.split("\\|", -1);
            if (p.length < 8) {
                continue;
            }
            Map<String, String> item = new LinkedHashMap<>();
            item.put("id", p[0]);
            item.put("time", p[1]);
            item.put("userId", p[2]);
            item.put("userName", p[3]);
            item.put("role", p[4]);
            item.put("action", p[5]);
            item.put("detail", p[6]);
            item.put("result", p[7]);
            logs.add(item);
        }
        Collections.reverse(logs);
        return logs;
    }

    public synchronized Map<String, Object> getAnalyticsSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        List<Map<String, String>> positions = getAllPositions();
        List<Map<String, String>> apps = getAllApplications();

        int totalPositions = positions.size();
        int totalApplications = apps.size();
        int totalApproved = 0;
        for (Map<String, String> a : apps) {
            if ("approved".equalsIgnoreCase(a.get("status"))) {
                totalApproved++;
            }
        }

        int unfilled = 0;
        for (Map<String, String> p : positions) {
            int openings = parseIntSafe(p.get("openings"), 0);
            int accepted = parseIntSafe(p.get("acceptedCount"), 0);
            if (accepted < openings) {
                unfilled++;
            }
        }

        summary.put("totalPositions", totalPositions);
        summary.put("totalApplications", totalApplications);
        summary.put("totalApproved", totalApproved);
        summary.put("unfilledPositions", unfilled);
        summary.put("positions", positions);
        summary.put("applications", apps);
        return summary;
    }

    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

    private String safe(String v) {
        if (v == null) {
            return "";
        }
        return v.replace("|", "/").replace("\n", " ").replace("\r", " ").trim();
    }
}
