<<<<<<< HEAD
package com.bupt.model;

import java.util.UUID;

public class Job {
    private String jobId;
    private String moduleName;
    private String role; 
    private String requiredSkills;

    // 构造函数
    public Job(String moduleName, String role, String requiredSkills) {
        // 自动生成一个简短的唯一ID
        this.jobId = UUID.randomUUID().toString().substring(0, 8);
        this.moduleName = moduleName;
        this.role = role;
        this.requiredSkills = requiredSkills;
    }

    // 将对象数据拼接为 CSV 格式的一行
    public String toCSV() {
        // 注意：实际开发中需处理用户输入中自带逗号的情况，这里为了演示保持简单
        return jobId + "," + moduleName + "," + role + "," + requiredSkills;
    }

    // Getters
    public String getJobId() { return jobId; }
    public String getModuleName() { return moduleName; }
    public String getRole() { return role; }
    public String getRequiredSkills() { return requiredSkills; }


// 在你的 Job.java 的最后，加上这一行：
public void setJobId(String jobId) { this.jobId = jobId; 
    
}
=======
package com.bupt.model;

import java.util.UUID;

public class Job {
    private String jobId;
    private String moduleName;
    private String role; 
    private String requiredSkills;

    // 构造函数
    public Job(String moduleName, String role, String requiredSkills) {
        // 自动生成一个简短的唯一ID
        this.jobId = UUID.randomUUID().toString().substring(0, 8);
        this.moduleName = moduleName;
        this.role = role;
        this.requiredSkills = requiredSkills;
    }

    // 将对象数据拼接为 CSV 格式的一行
    public String toCSV() {
        // 注意：实际开发中需处理用户输入中自带逗号的情况，这里为了演示保持简单
        return jobId + "," + moduleName + "," + role + "," + requiredSkills;
    }

    // Getters
    public String getJobId() { return jobId; }
    public String getModuleName() { return moduleName; }
    public String getRole() { return role; }
    public String getRequiredSkills() { return requiredSkills; }
>>>>>>> 672021a (Initial commit - Java 11 baseline)
}