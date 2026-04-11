package com.bupt.controller;

import com.bupt.model.Job;
import com.bupt.utils.FileStorageUtil;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/jobs")
public class JobListServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 从 CSV 读取所有工作
        List<Job> jobList = FileStorageUtil.getAllJobs();
        
        // 把数据放进 request 中，传给 JSP 页面
        request.setAttribute("jobList", jobList);
        request.getRequestDispatcher("jobList.jsp").forward(request, response);
    }
}