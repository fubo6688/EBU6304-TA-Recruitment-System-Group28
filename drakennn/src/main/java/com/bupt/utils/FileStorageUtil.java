mvnpackage com.bupt.utils;

import com.bupt.model.Application;
import com.bupt.model.Job;
import java.io.*;
import java.util.*;

public class FileStorageUtil {
    private static final String APP_FILE = "applications.csv";
    private static final String JOB_FILE = "jobs.csv";
    private static final String PROFILE_FILE = "profiles.csv";

    // --- 保存申请 ---
    public static synchronized void saveApplication(Application app) {
        try (PrintWriter out = new PrintWriter(new FileWriter(APP_FILE, true))) {
            out.println(app.toCSV());
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- 保存工作 ---
    public static synchronized void saveJob(Job job) {
        try (PrintWriter out = new PrintWriter(new FileWriter(JOB_FILE, true))) {
            out.println(job.toCSV());
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- 获取所有工作 ---
    public static List<Job> getAllJobs() {
        List<Job> jobs = new ArrayList<>();
        File file = new File(JOB_FILE);
        if (!file.exists()) return jobs;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 4);
                if (parts.length >= 4) {
                    Job job = new Job(parts[1], parts[2], parts[3]);
                    job.setJobId(parts[0]);
                    jobs.add(job);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return jobs;
    }

    // --- TA 功能：保存个人档案和简历路径 ---
    public static synchronized void saveProfile(String taId, String name, String skills, String cvPath) {
        try (PrintWriter out = new PrintWriter(new FileWriter(PROFILE_FILE, true))) {
            out.println(taId + "," + name + "," + skills + "," + cvPath);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- Get all applications ---
    public static List<Application> getAllApplications() {
        List<Application> apps = new ArrayList<>();
        File file = new File(APP_FILE);
        if (!file.exists()) return apps;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 5);
                if (parts.length >= 4) {
                    Application app = new Application(parts[1], parts[2], parts[3]);
                    app.setApplicationId(parts[0]);
                    if (parts.length >= 5) {
                        app.setStatus(parts[4]);
                    }
                    apps.add(app);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return apps;
    }

    // --- Update application status (accept/reject) ---
    public static synchronized void updateApplicationStatus(String appId, String newStatus) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(APP_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 5);
                if (parts[0].equals(appId)) {
                    if (parts.length >= 5) {
                        parts[4] = newStatus;
                    }
                    line = String.join(",", parts);
                }
                lines.add(line);
            }
        } catch (IOException e) { e.printStackTrace(); }

        try (PrintWriter out = new PrintWriter(new FileWriter(APP_FILE, false))) {
            for (String l : lines) { out.println(l); }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- Admin 功能：统计 TA 工作量 ---
    public static Map<String, Integer> getTAWorkload() {
        Map<String, Integer> workload = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(APP_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                // 仅统计被 ACCEPTED 的工作 (状态在第 3 列，TA_ID 在第 2 列)
                if (parts.length >= 5 && "ACCEPTED".equals(parts[4])) {
                    String taId = parts[2];
                    workload.put(taId, workload.getOrDefault(taId, 0) + 1);
                }
            }
        } catch (IOException e) { }
        return workload;
    }
}