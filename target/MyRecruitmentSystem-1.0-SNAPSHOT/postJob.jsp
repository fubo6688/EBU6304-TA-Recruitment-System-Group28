<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Post a Job - MO Dashboard</title>
</head>
<body style="font-family: Arial; margin: 40px;">
    <h2>Create a New Job Listing</h2>
    <hr>
    
    <form action="postJob" method="POST">
        <p>
            <label>Module Name:</label><br>
            <input type="text" name="moduleName" required placeholder="e.g. Software Engineering">
        </p>
        <p>
            <label>Role:</label><br>
            <select name="role">
                <option value="Teaching Assistant">Teaching Assistant</option>
                <option value="Invigilator">Invigilator</option>
            </select>
        </p>
        <p>
            <label>Required Skills (Comma separated):</label><br>
            <input type="text" name="requiredSkills" required placeholder="e.g. Java, Git, Agile" size="40">
        </p>
        <button type="submit" style="padding: 8px 16px;">Post Job</button>
    </form>
</body>
</html>