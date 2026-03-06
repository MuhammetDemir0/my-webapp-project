
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>To Do List</title>
    <link rel="stylesheet" href="styles.css" />
    <script src="tasks.js" defer></script>
</head>
<body data-context-path="<%= request.getContextPath() %>">
    <div class="backdrop-glow"></div>
    <div class="app-card">
        <h1>To Do List</h1>
        <form id="task-form" class="task-form" autocomplete="off">
            <input id="task-input" name="title" type="text" placeholder="Add a new task..." maxlength="120" required />
            <button type="submit">Add</button>
        </form>

        <ul id="task-list" class="task-list" aria-live="polite"></ul>
    </div>
</body>
</html>
