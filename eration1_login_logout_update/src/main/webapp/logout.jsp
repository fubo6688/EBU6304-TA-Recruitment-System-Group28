<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // 1. 获取当前会话（如果存在）
    // 修改：删除了 "HttpSession" 关键字，直接使用内置对象或重新赋值
    session = request.getSession(false);
    
    // 2. 记录登出事件用于审计
    if (session != null) {
        String username = (String) session.getAttribute("username");
        System.out.println("[AUDIT LOG] User '" + username + "' logged out at " + new java.util.Date());
        
        // 3. 清除登录状态（销毁会话）
        session.invalidate();
    } else {
        System.out.println("[AUDIT LOG] Anonymous user attempted to logout at " + new java.util.Date());
    }
    
    // 4. 重定向到登录页面
    response.sendRedirect(request.getContextPath() + "/login.jsp");
%>