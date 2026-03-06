package com.example.todo.web;

import com.example.todo.dao.TaskDao;
import com.example.todo.model.Task;
import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class TasksServlet extends HttpServlet {
    private transient TaskDao taskDao;
    private final Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        try {
            taskDao = new TaskDao();
        } catch (SQLException exception) {
            throw new ServletException("Failed to initialize TaskDao", exception);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            Integer id = parseIdFromPath(request.getPathInfo());
            if (id == null) {
                List<Task> tasks = taskDao.getAllTasks();
                response.getWriter().write(gson.toJson(tasks));
                return;
            }

            Task task = taskDao.getTaskById(id);
            if (task == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                writeError(response, "Task not found");
                return;
            }

            response.getWriter().write(gson.toJson(task));
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(response, exception.getMessage());
        } catch (SQLException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(response, "Database error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            TaskPayload payload = gson.fromJson(request.getReader(), TaskPayload.class);
            if (payload == null || payload.title == null || payload.title.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeError(response, "Title is required");
                return;
            }

            boolean completed = payload.completed != null && payload.completed;
            Task createdTask = taskDao.createTask(payload.title.trim(), completed);
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(gson.toJson(createdTask));
        } catch (SQLException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(response, "Database error");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            Integer id = parseIdFromPath(request.getPathInfo());
            if (id == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeError(response, "Task id is required in path");
                return;
            }

            Task existingTask = taskDao.getTaskById(id);
            if (existingTask == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                writeError(response, "Task not found");
                return;
            }

            TaskPayload payload = gson.fromJson(request.getReader(), TaskPayload.class);
            if (payload == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeError(response, "Invalid request body");
                return;
            }

            String nextTitle = payload.title == null ? existingTask.getTitle() : payload.title.trim();
            if (nextTitle.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeError(response, "Title cannot be empty");
                return;
            }

            boolean nextCompleted = payload.completed == null ? existingTask.isCompleted() : payload.completed;
            Task updatedTask = new Task(id, nextTitle, nextCompleted);
            boolean updated = taskDao.updateTask(updatedTask);
            if (!updated) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                writeError(response, "Task not found");
                return;
            }

            response.getWriter().write(gson.toJson(updatedTask));
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(response, exception.getMessage());
        } catch (SQLException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(response, "Database error");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            Integer id = parseIdFromPath(request.getPathInfo());
            if (id == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeError(response, "Task id is required in path");
                return;
            }

            boolean deleted = taskDao.deleteTask(id);
            if (!deleted) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                writeError(response, "Task not found");
                return;
            }

            response.getWriter().write("{\"message\":\"Task deleted\"}");
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(response, exception.getMessage());
        } catch (SQLException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(response, "Database error");
        }
    }

    private Integer parseIdFromPath(String pathInfo) {
        if (pathInfo == null || "/".equals(pathInfo)) {
            return null;
        }

        String normalized = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        if (normalized.contains("/")) {
            throw new IllegalArgumentException("Invalid path format");
        }

        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Task id must be an integer");
        }
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        response.getWriter().write("{\"error\":\"" + message.replace("\"", "\\\"") + "\"}");
    }

    private static final class TaskPayload {
        private String title;
        private Boolean completed;
    }
}
