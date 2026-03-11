<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.bupt.model.Job" %>
<!DOCTYPE html>
<html>
<head>
    <title>Available Jobs - TA Recruitment</title>
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

        .btn-apply {
            display: inline-block;
            padding: 8px 18px;
            background: #1a73e8;
            color: white;
            text-decoration: none;
            border-radius: 6px;
            font-size: 13px;
            font-weight: 600;
            transition: background 0.2s;
        }
        .btn-apply:hover { background: #1557b0; }

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
        <h2>Available TA/Invigilator Positions</h2>
        <table>
            <tr>
                <th>Job ID</th>
                <th>Module Name</th>
                <th>Role</th>
                <th>Required Skills</th>
                <th>Action</th>
            </tr>
            <%
                List<Job> jobs = (List<Job>) request.getAttribute("jobList");
                if(jobs != null && !jobs.isEmpty()) {
                    for(Job j : jobs) {
            %>
            <tr>
                <td><%= j.getJobId() %></td>
                <td><%= j.getModuleName() %></td>
                <td><%= j.getRole() %></td>
                <td><%= j.getRequiredSkills() %></td>
                <td>
                    <a href="applyJob.jsp?jobId=<%= j.getJobId() %>" class="btn-apply">Apply</a>
                </td>
            </tr>
            <%
                    }
                } else {
            %>
            <tr><td colspan="5" class="empty-msg">No jobs available right now.</td></tr>
            <% } %>
        </table>
    </div>

</body>
</html>