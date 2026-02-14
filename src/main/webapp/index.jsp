
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>Hello World</title>
    <style>
        body {
            margin: 0;
            height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            background: linear-gradient(135deg, #667eea, #764ba2);
            font-family: Arial, sans-serif;
        }

        .card {
            background: rgba(255,255,255,0.15);
            padding: 40px;
            border-radius: 15px;
            text-align: center;
            backdrop-filter: blur(10px);
            color: white;
            box-shadow: 0 15px 40px rgba(0,0,0,0.3);
        }

        h1 {
            margin: 0;
            font-size: 40px;
        }
    </style>
</head>
<body>
    <div class="card">
        <h1>Hello World!</h1>
    </div>
</body>
</html>
