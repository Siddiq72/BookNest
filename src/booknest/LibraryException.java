package booknest;

/**
 * Custom exception for business-rule problems
 * (invalid input, unavailable book, borrowing limit reached, and so on).
 */
public class LibraryException extends Exception {

    private static final long serialVersionUID = 1L;

    public LibraryException(String message) {
        super(message);
    }
}
