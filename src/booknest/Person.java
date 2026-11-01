package booknest;

/**
 * Abstract parent class for everyone who uses the library.
 * Member and Librarian extend this class.
 */
public abstract class Person {

    private String id;
    private String name;

    public Person(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Every child class must show its own details (method overriding).
    public abstract void displayDetails();

    // Header used when a list of Person objects is printed.
    public static void printHeader() {
        System.out.printf("%-10s %-8s %-24s %-26s %-12s %s%n",
                "ROLE", "ID", "NAME", "EMAIL / USERNAME", "PHONE", "STATUS");
        System.out.println("-----------------------------------------------------------------------------------------------");
    }
}
