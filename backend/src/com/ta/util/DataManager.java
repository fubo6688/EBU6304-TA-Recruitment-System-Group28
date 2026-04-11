package com.ta.util;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import com.ta.model.User;

/**
 * 数据访问与文本持久化网关。
 *
 * <p>该类统一管理 users/positions/applications/profiles/logs/notifications 的读写，
 * 并提供面向 Servlet 的聚合查询能力（例如管理员仪表盘与 TA workload 统计）。</p>
 */
public class DataManager {
    // 统一数据目录（用户、岗位、申请、日志、资料）入口。
    private final Path dataDir;

    private static final String USERS_FILE = "users.txt";
    private static final String POSITIONS_FILE = "positions.txt";
    private static final String APPLICATIONS_FILE = "applications.txt";
    private static final String LOGS_FILE = "logs.txt";
    private static final String PROFILES_FILE = "profiles.txt";

    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 构造时解析最终数据目录，并确保基础数据文件存在。
     */
    public DataManager() {
        // 启动时解析 data 目录并确保基础文件可用。
        this.dataDir = resolveDataDir();
        ensureDataDirAndDefaults();
    }

    /**
     * 解析最终数据目录：系统属性 > 环境变量 > 部署目录推断 > 工作区推断 > 默认 data。
     */
    private Path resolveDataDir() {
        // 优先读取显式配置，便于在不同部署环境下复用同一套代码。
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

        Path workspaceData = resolveWorkspaceDataDir();
        if (workspaceData != null) {
            return workspaceData;
        }

        return Paths.get("data");
    }

