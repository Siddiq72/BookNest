package booknest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Reads the librarian (staff user) from the librarian table.
 */
public class LibrarianDAO {

    // READ: first librarian in the table (null if the table is empty).
    public Librarian getFirstLibrarian() throws SQLException {
        String sql = "SELECT librarian_id, librarian_name, username FROM librarian "
                + "ORDER BY librarian_id LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new Librarian(rs.getString("librarian_id"),
                        rs.getString("librarian_name"), rs.getString("username"));
            }
        }
        return null;
    }
}
