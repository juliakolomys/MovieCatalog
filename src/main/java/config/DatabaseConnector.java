package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector implements AutoCloseable {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private Connection connection;

    public DatabaseConnector(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            System.out.println("Connecting to database...");
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            System.out.println("Database connection established.");
        }
        return connection;
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("Database connection closed");
                }
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }
}