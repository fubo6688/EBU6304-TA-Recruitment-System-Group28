<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.bupt.model.Application" %>
<%@ page import="com.bupt.utils.FileStorageUtil" %>
<!DOCTYPE html>
<html>
<head>
    <title>Review Applications - TA Recruitment</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f0f2f5;
            margin: 0;
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .nav-bar {
            background: #1a73e8;
            color: white;
            padding: 15px 50px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            width: 100%;
            box-sizing: border-box;
        }
        .nav-bar strong { font-size: 18px; }
        .nav-bar a {
            color: white;
            text-decoration: none;
            font-size: 14px;
            border: 1px solid white;
            padding: 5px 10px;
            border-radius: 4px;
            transition: background 0.2s;
        }
        .nav-bar a:hover { background: rgba(255,255,255,0.15); }

        .card {
            background: white;
            padding: 40px;
            border-radius: 12px;
            box-shadow: 0 8px 24px rgba(0,0,0,0.1);
            width: 100%;
            max-width: 850px;
            margin-top: 50px;
        }
        h2 { color: #1a73e8; text-align: center; margin-bottom: 30px; }

        table { width: 100%; border-collapse: collapse; margin-top: 10px; }
        th {
            background-color: #1a73e8;
            color: white;
            padding: 14px 12px;
            text-align: left;
            font-weight: 600;
            font-size: 14px;
        }
        td {
            padding: 14px 12px;
            border-bottom: 1px solid #e8eaed;
            color: #3c4043;
            font-size: 14px;
        }
        tr:hover { background-color: #f8f9fa; }

        .status-pending { color: #fbbc04; font-weight: 600; }
        .status-accepted { color: #34a853; font-weight: 600; }
        .status-rejected { color: #ea4335; font-weight: 600; }

        .btn {
            padding: 7px 16px;
            border: none;
            border-radius: 6px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            transition: opacity 0.2s;
            color: white;
        }
        .btn:hover { opacity: 0.85; }
        .btn-accept { background: #34a853; }
        .btn-reject { background: #ea4335; margin-left: 6px; }

        .empty-msg {
            text-align: center;
            color: #5f6368;
            padding: 30px;
            font-size: 15px;
        }
    </style>
</head>
<body>

    <div class="nav-bar">
        <strong>BUPT TA Recruitment System</strong>
        <a href="index.jsp">&larr; Back to Main Menu</a>
    </div>

    <div class="card">
        <h2>Review Applications</h2>
        <table>
            <tr>
                <th>Application ID</th>
                <th>Applicant</th>
                <th>Status</th>
                <th>Action</th>
            </tr>
            <%
                List<Application> apps = FileStorageUtil.getAllApplications();
                if (apps != null && !apps.isEmpty()) {
                    for (Application app : apps) {
                        String status = app.getStatus();
                        String statusClass = "status-pending";
                        if ("ACCEPTED".equals(status)) statusClass = "status-accepted";
                        else if ("REJECTED".equals(status)) statusClass = "status-rejected";
            %>
            <tr>
                <td><%= app.getApplicationId() %></td>
                <td><%= app.getApplicantName() %></td>
                <td><span class="<%= statusClass %>"><%= status %></span></td>
                <td>
                    <% if ("PENDING".equals(status)) { %>
                    <form action="processApplication" method="POST" style="display:inline;">
                        <input type="hidden" name="appId" value="<%= app.getApplicationId() %>">
                        <button type="submit" name="decision" value="ACCEPTED" class="btn btn-accept">Accept</button>
                        <button type="submit" name="decision" value="REJECTED" class="btn btn-reject">Reject</button>
                    </form>
                    <% } else { %>
                    <span style="color:#9aa0a6; font-size:13px;">Decided</span>
                    <% } %>
                </td>
            </tr>
            <%
                    }
                } else {
            %>
            <tr><td colspan="4" class="empty-msg">No applications to review.</td></tr>
            <% } %>
        </table>
    </div>

</body>
</html>