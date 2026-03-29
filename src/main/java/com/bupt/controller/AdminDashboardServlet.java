<<<<<<< HEAD
package com.bupt.controller;

import com.bupt.utils.FileStorageUtil;
import java.io.IOException;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/adminDashboard")
public class AdminDashboardServlet extends HttpServlet {
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 获取所有 TA 已被接受的工作数量
        Map<String, Integer> workloadMap = FileStorageUtil.getTAWorkload();
        
        request.setAttribute("workload", workloadMap);
        request.getRequestDispatcher("admin.jsp").forward(request, response);
    }
=======
package com.bupt.controller;

import com.bupt.utils.FileStorageUtil;
import java.io.IOException;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/adminDashboard")
public class AdminDashboardServlet extends HttpServlet {
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 获取所有 TA 已被接受的工作数量
        Map<String, Integer> workloadMap = FileStorageUtil.getTAWorkload();
        
        request.setAttribute("workload", workloadMap);
        request.getRequestDispatcher("admin.jsp").forward(request, response);
    }
>>>>>>> 655e964 (BUPT TA Recruitment System - complete implementation with unified UI)
}