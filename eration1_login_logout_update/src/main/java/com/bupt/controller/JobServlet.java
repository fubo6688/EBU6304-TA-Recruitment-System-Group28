package com.bupt.controller;

import com.bupt.model.Job;
import com.bupt.utils.FileStorageUtil;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 使用 @WebServlet 注解映射 URL，无需配置 web.xml
@WebServlet("/postJob")
public class JobServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 1. 设置请求编码，防止中文乱码
        request.setCharacterEncoding("UTF-8");
        
        // 2. 获取 JSP 表单提交的参数
        String moduleName = request.getParameter("moduleName");
        String role = request.getParameter("role");
        String requiredSkills = request.getParameter("requiredSkills");
        
        // 3. 实例化 Job 对象
        Job newJob = new Job(moduleName, role, requiredSkills);

        // 4. 持久化保存到文本文件
        FileStorageUtil.saveJob(newJob);

        // 5. 将成功信息传递给前端并进行页面跳转
        request.setAttribute("message", "Job for " + moduleName + " successfully posted!");
        request.getRequestDispatcher("success.jsp").forward(request, response);
    }
}