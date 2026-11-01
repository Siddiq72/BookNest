package booknest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Creates JDBC connections to MySQL.
 *
 * Connection details are NOT written in the source code. They come from
 * environment variables so passwords are never uploaded to GitHub:
 *   BOOKNEST_DB_URL       (default: jdbc:mysql://localhost:3306/booknest_db)
 *   BOOKNEST_DB_USER      (default: root)
 *   BOOKNEST_DB_PASSWORD  (if not set, LibraryApp asks for it when it starts)
 */
public class DatabaseConnection {

    private static final String url = readSetting("BOOKNEST_DB_URL",
            "jdbc:mysql://localhost:3306/booknest_db?useSSL=false&allowPublicKeyRetrieval=true");
    private static final String user = readSetting("BOOKNEST_DB_USER", "root");
    private static String password = readSetting("BOOKNEST_DB_PASSWORD", null);

    private DatabaseConnection() {
        // Only static methods are used.
    }

    private static String readSetting(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null) ? defaultValue : value;
    }

    public static boolean hasPassword() {
        return password != null;
    }

    public static void setPassword(String newPassword) {
        password = newPassword;
    }

    // Every call returns a new connection. Callers close it with try-with-resources.
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password == null ? "" : password);
    }
}
