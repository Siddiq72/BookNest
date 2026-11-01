package booknest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the book table lives here (Create, Read, Update, Delete).
 */
public class BookDAO {

    // Converts the current row of a ResultSet into a Book object.
    private Book mapRow(ResultSet rs) throws SQLException {
        return new Book(rs.getString("book_id"), rs.getString("title"), rs.getString("author"),
                rs.getInt("total_copies"), rs.getInt("available_copies"), rs.getString("status"));
    }

    // ---------- CREATE ----------
    public void addBook(Book book) throws SQLException {
        String sql = "INSERT INTO book (book_id, title, author, total_copies, available_copies, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, book.getBookId());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setInt(4, book.getTotalCopies());
            ps.setInt(5, book.getAvailableCopies());
            ps.setString(6, book.getStatus());
            ps.executeUpdate();
        }
    }

    // ---------- READ ----------
    // Simple version: opens and closes its own connection.
    public Book getBookById(String bookId) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return getBookById(con, bookId, false);
        }
    }

    // Overloaded version used inside a transaction. lockRow = true adds FOR UPDATE
    // so nobody else can change this row until we commit.
    public Book getBookById(Connection con, String bookId, boolean lockRow) throws SQLException {
        String sql = "SELECT * FROM book WHERE book_id = ?";
        if (lockRow) {
            sql = sql + " FOR UPDATE";
        }
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Book> getAllBooks() throws SQLException {
        return runListQuery("SELECT * FROM book ORDER BY book_id", null);
    }

    public List<Book> searchByTitle(String keyword) throws SQLException {
        return runListQuery("SELECT * FROM book WHERE title LIKE ? ORDER BY book_id", "%" + keyword + "%");
    }

    public List<Book> searchByIdPart(String part) throws SQLException {
        return runListQuery("SELECT * FROM book WHERE book_id LIKE ? ORDER BY book_id", "%" + part + "%");
    }

    // Shared helper: runs a SELECT (with at most one parameter) and returns a list of books.
    private List<Book> runListQuery(String sql, String parameter) throws SQLException {
        List<Book> books = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (parameter != null) {
                ps.setString(1, parameter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    books.add(mapRow(rs));
                }
            }
        }
        return books;
    }

    // ---------- UPDATE ----------
    public void updateBook(Book book) throws SQLException {
        String sql = "UPDATE book SET title = ?, author = ?, total_copies = ?, available_copies = ? "
                + "WHERE book_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setInt(3, book.getTotalCopies());
            ps.setInt(4, book.getAvailableCopies());
            ps.setString(5, book.getBookId());
            ps.executeUpdate();
        }
    }

    // Used for deactivating (soft delete) and re-activating a book.
    public void updateStatus(String bookId, String status) throws SQLException {
        String sql = "UPDATE book SET status = ? WHERE book_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, bookId);
            ps.executeUpdate();
        }
    }

    // Part of the issue transaction: takes one copy. Returns false if no copy was left.
    public boolean decreaseAvailable(Connection con, String bookId) throws SQLException {
        String sql = "UPDATE book SET available_copies = available_copies - 1 "
                + "WHERE book_id = ? AND available_copies > 0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, bookId);
            return ps.executeUpdate() == 1;
        }
    }

    // Part of the return transaction: puts one copy back. Returns false if the update failed.
    public boolean increaseAvailable(Connection con, String bookId) throws SQLException {
        String sql = "UPDATE book SET available_copies = available_copies + 1 "
                + "WHERE book_id = ? AND available_copies < total_copies";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, bookId);
            return ps.executeUpdate() == 1;
        }
    }

    // ---------- DELETE ----------
    public void deleteBook(String bookId) throws SQLException {
        String sql = "DELETE FROM book WHERE book_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, bookId);
            ps.executeUpdate();
        }
    }
}
