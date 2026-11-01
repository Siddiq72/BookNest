package booknest;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the issue_record table lives here.
 * Methods that take a Connection are used inside issue/return transactions.
 */
public class IssueDAO {

    private IssueRecord mapRow(ResultSet rs) throws SQLException {
        Date returnSqlDate = rs.getDate("return_date");
        LocalDate returnDate = (returnSqlDate == null) ? null : returnSqlDate.toLocalDate();
        return new IssueRecord(rs.getLong("issue_id"), rs.getString("book_id"), rs.getString("member_id"),
                rs.getDate("issue_date").toLocalDate(), rs.getDate("due_date").toLocalDate(),
                returnDate, rs.getDouble("fine_amount"), rs.getString("status"));
    }

    // ---------- CREATE ----------
    // Inserts a new issue record and returns the auto-generated issue_id.
    public long insertIssue(Connection con, IssueRecord record) throws SQLException {
        String sql = "INSERT INTO issue_record (book_id, member_id, issue_date, due_date, fine_amount, status) "
                + "VALUES (?, ?, ?, ?, 0, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.getBookId());
            ps.setString(2, record.getMemberId());
            ps.setDate(3, Date.valueOf(record.getIssueDate()));
            ps.setDate(4, Date.valueOf(record.getDueDate()));
            ps.setString(5, Status.ISSUED);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        return 0;
    }

    // ---------- READ ----------
    // Latest issue record of this member for this book (locked for the return transaction).
    public IssueRecord findLatestIssueForUpdate(Connection con, String memberId, String bookId) throws SQLException {
        String sql = "SELECT * FROM issue_record WHERE member_id = ? AND book_id = ? "
                + "ORDER BY issue_id DESC LIMIT 1 FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, memberId);
            ps.setString(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    // All books that are currently issued (not yet returned).
    public List<IssueRecord> getActiveIssues() throws SQLException {
        return runListQuery("SELECT * FROM issue_record WHERE status = 'Issued' ORDER BY issue_id", null);
    }

    // Full borrowing history of one member, newest first.
    public List<IssueRecord> getHistoryByMember(String memberId) throws SQLException {
        return runListQuery("SELECT * FROM issue_record WHERE member_id = ? ORDER BY issue_id DESC", memberId);
    }

    private List<IssueRecord> runListQuery(String sql, String parameter) throws SQLException {
        List<IssueRecord> records = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (parameter != null) {
                ps.setString(1, parameter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRow(rs));
                }
            }
        }
        return records;
    }

    // Total number of issues ever made (every row in issue_record).
    public int countAllIssues() throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return count(con, "SELECT COUNT(*) FROM issue_record", null, null);
        }
    }

    // Total of the fines stored for one member.
    public double sumFineByMember(String memberId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(fine_amount), 0) FROM issue_record WHERE member_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0;
    }

    // ----- counting helpers (used for the 3-book limit and the delete rules) -----
    public int countActiveByMember(Connection con, String memberId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM issue_record WHERE member_id = ? AND status = 'Issued'",
                memberId, null);
    }

    public int countActiveByMemberAndBook(Connection con, String memberId, String bookId) throws SQLException {
        return count(con,
                "SELECT COUNT(*) FROM issue_record WHERE member_id = ? AND book_id = ? AND status = 'Issued'",
                memberId, bookId);
    }

    public int countActiveByMember(String memberId) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return countActiveByMember(con, memberId);
        }
    }

    public int countAllByMember(String memberId) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return count(con, "SELECT COUNT(*) FROM issue_record WHERE member_id = ?", memberId, null);
        }
    }

    public int countActiveByBook(String bookId) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return count(con, "SELECT COUNT(*) FROM issue_record WHERE book_id = ? AND status = 'Issued'",
                    bookId, null);
        }
    }

    public int countAllByBook(String bookId) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return count(con, "SELECT COUNT(*) FROM issue_record WHERE book_id = ?", bookId, null);
        }
    }

    // Runs a "SELECT COUNT(*)" query with zero, one or two text parameters.
    private int count(Connection con, String sql, String first, String second) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (first != null) {
                ps.setString(1, first);
            }
            if (second != null) {
                ps.setString(2, second);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    // ---------- UPDATE ----------
    // Marks an issue record as Returned and stores the return date and fine.
    // Returns false if the record was not in Issued state (e.g. already returned).
    public boolean markReturned(Connection con, long issueId, LocalDate returnDate, double fine)
            throws SQLException {
        String sql = "UPDATE issue_record SET return_date = ?, fine_amount = ?, status = 'Returned' "
                + "WHERE issue_id = ? AND status = 'Issued'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(returnDate));
            ps.setDouble(2, fine);
            ps.setLong(3, issueId);
            return ps.executeUpdate() == 1;
        }
    }
}
