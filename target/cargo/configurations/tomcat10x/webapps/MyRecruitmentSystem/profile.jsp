<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>My Profile - TA Recruitment</title>
    <style>
        /* Unified background and font */
        body { 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
            background-color: #f0f2f5; 
            margin: 0; 
            display: flex; 
            flex-direction: column;
            align-items: center;
        }

        /* Navigation bar style */
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

        /* Card container */
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
        textarea { 
            width: 100%; 
            padding: 12px; 
            border: 1px solid #dadce0; 
            border-radius: 6px; 
            box-sizing: border-box; 
            font-size: 15px; 
        }

        /* File upload button styling */
        input[type="file"] {
            display: block;
            margin-top: 5px;
            font-size: 14px;
            color: #5f6368;
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

        .hint { font-size: 12px; color: #70757a; margin-top: 5px; }
    </style>
</head>
<body>

    <div class="nav-bar">
        <strong>BUPT TA Recruitment System</strong>
        <a href="index.jsp">&larr; Back to Main Menu</a>
    </div>

    <div class="card">
        <h2>Create Your Profile</h2>
        <form action="createProfile" method="POST" enctype="multipart/form-data">
            
            <div class="form-group">
                <label>Full Name</label>
                <input type="text" name="name" value="${name}" readonly style="background:#f8f9fa;">
                <p class="hint">Name is retrieved from your account system.</p>
            </div>

            <div class="form-group">
                <label>Key Skills</label>
                <input type="text" name="skills" placeholder="e.g. Java, Python, Agile, Tutoring" required>
            </div>

            <div class="form-group">
                <label>Experience Summary</label>
                <textarea name="experience" rows="4" placeholder="Briefly describe your relevant experience..."></textarea>
            </div>

            <div class="form-group">
                <label>Upload CV (Resume)</label>
                <input type="file" name="cvFile" accept=".pdf,.doc,.docx" required>
                <p class="hint">Supported formats: PDF, DOC, DOCX (Max 10MB)</p>
            </div>

            <button type="submit">Save Profile & CV</button>
        </form>
    </div>

</body>
</html>