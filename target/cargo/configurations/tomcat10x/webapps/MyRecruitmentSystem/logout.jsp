<%
    session.invalidate(); // Clear session
    response.sendRedirect("login.jsp");
%>