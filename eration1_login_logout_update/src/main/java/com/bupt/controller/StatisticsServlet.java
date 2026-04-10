package com.bupt.controller;

import com.bupt.model.Application;
import com.bupt.model.Job;
import com.bupt.utils.FileStorageUtil;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/statistics")
public class StatisticsServlet extends HttpServlet {
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 获取统计数据
        List<Job> jobs = FileStorageUtil.getAllJobs();
        List<Application> applications = FileStorageUtil.getAllApplications();
        Map<String, Integer> workload = FileStorageUtil.getTAWorkload();
        
        // 计算统计
        int totalJobs = jobs.size();
        int totalApplications = applications.size();
        int acceptedApplications = (int) applications.stream()
            .filter(app -> "ACCEPTED".equals(app.getStatus()))
            .count();
        int rejectedApplications = (int) applications.stream()
            .filter(app -> "REJECTED".equals(app.getStatus()))
            .count();
        int pendingApplications = totalApplications - acceptedApplications - rejectedApplications;
        
        // 按模块统计工作
        Map<String, Long> jobsByModule = jobs.stream()
            .collect(Collectors.groupingBy(Job::getModuleName, Collectors.counting()));
        
        // 按角色统计工作
        Map<String, Long> jobsByRole = jobs.stream()
            .collect(Collectors.groupingBy(Job::getRole, Collectors.counting()));
        
        request.setAttribute("totalJobs", totalJobs);
        request.setAttribute("totalApplications", totalApplications);
        request.setAttribute("acceptedApplications", acceptedApplications);
        request.setAttribute("rejectedApplications", rejectedApplications);
        request.setAttribute("pendingApplications", pendingApplications);
        request.setAttribute("jobsByModule", jobsByModule);
        request.setAttribute("jobsByRole", jobsByRole);
        request.setAttribute("workload", workload);
        
        request.getRequestDispatcher("statistics.jsp").forward(request, response);
    }
}