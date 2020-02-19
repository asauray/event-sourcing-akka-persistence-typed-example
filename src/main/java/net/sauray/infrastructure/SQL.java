package net.sauray.infrastructure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQL {

    public static Connection newConnection(String user, String password, String database, String host, int port) throws SQLException {
        String url = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        // Connection is the only JDBC resource that we need
        // PreparedStatement and ResultSet are handled by jOOQ, internally
        return DriverManager.getConnection(url, user, password);
    }
}
