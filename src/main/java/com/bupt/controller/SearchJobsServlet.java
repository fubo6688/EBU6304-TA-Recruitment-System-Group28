package com.bupt.controller;

import com.bupt.model.Job;
import com.bupt.utils.FileStorageUtil;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/searchJobs")
public class SearchJobsServlet extends HttpServlet {
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String query = request.getParameter("query");
        List<Job> allJobs = FileStorageUtil.getAllJobs();
        
        List<Job> filteredJobs;
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase();
            filteredJobs = allJobs.stream()
                .filter(job -> job.getModuleName().toLowerCase().contains(lowerQuery) ||
                               job.getRole().toLowerCase().contains(lowerQuery) ||
                               job.getRequiredSkills().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
        } else {
            filteredJobs = allJobs;
        }
        
        request.setAttribute("jobList", filteredJobs);
        request.setAttribute("query", query);
        request.getRequestDispatcher("jobList.jsp").forward(request, response);
    }
}