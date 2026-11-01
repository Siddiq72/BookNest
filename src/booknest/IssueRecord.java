package booknest;

import java.time.LocalDate;

/**
 * One row of borrowing history: which member borrowed which book, and when.
 */
public class IssueRecord {

    private long issueId;
    private String bookId;
    private String memberId;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;   // null until the book is returned
    private double fineAmount;
    private String status;

    // Constructor used when reading a row from the database.
    public IssueRecord(long issueId, String bookId, String memberId, LocalDate issueDate,
                       LocalDate dueDate, LocalDate returnDate, double fineAmount, String status) {
        this.issueId = issueId;
        this.bookId = bookId;
        this.memberId = memberId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fineAmount = fineAmount;
        this.status = status;
    }

    // Overloaded constructor for a new issue: not returned yet, no fine, status Issued.
    public IssueRecord(String bookId, String memberId, LocalDate issueDate, LocalDate dueDate) {
        this(0, bookId, memberId, issueDate, dueDate, null, 0.0, Status.ISSUED);
    }

    public long getIssueId() {
        return issueId;
    }

    public String getBookId() {
        return bookId;
    }

    public String getMemberId() {
        return memberId;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Days after the allowed period. Uses today's date if the book is not returned yet.
    public long getLateDays() {
        LocalDate endDate = (returnDate != null) ? returnDate : LocalDate.now();
        return Library.calculateLateDays(issueDate, endDate);
    }

    public static void printHeader() {
        System.out.printf("%-8s %-8s %-8s %-12s %-12s %-12s %8s  %s%n",
                "ISSUE ID", "BOOK ID", "MEMBER", "ISSUE DATE", "DUE DATE", "RETURNED", "FINE", "STATUS");
        System.out.println("-----------------------------------------------------------------------------------------");
    }

    public void displayDetails() {
        System.out.printf("%-8d %-8s %-8s %-12s %-12s %-12s %8.2f  %s%n",
                issueId, bookId, memberId, issueDate, dueDate,
                returnDate == null ? "-" : returnDate.toString(),
                fineAmount, status);
    }
}
