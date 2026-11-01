package booknest;

/**
 * A library member (student). Extends the abstract Person class.
 */
public class Member extends Person {

    private String email;
    private String phone;
    private String status;

    // Constructor used when reading a member from the database.
    public Member(String memberId, String name, String email, String phone, String status) {
        super(memberId, name);
        this.email = email;
        this.phone = phone;
        this.status = status;
    }

    // Overloaded constructor used for a brand-new member (always Active).
    public Member(String memberId, String name, String email, String phone) {
        this(memberId, name, email, phone, Status.ACTIVE);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public void displayDetails() {
        System.out.printf("%-10s %-8s %-24s %-26s %-12s %s%n",
                "Member", getId(), getName(),
                email == null ? "-" : email,
                phone == null ? "-" : phone,
                status);
    }
}