    /**
     * 在本地开发场景下尝试推断工作区根目录的 data 路径。
     */
    private Path resolveWorkspaceDataDir() {
        try {
            // 本地开发统一回到工作区根目录 data/，避免出现 backend/data 与根 data 双写。
            Path cwd = Paths.get("").toAbsolutePath().normalize();
            if (cwd.getFileName() != null && "backend".equalsIgnoreCase(String.valueOf(cwd.getFileName()))) {
                Path parent = cwd.getParent();
                if (parent != null) {
                    return parent.resolve("data");
                }
            }

            if (Files.exists(cwd.resolve("backend")) || Files.exists(cwd.resolve("README.md"))) {
                return cwd.resolve("data");
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    /**
     * 在 Web 容器部署场景下根据 classes 位置推断 webapp/data 目录。
     */
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

    /**
     * 确保数据目录与基础文件存在，并在空库时写入默认用户种子数据。
     */
    private void ensureDataDirAndDefaults() {
        try {
            // 首次启动自动创建目录与基础文件，降低手动部署门槛。
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

    /**
     * 确保指定数据文件存在，不存在则创建空文件。
     */
    private void ensureFile(String fileName) throws IOException {
        Path file = resolve(fileName);
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
    }

    /**
     * 将逻辑文件名映射为当前 dataDir 下的绝对路径。
     */
    private Path resolve(String fileName) {
        return dataDir.resolve(fileName);
    }

    /**
     * 返回当前实例使用的运行时数据目录路径。
     */
    public Path getDataDirPath() {
        return dataDir;
    }

    /**
     * 安全读取文本文件，失败时返回空集合而不抛出到上层业务。
     */
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

    /**
     * 以覆盖方式写入整文件内容。
     */
    private void writeLinesSafe(Path path, List<String> lines) {
        try {
            Files.write(path, lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 以追加方式写入单行记录。
     */
    private void appendLineSafe(Path path, String line) {
        try {
            Files.write(path, Collections.singletonList(line), StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回当前时间字符串（yyyy-MM-dd HH:mm:ss）。
     */
    private String nowDateTime() {
        synchronized (DATE_TIME_FORMAT) {
            return DATE_TIME_FORMAT.format(new Date());
        }
    }

    /**
     * 返回当天日期字符串（yyyy-MM-dd）。
     */
    private String todayDate() {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(new Date());
        }
    }

    /**
     * 基于前缀生成简单时间戳主键。
     */
    private String nextId(String prefix) {
        return prefix + System.currentTimeMillis();
    }

    /**
     * 读取全部用户记录。
     */
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

    /**
     * 按 userId（忽略大小写）查询单个用户。
     */
    public synchronized User getUserById(String userId) {
        return getAllUsers().stream()
                .filter(u -> u.getUserId().equalsIgnoreCase(userId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 执行登录凭据校验，仅 active 账号允许通过。
     */
    public synchronized User validateLogin(String userId, String password) {
        // 登录校验与账号状态绑定，inactive/pending 不可直接登录。
        User user = getUserById(userId);
        if (user != null && "active".equalsIgnoreCase(user.getStatus()) && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    /**
     * 保存单个用户（按 userId 覆盖旧记录）。
     */
    public synchronized void saveUser(User user) {
        List<User> users = getAllUsers();
        users.removeIf(u -> u.getUserId().equalsIgnoreCase(user.getUserId()));
        users.add(user);
        saveAllUsers(users);
    }

    /**
     * 将用户集合整体持久化到 users.txt。
     */
    private synchronized void saveAllUsers(List<User> users) {
        List<String> lines = users.stream().map(User::toString).collect(Collectors.toList());
        writeLinesSafe(resolve(USERS_FILE), lines);
    }

    /**
     * 初始化默认账号数据（用于首次启动空库引导）。
     */
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

    /**
     * 读取全部岗位记录，并在读取前自动执行过期关停。
     */
    public synchronized List<Map<String, String>> getAllPositions() {
        // 每次读取岗位前先做一次过期关停，确保前端拿到的状态是最新的。
        autoCloseExpiredPositions();
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

    /**
     * 自动将截止日期早于今天且仍为 open 的岗位改为 closed。
     */
    private void autoCloseExpiredPositions() {
        // 自动将 "open 且 deadline < 今天" 的岗位改为 closed 并落盘。
        Path file = resolve(POSITIONS_FILE);
        List<String> lines = readLinesSafe(file);
        if (lines.isEmpty()) {
            return;
        }

        boolean changed = false;
        LocalDate today = LocalDate.now();
        List<String> updated = new ArrayList<>(lines.size());

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                updated.add(line);
                continue;
            }

            String[] p = line.split("\\|", -1);
            if (p.length >= 13) {
                String status = p[10] == null ? "" : p[10].trim().toLowerCase(Locale.ROOT);
                String deadline = p[12] == null ? "" : p[12].trim();
                if ("open".equals(status) && isPastDeadline(deadline, today)) {
                    p[10] = "closed";
                    line = String.join("|", p);
                    changed = true;
                }
            }

            updated.add(line);
        }

        if (changed) {
            writeLinesSafe(file, updated);
        }
    }

    /**
     * 判断给定截止日期是否早于今天。
     */
    private boolean isPastDeadline(String deadline, LocalDate today) {
        if (deadline == null || deadline.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDate d = LocalDate.parse(deadline.trim());
            return d.isBefore(today);
        } catch (DateTimeParseException ignore) {
            return false;
        }
    }

    /**
     * 按岗位 ID 查询岗位详情。
     */
    public synchronized Map<String, String> getPositionById(String positionId) {
        for (Map<String, String> item : getAllPositions()) {
            if (positionId.equals(item.get("id"))) {
                return item;
            }
        }
        return null;
    }

    /**
     * 创建岗位记录并返回新岗位对象。
     */
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

    /**
     * 更新岗位状态（open/closed）。
     */
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

    /**
     * 按需更新岗位字段（仅非空入参会覆盖原值）。
     */
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

    /**
     * 将岗位 Map 序列化为一行管道分隔文本。
     */
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

    /**
     * 读取全部申请记录。
     */
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

    /**
     * 按 userId 获取该用户全部申请。
     */
    public synchronized List<Map<String, String>> getApplicationsByUser(String userId) {
        return getAllApplications().stream()
                .filter(a -> userId.equals(a.get("userId")))
                .collect(Collectors.toList());
    }

    /**
     * 按岗位 ID 获取全部申请。
     */
    public synchronized List<Map<String, String>> getApplicationsByPosition(String positionId) {
        return getAllApplications().stream()
                .filter(a -> positionId.equals(a.get("positionId")))
                .collect(Collectors.toList());
    }

    /**
     * 按申请 ID 获取单条申请。
     */
    public synchronized Map<String, String> getApplicationById(String applicationId) {
        for (Map<String, String> app : getAllApplications()) {
            if (applicationId.equals(app.get("id"))) {
                return app;
            }
        }
        return null;
    }

    /**
     * 提交申请并联动岗位 appliedCount 计数。
     */
    public synchronized Map<String, String> submitApplication(String userId,
                                                               String userName,
                                                               String positionId,
                                                               String priority) {
        Map<String, String> position = getPositionById(positionId);
        if (position == null) {
            return null;
        }

        // 同一用户对同一岗位的未撤回申请只保留一条，避免重复投递。
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

    /**
     * 更新申请优先级。
     */
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

    /**
     * 撤回申请（状态置为 canceled）。
     */
    public synchronized boolean cancelApplication(String applicationId) {
        return updateApplicationStatus(applicationId, "canceled", "申请已撤回");
    }

    /**
     * 根据审核决策更新申请状态（accept/approve -> approved）。
     */
    public synchronized boolean processApplication(String applicationId, String decision, String feedback) {
        String status = "rejected";
        if ("approved".equalsIgnoreCase(decision) || "accept".equalsIgnoreCase(decision) || "accepted".equalsIgnoreCase(decision)) {
            status = "approved";
        }
        return updateApplicationStatus(applicationId, status, feedback);
    }

    /**
     * 更新申请状态并同步修正岗位申请/录用计数。
     */
    public synchronized boolean updateApplicationStatus(String applicationId, String status, String feedback) {
        // 申请状态变化会联动岗位计数（appliedCount / acceptedCount）。
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

    /**
     * 将申请 Map 序列化为一行管道分隔文本。
     */
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

    /**
     * 对岗位计数字段执行增减并保证不小于 0。
     */
    private void incrementPositionCounter(String positionId, String field, int delta) {
        // 防止计数出现负数，统一在这里做下界保护。
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

    /**
     * 保存系统通知（简化重载）。
     */
    public synchronized void saveNotification(String userId, String notification) {
        saveNotification(userId, "system", "系统通知", notification);
    }

    /**
     * 保存结构化通知到用户专属通知文件。
     */
    public synchronized void saveNotification(String userId, String type, String title, String message) {
        // 每个用户独立通知文件，读写简单且便于按用户隔离。
        String line = String.join("|",
                nextId("ntf"),
                safe(type),
                safe(title),
                safe(message),
                nowDateTime(),
                "0");
        appendLineSafe(resolve(userId + "_notifications.txt"), line);
    }

    /**
     * 读取用户通知列表并兼容历史纯文本通知格式。
     */
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
                // 兼容历史旧格式（纯文本一行），读取时补齐结构化字段。
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

    /**
     * 将指定用户全部通知标记为已读。
     */
    public synchronized void markAllNotificationsRead(String userId) {
        List<Map<String, String>> list = getNotifications(userId);
        List<String> lines = new ArrayList<>();
        for (Map<String, String> item : list) {
            item.put("read", "1");
            lines.add(toNotificationLine(item));
        }
        writeLinesSafe(resolve(userId + "_notifications.txt"), lines);
    }

    /**
     * 将单条通知标记为已读。
     */
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

    /**
     * 将通知 Map 序列化为一行管道分隔文本。
     */
    private String toNotificationLine(Map<String, String> n) {
        return String.join("|",
                safe(n.get("id")),
                safe(n.get("type")),
                safe(n.get("title")),
                safe(n.get("message")),
                safe(n.get("time")),
                safe(n.get("read")));
    }

    /**
     * 保存或覆盖用户档案。
     */
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

    /**
     * 按 userId 读取用户档案并兼容历史字段版本。
     */
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
                // 兼容历史字段版本：老数据缺 avatar/availableTime 时给默认值。
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

    /**
     * 追加写入操作日志。
     */
    public synchronized void writeLog(String userId,
                                      String userName,
                                      String role,
                                      String action,
                                      String detail,
                                      String result) {
        // 日志按时间追加，便于审计登录、审批、状态变更等关键动作。
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

    /**
     * 读取日志并按时间倒序返回。
     */
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

    /**
     * 计算管理员分析页的总览统计。
     */
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

    /**
     * 计算 TA 工作负载聚合。
     *
     * <p>统计口径：</p>
     * <ul>
     *   <li>totalApplications：该 TA 申请总数</li>
     *   <li>pending：非 approved/rejected/canceled 的申请数</li>
     *   <li>approved/rejected/canceled：按状态精确计数</li>
     *   <li>currentLoad：当前实现等于 approved 数量</li>
     * </ul>
     */
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

        // 先按当前在岗负载（approved）降序，再按姓名升序，方便 Admin 快速比较。
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

    /**
     * 安全解析整数，失败时返回默认值。
     */
    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

    /**
     * 清洗文本值，避免破坏管道分隔存储格式。
     */
    private String safe(String v) {
        if (v == null) {
            return "";
        }
        // 统一清洗分隔符和换行，避免破坏 txt 管道分隔格式。
        return v.replace("|", "/").replace("\n", " ").replace("\r", " ").trim();
    }
}
