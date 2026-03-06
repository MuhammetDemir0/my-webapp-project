package com.example.todo.dao;

import com.example.todo.model.Task;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TaskDao {
    private static final String DB_URL = "jdbc:sqlite:todo.db";

    public TaskDao() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("SQLite JDBC driver not found", exception);
        }
        initializeTable();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initializeTable() throws SQLException {
        String createTableSql = "CREATE TABLE IF NOT EXISTS tasks ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "title TEXT NOT NULL, "
            + "completed INTEGER NOT NULL DEFAULT 0"
            + ")";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createTableSql);
        }
    }

    public List<Task> getAllTasks() throws SQLException {
        String query = "SELECT id, title, completed FROM tasks ORDER BY id ASC";
        List<Task> tasks = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                tasks.add(mapTask(resultSet));
            }
        }

        return tasks;
    }

    public Task getTaskById(int id) throws SQLException {
        String query = "SELECT id, title, completed FROM tasks WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapTask(resultSet);
                }
            }
        }

        return null;
    }

    public Task createTask(String title, boolean completed) throws SQLException {
        String query = "INSERT INTO tasks(title, completed) VALUES(?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title);
            statement.setInt(2, completed ? 1 : 0);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    return new Task(generatedId, title, completed);
                }
            }
        }

        return null;
    }

    public boolean updateTask(Task task) throws SQLException {
        String query = "UPDATE tasks SET title = ?, completed = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, task.getTitle());
            statement.setInt(2, task.isCompleted() ? 1 : 0);
            statement.setInt(3, task.getId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteTask(int id) throws SQLException {
        String query = "DELETE FROM tasks WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private Task mapTask(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String title = resultSet.getString("title");
        boolean completed = resultSet.getInt("completed") == 1;
        return new Task(id, title, completed);
    }
}