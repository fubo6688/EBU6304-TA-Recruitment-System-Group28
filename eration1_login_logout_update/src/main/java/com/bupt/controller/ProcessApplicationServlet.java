package com.bupt.controller;

import com.bupt.utils.FileStorageUtil;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/processApplication")
public class ProcessApplicationServlet extends HttpServlet {
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 获取前端传来的申请ID和MO做出的决定
        String appId = request.getParameter("appId");
        String decision = request.getParameter("decision"); // "ACCEPTED" or "REJECTED"
        
        // Update status in CSV
        FileStorageUtil.updateApplicationStatus(appId, decision);
        
        response.sendRedirect("moDashboard.jsp");
    }
}