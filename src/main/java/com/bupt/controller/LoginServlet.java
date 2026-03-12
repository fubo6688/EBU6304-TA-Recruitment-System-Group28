package com.bupt.controller;

import java.io.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String userIn = request.getParameter("username");
        String passIn = request.getParameter("password");
        
        // 读取 users.csv 验证身份
        
        try (BufferedReader br = new BufferedReader(new FileReader("users.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] u = line.split(",");
                if (u[0].equals(userIn) && u[1].equals(passIn)) {
                    // 登录成功：将用户信息存入 Session
                    HttpSession session = request.getSession();
                    session.setAttribute("username", u[0]);
                    session.setAttribute("role", u[2]); // 存入角色：MO/TA/ADMIN
                    session.setAttribute("name", u[3]);
                    
                    response.sendRedirect("index.jsp");
                    return;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        
        // 失败则返回登录页并报错
        response.sendRedirect("login.jsp?error=1");
    }
}