<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Login - BUPT International School</title>
    <style>
        /* --- 1. 全局样式与背景 --- */
        body {
            margin: 0;
            padding: 0;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            color: #3c4043;

            /* 背景图片设置 - 关键修改：路径改为绝对路径 */
            background-image: url('/images/bupt-bg.jpg');
            background-size: cover;
            background-position: center;
            background-repeat: no-repeat;
            background-color: #f0f2f5;

            height: 100vh; /* 设置高度，确保背景图撑满屏幕 */
            overflow: auto;
        }

        /* --- 2. 登录框卡片样式 --- */
        .card {
            /* 背景改为纯白或半透，确保文字可读性 */
            background: rgba(255, 255, 255, 0.95);
            padding: 40px;
            border-radius: 12px;
            box-shadow: 0 8px 24px rgba(0,0,0,0.2);
            width: 100%;
            max-width: 400px;
            /* 位置：使用 margin-top 让它悬浮在上方，不挡住背景图中间的内容 */
            margin: 80px auto 0; /* 上 左右 下 */
            position: relative;
            z-index: 1;
        }

        h2 {
            color: #1a73e8;
            text-align: center;
            margin-bottom: 30px;
            font-size: 24px;
        }

        /* --- 3. 表单元素样式 --- */
        .form-group {
            margin-bottom: 20px;
        }

        label {
            display: block;
            margin-bottom: 8px;
            font-weight: 600;
            color: #5f6368;
        }

        input[type="text"],
        input[type="password"] {
            width: 100%;
            padding: 12px;
            border: 1px solid #dadce0;
            border-radius: 6px;
            box-sizing: border-box;
            font-size: 15px;
            transition: border-color 0.2s;
        }

        input[type="text"]:focus,
        input[type="password"]:focus {
            outline: none;
            border-color: #1a73e8;
            box-shadow: 0 0 0 2px rgba(26,115,232,0.2);
        }

        /* --- 4. 错误提示样式 --- */
        .error-msg {
            background: #fce8e6;
            color: #c5221f;
            padding: 12px;
            border-radius: 6px;
            font-size: 14px;
            margin-bottom: 20px;
            text-align: center;
        }

        /* --- 5. 按钮样式 --- */
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
            margin-top: 10px;
        }

        button:hover {
            background-color: #1557b0;
        }
    </style>
</head>
<body>

    <!-- 登录卡片 -->
    <div class="card">
        <h2>System Login</h2>

        <%-- 错误提示逻辑 --%>
        <%
            String errorType = request.getParameter("error");
            String errorMsg = "";

            if ("locked".equals(errorType)) {
                errorMsg = "Account is locked. Please try again in 5 minutes.";
            } else if ("empty".equals(errorType)) {
                errorMsg = "Username and password cannot be empty.";
            } else if ("invalid".equals(errorType)) {
                String attemptsStr = request.getParameter("attempts");
                if (attemptsStr != null) {
                    errorMsg = "Invalid credentials. You have " + attemptsStr + " attempt(s) left.";
                } else {
                    errorMsg = "Invalid username or password.";
                }
            } else if ("system".equals(errorType)) {
                errorMsg = "System error. Please try again later.";
            }
        %>

        <% if (!errorMsg.isEmpty()) { %>
            <div class="error-msg"><%= errorMsg %></div>
        <% } %>

        <form action="login" method="POST">
            <div class="form-group">
                <label>Username</label>
                <input type="text" name="username" placeholder="Enter your username" required>
            </div>
            <div class="form-group">
                <label>Password</label>
                <input type="password" name="password" placeholder="Enter your password" required>
            </div>
            <button type="submit">Login</button>
        </form>
    </div>

</body>
</html>