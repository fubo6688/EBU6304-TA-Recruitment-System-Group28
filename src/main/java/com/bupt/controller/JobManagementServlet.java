package com.bupt.controller;

import com.bupt.model.Job;
import com.bupt.utils.FileStorageUtil;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/jobManagement")
public class JobManagementServlet extends HttpServlet {
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        String jobId = request.getParameter("jobId");
        
        if ("edit".equals(action) && jobId != null) {
            // 获取特定工作进行编辑
            List<Job> jobs = FileStorageUtil.getAllJobs();
            for (Job job : jobs) {
                if (job.getJobId().equals(jobId)) {
                    request.setAttribute("job", job);
                    break;
                }
            }
            request.getRequestDispatcher("editJob.jsp").forward(request, response);
        } else if ("delete".equals(action) && jobId != null) {
            // 删除工作
            deleteJob(jobId);
            response.sendRedirect("jobs");
        } else {
            // 显示所有工作列表
            List<Job> jobList = FileStorageUtil.getAllJobs();
            request.setAttribute("jobList", jobList);
            request.getRequestDispatcher("jobManagement.jsp").forward(request, response);
        }
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        
        if ("update".equals(action)) {
            String jobId = request.getParameter("jobId");
            String moduleName = request.getParameter("moduleName");
            String role = request.getParameter("role");
            String requiredSkills = request.getParameter("requiredSkills");
            
            updateJob(jobId, moduleName, role, requiredSkills);
            response.sendRedirect("jobManagement");
        }
    }
    
    private void deleteJob(String jobId) {
        // 由于FileStorageUtil没有删除方法，我们需要手动实现
        // 这里简化，直接从列表中移除并重写文件
        List<Job> jobs = FileStorageUtil.getAllJobs();
        jobs.removeIf(job -> job.getJobId().equals(jobId));
        
        // 重写jobs.csv
        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter("jobs.csv", false))) {
            for (Job job : jobs) {
                out.println(job.toCSV());
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    private void updateJob(String jobId, String moduleName, String role, String requiredSkills) {
        List<Job> jobs = FileStorageUtil.getAllJobs();
        for (Job job : jobs) {
            if (job.getJobId().equals(jobId)) {
                job.setModuleName(moduleName);
                job.setRole(role);
                job.setRequiredSkills(requiredSkills);
                break;
            }
        }
        
        // 重写jobs.csv
        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter("jobs.csv", false))) {
            for (Job job : jobs) {
                out.println(job.toCSV());
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}