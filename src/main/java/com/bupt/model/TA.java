package com.bupt.model;

import java.io.*;
import java.util.*;

public class TA extends User {
    // 默认档案存储文件（项目当前采用 CSV 文本持久化）
    private static final String DEFAULT_PROFILE_FILE = "profiles.csv";

    private final String taId;
    private String major;
    private List<String> skillList;
    private String cvPath;

    public TA(String username, String password, String email, String major, List<String> skillList, String cvPath) {
        this(generateTaId(), username, password, email, major, skillList, cvPath);
    }

    public TA(String taId, String username, String password, String email, String major, List<String> skillList, String cvPath) {
        super(taId, username, password, email);
        this.taId = taId;
        setMajor(major);
        setSkillList(skillList);
        setCvPath(cvPath);
    }

    private static String generateTaId() {
        // 使用 UUID 截断生成短 ID，满足“唯一 TA ID”需求
        return "TA_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    public String getTaId() {
        return taId;
    }

    public String getMajor() {
        return major;
    }

    public List<String> getSkillList() {
        // 返回只读视图，避免外部直接修改内部技能列表
        return Collections.unmodifiableList(skillList);
    }

    public String getCvPath() {
        return cvPath;
    }

    public void setMajor(String major) {
        // Major 为必填字段
        if (major == null || major.trim().isEmpty()) {
            throw new IllegalArgumentException("Major is mandatory.");
        }
        this.major = major.trim();
    }

    public void setSkillList(List<String> skillList) {
        // Skill List 为必填字段，且需要去除空值和空白项
        if (skillList == null || skillList.isEmpty()) {
            throw new IllegalArgumentException("Skill list is mandatory.");
        }

        List<String> cleaned = new ArrayList<>();
        for (String skill : skillList) {
            if (skill != null) {
                String trimmed = skill.trim();
                if (!trimmed.isEmpty()) {
                    cleaned.add(trimmed);
                }
            }
        }

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Skill list is mandatory.");
        }

        this.skillList = cleaned;
    }

    public void setCvPath(String cvPath) {
        // CV 路径必填，且仅允许 TXT/CSV 文件
        if (cvPath == null || cvPath.trim().isEmpty()) {
            throw new IllegalArgumentException("CV path is mandatory.");
        }

        String normalized = cvPath.trim();
        String lower = normalized.toLowerCase();
        if (!(lower.endsWith(".txt") || lower.endsWith(".csv"))) {
            throw new IllegalArgumentException("CV path must point to a .txt or .csv file.");
        }
        this.cvPath = normalized;
    }

    public void editProfile(String major, List<String> skillList, String cvPath) {
        setMajor(major);
        setSkillList(skillList);
        setCvPath(cvPath);
    }

    public String viewProfile() {
        return "TA ID: " + taId
                + "\nName: " + getUsername()
                + "\nEmail: " + getEmail()
                + "\nMajor: " + major
                + "\nSkills: " + String.join(" | ", skillList)
                + "\nCV Path: " + cvPath;
    }

    public String toCSV() {
        // skills 使用 | 连接，避免与 CSV 的逗号分隔冲突
        return taId + ","
                + sanitize(getUsername()) + ","
                + sanitize(getEmail()) + ","
                + sanitize(major) + ","
                + sanitize(String.join("|", skillList)) + ","
                + sanitize(cvPath);
    }

    public void saveProfile() {
        saveProfile(DEFAULT_PROFILE_FILE);
    }

    public void saveProfile(String filePath) {
        // 追加写入：保留历史记录（新建档案场景）
        try (PrintWriter out = new PrintWriter(new FileWriter(filePath, true))) {
            out.println(toCSV());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save TA profile.", e);
        }
    }

    public void updateProfile(String filePath) {
        File file = new File(filePath);
        List<String> lines = new ArrayList<>();
        boolean replaced = false;

        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",", 2);
                    // 以 taId 作为唯一键：命中则替换整行，否则保留原记录
                    if (parts.length > 0 && taId.equals(parts[0])) {
                        lines.add(toCSV());
                        replaced = true;
                    } else {
                        lines.add(line);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read TA profile file.", e);
            }
        }

        if (!replaced) {
            // 如果不存在原记录，按“新建档案”处理
            lines.add(toCSV());
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(file, false))) {
            for (String line : lines) {
                out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to update TA profile.", e);
        }
    }

    public static TA viewProfile(String filePath, String taId) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 6);
                // 固定 6 列：taId, username, email, major, skills, cvPath
                if (parts.length == 6 && taId.equals(parts[0])) {
                    List<String> skills = parseSkills(parts[4]);
                    return new TA(parts[0], parts[1], "", parts[2], parts[3], skills, parts[5]);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load TA profile.", e);
        }

        return null;
    }

    private static List<String> parseSkills(String serializedSkills) {
        List<String> skills = new ArrayList<>();
        if (serializedSkills == null || serializedSkills.trim().isEmpty()) {
            return skills;
        }

        String[] parts = serializedSkills.split("\\|");
        // 反序列化技能列表：按 | 切分并清洗空值
        for (String part : parts) {
            String skill = part.trim();
            if (!skill.isEmpty()) {
                skills.add(skill);
            }
        }
        return skills;
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        // 轻量清洗，避免字段中的逗号破坏 CSV 列结构
        return value.replace(",", " ").trim();
    }
}