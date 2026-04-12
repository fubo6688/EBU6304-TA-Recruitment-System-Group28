<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Success - TA Recruitment</title>
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
            padding: 50px 40px;
            border-radius: 12px;
            box-shadow: 0 8px 24px rgba(0,0,0,0.1);
            width: 100%;
            max-width: 500px;
            margin-top: 80px;
            text-align: center;
        }
        .success-icon {
            width: 64px;
            height: 64px;
            background: #e6f4ea;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 20px;
            font-size: 32px;
        }
        h2 { color: #34a853; margin-bottom: 15px; }
        p { color: #5f6368; font-size: 15px; line-height: 1.6; }
        .btn-home {
            display: inline-block;
            margin-top: 25px;
            padding: 12px 30px;
            background-color: #1a73e8;
            color: white;
            text-decoration: none;
            border-radius: 6px;
            font-size: 15px;
            font-weight: 600;
            transition: background 0.2s;
        }
        .btn-home:hover { background-color: #1557b0; }
    </style>
</head>
<body>

    <div class="nav-bar">
        <strong>BUPT TA Recruitment System</strong>
        <a href="index.jsp">&larr; Back to Main Menu</a>
    </div>

    <div class="card">
        <div class="success-icon">&#10003;</div>
        <h2>Success!</h2>
        <p>${message}</p>
        <a href="index.jsp" class="btn-home">Back to Main Menu</a>
    </div>

</body>
</html>