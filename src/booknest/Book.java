package booknest;

/**
 * Stores the details of one book title (and how many copies exist).
 */
public class Book {

    private String bookId;
    private String title;
    private String author;
    private int totalCopies;
    private int availableCopies;
    private String status;

    // Constructor used when reading a full row from the database.
    public Book(String bookId, String title, String author,
                int totalCopies, int availableCopies, String status) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.status = status;
    }

    // Overloaded constructor for a new book: all copies are available and the book is Active.
    public Book(String bookId, String title, String author, int totalCopies) {
        this(bookId, title, author, totalCopies, totalCopies, Status.ACTIVE);
    }

    public String getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static void printHeader() {
        System.out.printf("%-8s %-32s %-24s %6s %6s  %s%n",
                "BOOK ID", "TITLE", "AUTHOR", "TOTAL", "AVAIL", "STATUS");
        System.out.println("-----------------------------------------------------------------------------------------");
    }

    public void displayDetails() {
        System.out.printf("%-8s %-32s %-24s %6d %6d  %s%n",
                bookId, title, author, totalCopies, availableCopies, status);
    }
}
