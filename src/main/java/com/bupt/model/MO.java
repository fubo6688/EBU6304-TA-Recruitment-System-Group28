package com.bupt.model;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

// MO 类继承自 User，增加了 MO 特有的属性和方法，主要负责创建和发布岗位，目前逻辑未接入Servlet/JSP
public class MO extends User {
    // MO 发布岗位默认写入文件（简化版持久化）
    private static final String DEFAULT_MO_JOB_FILE = "mo_jobs.csv";

    private final String moId;

    public MO(String moId, String username, String password, String email) {
        // 让 User.userId 与 moId 对齐，便于后续统一按用户 ID 检索
        super(requireNonBlank(moId, "MO ID is required."), username, password, email);
        this.moId = moId.trim();
    }

    public String getMoId() {
        return moId;
    }

    // 创建并发布岗位：生成唯一 Job ID，默认状态 OPEN，并立即持久化到 CSV
    public MOJobPosting createAndPublishJobPosting(
            String courseName,
            List<String> requiredSkills,
            int weeklyWorkload,
            String applicationDeadline
    ) {
        MOJobPosting posting = new MOJobPosting(
                generateJobId(),
                moId,
                courseName,
                requiredSkills,
                weeklyWorkload,
                applicationDeadline,
                "OPEN"
        );

        posting.saveToFile(DEFAULT_MO_JOB_FILE);
        return posting;
    }

    private static String generateJobId() {
        // 生成短格式岗位 ID，示例：JOB_A1B2C3D4
        return "JOB_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static class MOJobPosting {
        // Job ID / MO ID 设为 final，保证发布后不可修改
        private final String jobId;
        private final String moId;
        private final String courseName;
        private final List<String> requiredSkills;
        private final int weeklyWorkload;
        private final String applicationDeadline;
        private final String status;

        private MOJobPosting(
                String jobId,
                String moId,
                String courseName,
                List<String> requiredSkills,
                int weeklyWorkload,
                String applicationDeadline,
                String status
        ) {
            // 以下字段在创建时一次性校验，后续只读，不提供 setter
            this.jobId = requireNonBlank(jobId, "Job ID is required.").trim();
            this.moId = requireNonBlank(moId, "MO ID is required.").trim();
            this.courseName = requireNonBlank(courseName, "Course name is required.").trim();
            this.requiredSkills = normalizeSkills(requiredSkills);
            this.weeklyWorkload = validateWeeklyWorkload(weeklyWorkload);
            this.applicationDeadline = validateDeadline(applicationDeadline);
            // 当前规则固定默认 OPEN，status 参数预留给后续扩展状态流转
            this.status = "OPEN";
        }

        public String getJobId() {
            return jobId;
        }

        public String getMoId() {
            return moId;
        }

        public String getCourseName() {
            return courseName;
        }

        public List<String> getRequiredSkills() {
            // 返回只读集合，避免外部修改岗位技能要求
            return Collections.unmodifiableList(requiredSkills);
        }

        public int getWeeklyWorkload() {
            return weeklyWorkload;
        }

        public String getApplicationDeadline() {
            return applicationDeadline;
        }

        public String getStatus() {
            return status;
        }

        public String toCSV() {
            // 技能列表用 | 连接，避免与 CSV 的逗号分隔冲突
            return jobId + ","
                    + moId + ","
                    + sanitize(courseName) + ","
                    + sanitize(String.join("|", requiredSkills)) + ","
                    + weeklyWorkload + ","
                    + applicationDeadline + ","
                    + status;
        }

        public void saveToFile(String filePath) {
            // 发布即落盘：追加写入，不覆盖历史岗位
            try (PrintWriter out = new PrintWriter(new FileWriter(filePath, true))) {
                out.println(toCSV());
            } catch (IOException e) {
                throw new RuntimeException("Failed to save MO job posting.", e);
            }
        }

        private static List<String> normalizeSkills(List<String> requiredSkills) {
            // 必填且过滤空白项，确保落盘数据可直接用于匹配
            if (requiredSkills == null || requiredSkills.isEmpty()) {
                throw new IllegalArgumentException("Required skills are mandatory.");
            }

            List<String> cleaned = new ArrayList<>();
            for (String skill : requiredSkills) {
                if (skill != null) {
                    String value = skill.trim();
                    if (!value.isEmpty()) {
                        cleaned.add(value);
                    }
                }
            }

            if (cleaned.isEmpty()) {
                throw new IllegalArgumentException("Required skills are mandatory.");
            }

            return cleaned;
        }

        private static int validateWeeklyWorkload(int weeklyWorkload) {
            // 需求约束：每周工时 1-15 小时
            if (weeklyWorkload < 1 || weeklyWorkload > 15) {
                throw new IllegalArgumentException("Weekly workload must be between 1 and 15 hours.");
            }
            return weeklyWorkload;
        }

        private static String validateDeadline(String applicationDeadline) {
            String value = requireNonBlank(applicationDeadline, "Application deadline is required.").trim();
            try {
                // 使用 ISO 日期格式 yyyy-MM-dd，便于后续排序和筛选
                LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Application deadline must use yyyy-MM-dd format.");
            }
            return value;
        }

        private static String sanitize(String value) {
            // 轻量清洗，避免字段中逗号导致 CSV 列错位
            return value == null ? "" : value.replace(",", " ").trim();
        }
    }
}