package com.bupt.controller;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

@WebFilter("/*") // 拦截所有请求
public class AuthFilter implements Filter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        String uri = req.getRequestURI();

        // 1. 放行不需要登录的资源（登录页、登录接口、CSS等）
        boolean isLoginPage = uri.endsWith("login.jsp") || uri.endsWith("login");
        boolean isLoggedIn = (session != null && session.getAttribute("role") != null);

        if (isLoginPage || isLoggedIn) {
            // 2. 角色权限细分控制
            String role = (isLoggedIn) ? (String) session.getAttribute("role") : "";
            
            if (uri.contains("admin") && !"ADMIN".equals(role)) {
                res.getWriter().println("Access Denied: Admin only!");
                return;
            }
            if (uri.contains("postJob") && !"MO".equals(role)) {
                res.getWriter().println("Access Denied: Module Owner only!");
                return;
            }
            
            chain.doFilter(request, response); // 通过验证，放行
        } else {
            res.sendRedirect("login.jsp"); // 未登录，跳转
        }
    }
}