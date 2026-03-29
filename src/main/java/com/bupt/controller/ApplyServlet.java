<<<<<<< HEAD
package com.bupt.controller;

import com.bupt.model.Application;
import com.bupt.utils.FileStorageUtil;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/apply")
public class ApplyServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        
        String jobId = request.getParameter("jobId");
        String applicantName = request.getParameter("applicantName");
        String applicantEmail = request.getParameter("applicantEmail");
        
        Application app = new Application(jobId, applicantName, applicantEmail);
        FileStorageUtil.saveApplication(app);
        
        request.setAttribute("moduleName", "Job ID: " + jobId); // 复用之前的成功页面
        request.getRequestDispatcher("success.jsp").forward(request, response);
    }
=======
package com.bupt.controller;

import com.bupt.model.Application;
import com.bupt.utils.FileStorageUtil;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/apply")
public class ApplyServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        
        String jobId = request.getParameter("jobId");
        String applicantName = request.getParameter("applicantName");
        String applicantEmail = request.getParameter("applicantEmail");
        
        Application app = new Application(jobId, applicantName, applicantEmail);
        FileStorageUtil.saveApplication(app);
        
        request.setAttribute("moduleName", "Job ID: " + jobId); // 复用之前的成功页面
        request.getRequestDispatcher("success.jsp").forward(request, response);
    }
>>>>>>> 655e964 (BUPT TA Recruitment System - complete implementation with unified UI)
}