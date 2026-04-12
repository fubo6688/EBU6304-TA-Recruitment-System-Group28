package com.bupt.model;

import java.util.UUID;

public class Application {
    private String applicationId;
    private String jobId;
    private String applicantName;
    private String applicantEmail;
    private String status;

    public Application(String jobId, String applicantName, String applicantEmail) {
        this.applicationId = UUID.randomUUID().toString().substring(0, 8);
        this.jobId = jobId;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.status = "PENDING";
    }

    public String toCSV() {
        return applicationId + "," + jobId + "," + applicantName + "," + applicantEmail + "," + status;
    }

    // Getters
    public String getApplicationId() { return applicationId; }
    public String getJobId() { return jobId; }
    public String getApplicantName() { return applicantName; }
    public String getApplicantEmail() { return applicantEmail; }
    public String getStatus() { return status; }

    // Setters
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public void setStatus(String status) { this.status = status; }
}