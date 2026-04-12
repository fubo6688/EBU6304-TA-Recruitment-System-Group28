<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Dashboard - TA Recruitment</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f0f2f5;
            margin: 0;
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
        .nav-bar .nav-right { display: flex; align-items: center; gap: 15px; font-size: 14px; }
        .nav-bar a {
            color: white;
            text-decoration: none;
            font-size: 14px;
            border: 1px solid white;
            padding: 5px 15px;
            border-radius: 4px;
            transition: background 0.2s;
        }
        .nav-bar a:hover { background: rgba(255,255,255,0.15); }

        .main-content {
            padding: 50px;
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .main-content h1 { color: #202124; margin-bottom: 30px; }

        .menu-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 25px;
            width: 100%;
            max-width: 800px;
        }
        .card {
            background: white;
            padding: 30px;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.08);
            text-decoration: none;
            color: #333;
            transition: all 0.25s ease;
            border-left: 5px solid #1a73e8;
        }
        .card:hover {
            transform: translateY(-5px);
            box-shadow: 0 8px 24px rgba(0,0,0,0.15);
        }
        .card h3 { color: #1a73e8; margin-top: 0; margin-bottom: 10px; }
        .card p { color: #5f6368; margin: 0; font-size: 14px; line-height: 1.5; }
    </style>
</head>
<body>
    <div class="nav-bar">
        <strong>BUPT TA Recruitment System</strong>
        <div class="nav-right">
            <span>Welcome, ${name} (${role})</span>
            <a href="logout.jsp">Logout</a>
        </div>
    </div>

    <div class="main-content">
        <h1>Dashboard</h1>
        <div class="menu-grid">
            <% String role = (String) session.getAttribute("role"); %>

            <% if ("MO".equals(role)) { %>
                <a href="postJob.jsp" class="card">
                    <h3>Post New Recruitment</h3>
                    <p>Create a new TA or Invigilator position for your module.</p>
                </a>
                <a href="moDashboard.jsp" class="card">
                    <h3>Review Applications</h3>
                    <p>Select the best candidates for your posted jobs.</p>
                </a>
            <% } %>

            <% if ("TA".equals(role)) { %>
                <a href="jobs" class="card">
                    <h3>Find Available Jobs</h3>
                    <p>Browse and apply for current teaching assistant openings.</p>
                </a>
                <a href="profile.jsp" class="card">
                    <h3>My Applicant Profile</h3>
                    <p>Update your skills and manage your uploaded CV.</p>
                </a>
            <% } %>

            <% if ("ADMIN".equals(role)) { %>
                <a href="adminDashboard" class="card" style="grid-column: span 2; border-left-color: #ea4335;">
                    <h3>Administrator Control Panel</h3>
                    <p>Monitor TA workload and identify missing skills across the system.</p>
                </a>
            <% } %>
        </div>
    </div>
</body>
</html>