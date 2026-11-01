package booknest;

/**
 * The staff user who operates the console system. Extends Person.
 */
public class Librarian extends Person {

    private String username;

    public Librarian(String librarianId, String name, String username) {
        super(librarianId, name);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void displayDetails() {
        System.out.printf("%-10s %-8s %-24s %-26s %-12s %s%n",
                "Librarian", getId(), getName(), "user: " + username, "-", "Staff");
    }
}
