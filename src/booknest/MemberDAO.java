package booknest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the member table lives here (Create, Read, Update, Delete).
 */
public class MemberDAO {

    private Member mapRow(ResultSet rs) throws SQLException {
        return new Member(rs.getString("member_id"), rs.getString("member_name"),
                rs.getString("email"), rs.getString("phone"), rs.getString("status"));
    }

    // ---------- CREATE ----------
    public void addMember(Member member) throws SQLException {
        String sql = "INSERT INTO member (member_id, member_name, email, phone, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, member.getId());
            ps.setString(2, member.getName());
            ps.setString(3, member.getEmail());   // null is stored as SQL NULL
            ps.setString(4, member.getPhone());
            ps.setString(5, member.getStatus());
            ps.executeUpdate();
        }
    }

    // ---------- READ ----------
    public Member getMemberById(String memberId) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return getMemberById(con, memberId, false);
        }
    }

    // Overloaded version used inside a transaction (lockRow = true adds FOR UPDATE).
    public Member getMemberById(Connection con, String memberId, boolean lockRow) throws SQLException {
        String sql = "SELECT * FROM member WHERE member_id = ?";
        if (lockRow) {
            sql = sql + " FOR UPDATE";
        }
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Member> getAllMembers() throws SQLException {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT * FROM member ORDER BY member_id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                members.add(mapRow(rs));
            }
        }
        return members;
    }

    // Returns the ID of the member who owns this email, or null if nobody does.
    public String getMemberIdByEmail(String email) throws SQLException {
        String sql = "SELECT member_id FROM member WHERE email = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("member_id");
                }
            }
        }
        return null;
    }

    // ---------- UPDATE ----------
    public void updateMember(Member member) throws SQLException {
        String sql = "UPDATE member SET member_name = ?, email = ?, phone = ? WHERE member_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, member.getName());
            ps.setString(2, member.getEmail());
            ps.setString(3, member.getPhone());
            ps.setString(4, member.getId());
            ps.executeUpdate();
        }
    }

    // Used for deactivating (soft delete) and re-activating a member.
    public void updateStatus(String memberId, String status) throws SQLException {
        String sql = "UPDATE member SET status = ? WHERE member_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, memberId);
            ps.executeUpdate();
        }
    }

    // ---------- DELETE ----------
    public void deleteMember(String memberId) throws SQLException {
        String sql = "DELETE FROM member WHERE member_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, memberId);
            ps.executeUpdate();
        }
    }
}
