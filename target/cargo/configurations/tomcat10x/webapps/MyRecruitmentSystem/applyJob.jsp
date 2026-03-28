<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Apply for Job - TA Recruitment</title>
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
            max-width: 500px;
            margin-top: 50px;
        }
        h2 { color: #1a73e8; text-align: center; margin-bottom: 30px; }
        .form-group { margin-bottom: 20px; }
        label { display: block; margin-bottom: 8px; color: #3c4043; font-weight: 600; }
        input[type="text"],
        input[type="email"] {
            width: 100%;
            padding: 12px;
            border: 1px solid #dadce0;
            border-radius: 6px;
            box-sizing: border-box;
            font-size: 15px;
        }
        input[type="text"]:focus,
        input[type="email"]:focus {
            outline: none;
            border-color: #1a73e8;
            box-shadow: 0 0 0 2px rgba(26,115,232,0.2);
        }
        button {
            width: 100%;
            padding: 14px;
            background-color: #1a73e8;
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 16px;
            font-weight: bold;
            cursor: pointer;
            transition: background 0.2s;
            margin-top: 20px;
        }
        button:hover { background-color: #1557b0; }
    </style>
</head>
<body>

    <div class="nav-bar">
        <strong>BUPT TA Recruitment System</strong>
        <a href="jobs">&larr; Back to Job List</a>
    </div>

    <div class="card">
        <h2>Job Application</h2>
        <form action="apply" method="POST">
            <input type="hidden" name="jobId" value="<%= request.getParameter("jobId") %>">

            <div class="form-group">
                <label>Your Name</label>
                <input type="text" name="applicantName" required placeholder="e.g. John Doe">
            </div>

            <div class="form-group">
                <label>Your Email</label>
                <input type="email" name="applicantEmail" required placeholder="e.g. john@bupt.edu">
            </div>

            <button type="submit">Submit Application</button>
        </form>
    </div>

</body>
</html>