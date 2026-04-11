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

    // 初始化数据目录并确保默认数据文件存在。
    public DataManager() {
        this.dataDir = resolveDataDir();
        ensureDataDirAndDefaults();
    }

    // 解析数据目录位置：优先系统参数/环境变量，其次按部署结构自动推断，最后回退到 data。
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

    // 从运行时代码位置推断 Web 应用根目录下的 data 目录。
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

    // 创建数据目录、数据文件，并在首次启动时注入默认账号。
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

    // 确保指定文件存在，不存在则创建空文件。
    private void ensureFile(String fileName) throws IOException {
        Path file = resolve(fileName);
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
    }

    // 组合数据目录与文件名，得到实际文件路径。
    private Path resolve(String fileName) {
        return dataDir.resolve(fileName);
    }

    // 暴露数据目录路径供文件上传等功能复用。
    public Path getDataDirPath() {
        return dataDir;
    }

    // 安全读取文本行：文件不存在返回空列表，避免调用方处理异常分支。
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

    // 安全覆盖写入文件内容。
    private void writeLinesSafe(Path path, List<String> lines) {
        try {
            Files.write(path, lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 安全追加一行到文件末尾。
    private void appendLineSafe(Path path, String line) {
        try {
            Files.write(path, Collections.singletonList(line), StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 获取当前日期时间字符串。
    private String nowDateTime() {
        synchronized (DATE_TIME_FORMAT) {
            return DATE_TIME_FORMAT.format(new Date());
        }
    }

    // 获取当前日期字符串。
    private String todayDate() {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(new Date());
        }
    }

    // 生成基于时间戳的简易主键。
    private String nextId(String prefix) {
        return prefix + System.currentTimeMillis();
    }

    // 读取并反序列化全部用户。
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

    // 按 userId 查询用户。
    public synchronized User getUserById(String userId) {
        return getAllUsers().stream()
                .filter(u -> u.getUserId().equalsIgnoreCase(userId))
                .findFirst()
                .orElse(null);
    }

    // 校验登录凭证：要求账号处于 active 且密码匹配。
    public synchronized User validateLogin(String userId, String password) {
        User user = getUserById(userId);
        if (user != null && "active".equalsIgnoreCase(user.getStatus()) && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    // 用户保存采用 upsert：同 userId 先删后加。
    public synchronized void saveUser(User user) {
        List<User> users = getAllUsers();
        users.removeIf(u -> u.getUserId().equalsIgnoreCase(user.getUserId()));
        users.add(user);
        saveAllUsers(users);
    }

    // 将用户列表整体写回 users.txt。
    private synchronized void saveAllUsers(List<User> users) {
        List<String> lines = users.stream().map(User::toString).collect(Collectors.toList());
        writeLinesSafe(resolve(USERS_FILE), lines);
    }

    // 初始化系统默认账号，方便开发和首次运行验证流程。
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

    // 读取全部岗位并按固定列映射为键值结构。
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

    // 按岗位 ID 查询单条岗位。
    public synchronized Map<String, String> getPositionById(String positionId) {
        for (Map<String, String> item : getAllPositions()) {
            if (positionId.equals(item.get("id"))) {
                return item;
            }
        }
        return null;
    }

    // 创建岗位并写入 positions.txt。
    // 字段采用固定列序，方便后续按列回读与更新。
    public synchronized Map<String, String> createPosition(String title,
                                                           String department,
                                                           String salary,
                                                           String description,
                                                           String requirements,
                                                           String moId,
                                                           String openings,
                                                           String deadline) {
        // File-store write format for positions.txt:
        // id|title|department|salary|description|requirements|moId|openings|
        // appliedCount|acceptedCount|status|createdAt|deadline
        // ID is generated server-side to guarantee uniqueness in this storage model.
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

    // 更新岗位开放状态（open/closed）。
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

    // 更新岗位基础信息：只覆盖非空参数，避免误清空已有字段。
    public synchronized boolean updatePosition(String positionId,
                                               String title,
                                               String department,
                                               String salary,
                                               String description,
                                               String requirements,
                                               String openings,
                                               String deadline) {
        List<Map<String, String>> positions = getAllPositions();
        boolean found = false;
        List<String> lines = new ArrayList<>();

        for (Map<String, String> p : positions) {
            if (positionId.equals(p.get("id"))) {
                if (title != null && !title.trim().isEmpty()) {
                    p.put("title", safe(title));
                }
                if (department != null && !department.trim().isEmpty()) {
                    p.put("department", safe(department));
                }
                if (salary != null && !salary.trim().isEmpty()) {
                    p.put("salary", safe(salary));
                }
                if (description != null && !description.trim().isEmpty()) {
                    p.put("description", safe(description));
                }
                if (requirements != null && !requirements.trim().isEmpty()) {
                    p.put("requirements", safe(requirements));
                }
                if (deadline != null && !deadline.trim().isEmpty()) {
                    p.put("deadline", safe(deadline));
                }
                if (openings != null && !openings.trim().isEmpty()) {
                    int n = parseIntSafe(openings, parseIntSafe(p.get("openings"), 1));
                    p.put("openings", String.valueOf(Math.max(1, n)));
                }
                found = true;
            }
            lines.add(toPositionLine(p));
        }

        if (found) {
            writeLinesSafe(resolve(POSITIONS_FILE), lines);
        }
        return found;
    }

    // 将岗位 Map 重新编码为文件存储行。
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

    // 读取全部申请记录。
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

    // 按申请人查询申请记录。
    public synchronized List<Map<String, String>> getApplicationsByUser(String userId) {
        return getAllApplications().stream()
                .filter(a -> userId.equals(a.get("userId")))
                .collect(Collectors.toList());
    }

    // 按岗位查询申请记录。
    public synchronized List<Map<String, String>> getApplicationsByPosition(String positionId) {
        return getAllApplications().stream()
                .filter(a -> positionId.equals(a.get("positionId")))
                .collect(Collectors.toList());
    }

    // 按申请 ID 查询单条申请。
    public synchronized Map<String, String> getApplicationById(String applicationId) {
        for (Map<String, String> app : getAllApplications()) {
            if (applicationId.equals(app.get("id"))) {
                return app;
            }
        }
        return null;
    }

    // 提交申请：同一 TA 对同一岗位若已有未取消申请，则直接返回已有记录。
    public synchronized Map<String, String> submitApplication(String userId,
                                                               String userName,
                                                               String positionId,
                                                               String priority) {
        Map<String, String> position = getPositionById(positionId);
        if (position == null) {
            return null;
        }

        for (Map<String, String> app : getAllApplications()) {
            // Business guard: one active application per TA per position.
            // If an existing non-canceled record exists, return it instead of creating new.
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

    // 修改申请优先级。
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

    // 取消申请：本质是将状态更新为 canceled。
    public synchronized boolean cancelApplication(String applicationId) {
        return updateApplicationStatus(applicationId, "canceled", "申请已撤回");
    }

    // 处理审核决策并规范化到 approved/rejected 状态。
    public synchronized boolean processApplication(String applicationId, String decision, String feedback) {
        String status = "rejected";
        if ("approved".equalsIgnoreCase(decision) || "accept".equalsIgnoreCase(decision) || "accepted".equalsIgnoreCase(decision)) {
            status = "approved";
        }
        return updateApplicationStatus(applicationId, status, feedback);
    }

    // 通用申请状态更新入口。
    // 同时维护岗位计数字段，保证 appliedCount/acceptedCount 一致性。
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
            if (!"approved".equalsIgnoreCase(status) && "approved".equalsIgnoreCase(oldStatus) && positionId != null) {
                incrementPositionCounter(positionId, "acceptedCount", -1);
            }
            if ("canceled".equalsIgnoreCase(status) && !"canceled".equalsIgnoreCase(oldStatus) && positionId != null) {
                incrementPositionCounter(positionId, "appliedCount", -1);
            }
        }
        return found;
    }

    // 将申请 Map 重新编码为文件行。
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

    // 增减岗位计数器并保证不小于 0。
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

    // 简化通知保存入口，默认系统通知类型。
    public synchronized void saveNotification(String userId, String notification) {
        saveNotification(userId, "system", "系统通知", notification);
    }

    // 写入一条结构化通知记录。
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

    // 读取用户通知列表，兼容旧版纯文本通知格式。
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

    // 批量标记通知已读。
    public synchronized void markAllNotificationsRead(String userId) {
        List<Map<String, String>> list = getNotifications(userId);
        List<String> lines = new ArrayList<>();
        for (Map<String, String> item : list) {
            item.put("read", "1");
            lines.add(toNotificationLine(item));
        }
        writeLinesSafe(resolve(userId + "_notifications.txt"), lines);
    }

    // 标记单条通知已读。
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

    // 将通知 Map 转回文件存储行。
    private String toNotificationLine(Map<String, String> n) {
        return String.join("|",
                safe(n.get("id")),
                safe(n.get("type")),
                safe(n.get("title")),
                safe(n.get("message")),
                safe(n.get("time")),
                safe(n.get("read")));
    }

    // 保存用户资料（upsert）。
    // 同一 userId 更新同一行，避免重复资料记录。
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
        // Profile persistence strategy: upsert by userId in profiles.txt.
        // This allows repeated edits from TA profile page without duplicate rows.
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

    // 读取用户资料，兼容不同历史版本的列数量。
    public synchronized Map<String, String> getProfile(String userId) {
        // Backward compatibility:
        // support both legacy profile rows and newer extended rows
        // (resumeStoredName/availableTime/avatarStoredName/updatedAt).
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

    // 记录系统操作日志，供管理员审计和导出。
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

    // 读取操作日志并按时间倒序返回。
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

    // 聚合系统概览指标，用于管理员分析看板。
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

    // 统计 TA 工作量摘要，供管理员查看申请分布与负载。
    public synchronized List<Map<String, Object>> getTaWorkloadSummary() {
        List<Map<String, Object>> workload = new ArrayList<>();
        List<Map<String, String>> apps = getAllApplications();

        for (User user : getAllUsers()) {
            if (!"TA".equalsIgnoreCase(user.getRole())) {
                continue;
            }

            int total = 0;
            int pending = 0;
            int approved = 0;
            int rejected = 0;
            int canceled = 0;

            for (Map<String, String> app : apps) {
                if (!user.getUserId().equalsIgnoreCase(safe(app.get("userId")))) {
                    continue;
                }
                total++;
                String status = safe(app.get("status")).toLowerCase(Locale.ROOT);
                if ("approved".equals(status)) {
                    approved++;
                } else if ("rejected".equals(status)) {
                    rejected++;
                } else if ("canceled".equals(status)) {
                    canceled++;
                } else {
                    pending++;
                }
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", user.getUserId());
            item.put("userName", user.getUserName());
            item.put("qmId", user.getQmId());
            item.put("status", user.getStatus());
            item.put("totalApplications", total);
            item.put("pending", pending);
            item.put("approved", approved);
            item.put("rejected", rejected);
            item.put("canceled", canceled);
            item.put("currentLoad", approved);
            workload.add(item);
        }

        workload.sort((a, b) -> {
            int ca = (Integer) a.get("currentLoad");
            int cb = (Integer) b.get("currentLoad");
            if (cb != ca) {
                return cb - ca;
            }
            String na = String.valueOf(a.get("userName"));
            String nb = String.valueOf(b.get("userName"));
            return na.compareToIgnoreCase(nb);
        });

        return workload;
    }

    // 安全数字解析，失败时返回默认值。
    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

    // 文本清洗：移除换行和分隔符冲突字符，防止破坏文件格式。
    private String safe(String v) {
        if (v == null) {
            return "";
        }
        return v.replace("|", "/").replace("\n", " ").replace("\r", " ").trim();
    }
}
