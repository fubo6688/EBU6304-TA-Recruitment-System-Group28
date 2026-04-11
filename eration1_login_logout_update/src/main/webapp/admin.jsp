<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Admin Dashboard - TA Recruitment</title>
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
            max-width: 700px;
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
        .status-normal { color: #34a853; font-weight: 600; }
        .status-overloaded { color: #ea4335; font-weight: 600; }
    </style>
</head>
<body>

    <div class="nav-bar">
        <strong>BUPT TA Recruitment System</strong>
        <a href="index.jsp">&larr; Back to Main Menu</a>
    </div>

    <div class="card">
        <h2>TA Workload Dashboard</h2>
        <table>
            <tr>
                <th>TA ID</th>
                <th>Assigned Jobs Count</th>
                <th>Status</th>
            </tr>
            <%
                Map<String, Integer> workload = (Map<String, Integer>) request.getAttribute("workload");
                if (workload != null) {
                    for (Map.Entry<String, Integer> entry : workload.entrySet()) {
                        int count = entry.getValue();
                        boolean overloaded = count > 2;
            %>
            <tr>
                <td><%= entry.getKey() %></td>
                <td><%= count %></td>
                <td>
                    <span class="<%= overloaded ? "status-overloaded" : "status-normal" %>">
                        <%= overloaded ? "Overloaded!" : "Normal" %>
                    </span>
                </td>
            </tr>
            <%      }
                }
            %>
        </table>
    </div>

</body>
</html>