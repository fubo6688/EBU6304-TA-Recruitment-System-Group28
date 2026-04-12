package com.bupt.controller;

import com.bupt.utils.FileStorageUtil;
import java.io.File;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/createProfile")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
    maxFileSize = 1024 * 1024 * 10,      // 10 MB
    maxRequestSize = 1024 * 1024 * 100   // 100 MB
)
public class ProfileServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        // 假设用户已登录，从 Session 获取 TA_ID
        String taId = "TA_001"; // 实际开发中应为: (String) request.getSession().getAttribute("userId");
        
        String name = request.getParameter("name");
        String skills = request.getParameter("skills");

        // 处理文件上传
        Part filePart = request.getPart("cvFile");
        String fileName = filePart.getSubmittedFileName();
        
        // 保存文件到服务器的 uploads 文件夹
        String uploadPath = getServletContext().getRealPath("") + File.separator + "uploads";
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) uploadDir.mkdir();
        
        String cvFilePath = uploadPath + File.separator + fileName;
        filePart.write(cvFilePath);

        // 将档案信息和文件相对路径存入 CSV
        FileStorageUtil.saveProfile(taId, name, skills, "uploads/" + fileName);

        response.getWriter().println("Profile created and CV uploaded successfully!");
    }
}