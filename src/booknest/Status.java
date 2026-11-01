package booknest;

/**
 * Keeps the status words in one place so Java code and SQL always agree.
 */
public final class Status {

    public static final String ACTIVE = "Active";
    public static final String INACTIVE = "Inactive";
    public static final String ISSUED = "Issued";
    public static final String RETURNED = "Returned";

    private Status() {
        // Nobody should create a Status object.
    }
}
