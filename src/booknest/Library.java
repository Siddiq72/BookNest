package booknest;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic of BookNest: all rules and validation live here.
 * SQL is NOT written here; this class calls the DAO classes.
 */
public class Library {

    public static final int MAX_BOOKS_PER_MEMBER = 3;
    public static final int ALLOWED_DAYS = 7;
    public static final int FINE_PER_DAY = 2;

    // Static member: counts issues made during this run of the program only.
    private static int totalIssuesThisRun = 0;

    private final BookDAO bookDAO = new BookDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final IssueDAO issueDAO = new IssueDAO();
    private final LibrarianDAO librarianDAO = new LibrarianDAO();

    private Librarian librarian;

    // ======================= START UP =======================

    // Loads the librarian from MySQL. This also proves the database connection works.
    public void loadLibrarian() throws SQLException {
        librarian = librarianDAO.getFirstLibrarian();
    }

    public Librarian getLibrarian() {
        return librarian;
    }

    // ======================= STATIC HELPERS =======================

    public static int getTotalIssuesThisRun() {
        return totalIssuesThisRun;
    }

    // Days the book was kept beyond the allowed 7 days (never negative).
    public static long calculateLateDays(LocalDate issueDate, LocalDate endDate) {
        long daysKept = ChronoUnit.DAYS.between(issueDate, endDate);
        return Math.max(0, daysKept - ALLOWED_DAYS);
    }

    // Fine = max(0, days kept - 7) x Rs. 2
    public static double calculateFine(LocalDate issueDate, LocalDate endDate) {
        return calculateLateDays(issueDate, endDate) * FINE_PER_DAY;
    }

    // ======================= VALIDATION HELPERS =======================

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    // Text must not be empty or too long for its database column.
    private String requireText(String value, String fieldName, int maxLength) throws LibraryException {
        if (isBlank(value)) {
            throw new LibraryException(fieldName + " cannot be empty.");
        }
        String clean = value.trim();
        if (clean.length() > maxLength) {
            throw new LibraryException(fieldName + " cannot be longer than " + maxLength + " characters.");
        }
        return clean;
    }

    // IDs are trimmed and converted to capital letters (b005 becomes B005).
    private String requireId(String value, String fieldName) throws LibraryException {
        String clean = requireText(value, fieldName, 20);
        if (clean.contains(" ")) {
            throw new LibraryException(fieldName + " cannot contain spaces.");
        }
        return clean.toUpperCase();
    }

    // Returns null if the email is blank (email is optional).
    private String checkEmail(String email, String ownMemberId) throws LibraryException, SQLException {
        if (isBlank(email)) {
            return null;
        }
        String clean = email.trim();
        if (clean.length() > 100 || clean.contains(" ") || !clean.contains("@") || !clean.contains(".")) {
            throw new LibraryException("Email address is not valid.");
        }
        String owner = memberDAO.getMemberIdByEmail(clean);
        if (owner != null && !owner.equalsIgnoreCase(ownMemberId)) {
            throw new LibraryException("This email is already used by member " + owner + ".");
        }
        return clean;
    }

    // Returns null if the phone is blank (phone is optional).
    private String checkPhone(String phone) throws LibraryException {
        if (isBlank(phone)) {
            return null;
        }
        String clean = phone.trim();
        if (!clean.matches("[0-9]{7,15}")) {
            throw new LibraryException("Phone must contain 7 to 15 digits only.");
        }
        return clean;
    }

    // ======================= BOOKS =======================

    public void addBook(String bookId, String title, String author, int totalCopies)
            throws LibraryException, SQLException {
        String id = requireId(bookId, "Book ID");
        String cleanTitle = requireText(title, "Book title", 150);
        String cleanAuthor = requireText(author, "Author", 100);
        if (totalCopies < 0) {
            throw new LibraryException("Number of copies cannot be negative.");
        }
        if (bookDAO.getBookById(id) != null) {
            throw new LibraryException("Book ID already exists: " + id);
        }
        bookDAO.addBook(new Book(id, cleanTitle, cleanAuthor, totalCopies));
    }

    public List<Book> getAllBooks() throws SQLException {
        return bookDAO.getAllBooks();
    }

    // Overloaded searchBook() #1: search by title keyword.
    public List<Book> searchBook(String titleKeyword) throws LibraryException, SQLException {
        String keyword = requireText(titleKeyword, "Title keyword", 150);
        return bookDAO.searchByTitle(keyword);
    }

    // Overloaded searchBook() #2: search by Book ID (exact match or part of the ID).
    public List<Book> searchBook(String bookId, boolean exactMatch) throws LibraryException, SQLException {
        String id = requireId(bookId, "Book ID");
        if (exactMatch) {
            List<Book> result = new ArrayList<>();
            Book book = bookDAO.getBookById(id);
            if (book != null) {
                result.add(book);
            }
            return result;
        }
        return bookDAO.searchByIdPart(id);
    }

    // A null or blank value means "keep the old value".
    public void updateBook(String bookId, String newTitle, String newAuthor, Integer newTotalCopies)
            throws LibraryException, SQLException {
        String id = requireId(bookId, "Book ID");
        Book book = bookDAO.getBookById(id);
        if (book == null) {
            throw new LibraryException("Book not found: " + id);
        }
        if (!isBlank(newTitle)) {
            book.setTitle(requireText(newTitle, "Book title", 150));
        }
        if (!isBlank(newAuthor)) {
            book.setAuthor(requireText(newAuthor, "Author", 100));
        }
        if (newTotalCopies != null) {
            if (newTotalCopies < 0) {
                throw new LibraryException("Number of copies cannot be negative.");
            }
            int copiesOut = book.getTotalCopies() - book.getAvailableCopies();
            if (newTotalCopies < copiesOut) {
                throw new LibraryException("Total copies cannot be less than the " + copiesOut
                        + " copies currently issued.");
            }
            book.setTotalCopies(newTotalCopies);
            book.setAvailableCopies(newTotalCopies - copiesOut);
        }
        bookDAO.updateBook(book);
    }

    public void reactivateBook(String bookId) throws LibraryException, SQLException {
        String id = requireId(bookId, "Book ID");
        if (bookDAO.getBookById(id) == null) {
            throw new LibraryException("Book not found: " + id);
        }
        bookDAO.updateStatus(id, Status.ACTIVE);
    }

    // Delete rule: refuse if copies are out; deactivate if there is history; otherwise delete.
    public String removeBook(String bookId) throws LibraryException, SQLException {
        String id = requireId(bookId, "Book ID");
        if (bookDAO.getBookById(id) == null) {
            throw new LibraryException("Book not found: " + id);
        }
        if (issueDAO.countActiveByBook(id) > 0) {
            throw new LibraryException("Cannot remove this book: some copies are currently issued.");
        }
        if (issueDAO.countAllByBook(id) > 0) {
            bookDAO.updateStatus(id, Status.INACTIVE);
            return "Book has borrowing history, so it was marked Inactive (history is kept).";
        }
        bookDAO.deleteBook(id);
        return "Book deleted permanently (it had no borrowing history).";
    }

    // ======================= MEMBERS =======================

    public void registerMember(String memberId, String name, String email, String phone)
            throws LibraryException, SQLException {
        String id = requireId(memberId, "Member ID");
        String cleanName = requireText(name, "Member name", 100);
        String cleanEmail = checkEmail(email, null);
        String cleanPhone = checkPhone(phone);
        if (memberDAO.getMemberById(id) != null) {
            throw new LibraryException("Member ID already exists: " + id);
        }
        memberDAO.addMember(new Member(id, cleanName, cleanEmail, cleanPhone));
    }

    // Returns the librarian followed by all members, as Person objects (polymorphism).
    public List<Person> getAllPeople() throws SQLException {
        List<Person> people = new ArrayList<>();
        if (librarian != null) {
            people.add(librarian);
        }
        people.addAll(memberDAO.getAllMembers());
        return people;
    }

    public Member findMember(String memberId) throws LibraryException, SQLException {
        String id = requireId(memberId, "Member ID");
        Member member = memberDAO.getMemberById(id);
        if (member == null) {
            throw new LibraryException("Member not found: " + id);
        }
        return member;
    }

    // A null or blank value means "keep the old value".
    public void updateMember(String memberId, String newName, String newEmail, String newPhone)
            throws LibraryException, SQLException {
        Member member = findMember(memberId);
        if (!isBlank(newName)) {
            member.setName(requireText(newName, "Member name", 100));
        }
        if (!isBlank(newEmail)) {
            member.setEmail(checkEmail(newEmail, member.getId()));
        }
        if (!isBlank(newPhone)) {
            member.setPhone(checkPhone(newPhone));
        }
        memberDAO.updateMember(member);
    }

    public void reactivateMember(String memberId) throws LibraryException, SQLException {
        Member member = findMember(memberId);
        memberDAO.updateStatus(member.getId(), Status.ACTIVE);
    }

    public String removeMember(String memberId) throws LibraryException, SQLException {
        Member member = findMember(memberId);
        String id = member.getId();
        if (issueDAO.countActiveByMember(id) > 0) {
            throw new LibraryException("Cannot remove this member: some books are still issued to them.");
        }
        if (issueDAO.countAllByMember(id) > 0) {
            memberDAO.updateStatus(id, Status.INACTIVE);
            return "Member has borrowing history, so they were marked Inactive (history is kept).";
        }
        memberDAO.deleteMember(id);
        return "Member deleted permanently (they had no borrowing history).";
    }

    // ======================= ISSUE AND RETURN (TRANSACTIONS) =======================

    // Issues a book. Returns the number of copies left after issuing.
    public int issueBook(String memberId, String bookId) throws LibraryException, SQLException {
        String mId = requireId(memberId, "Member ID");
        String bId = requireId(bookId, "Book ID");

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);   // start the transaction
            try {
                Member member = memberDAO.getMemberById(con, mId, true);
                if (member == null) {
                    throw new LibraryException("Member not found: " + mId);
                }
                if (!Status.ACTIVE.equals(member.getStatus())) {
                    throw new LibraryException("This member is Inactive and cannot borrow books.");
                }

                Book book = bookDAO.getBookById(con, bId, true);
                if (book == null) {
                    throw new LibraryException("Book not found: " + bId);
                }
                if (!Status.ACTIVE.equals(book.getStatus())) {
                    throw new LibraryException("This book is Inactive and cannot be issued.");
                }
                if (book.getAvailableCopies() <= 0) {
                    throw new LibraryException("No copies of this book are available right now.");
                }
                if (issueDAO.countActiveByMember(con, mId) >= MAX_BOOKS_PER_MEMBER) {
                    throw new LibraryException("Borrowing limit reached: a member can hold only "
                            + MAX_BOOKS_PER_MEMBER + " books at a time.");
                }
                if (issueDAO.countActiveByMemberAndBook(con, mId, bId) > 0) {
                    throw new LibraryException("This member already holds a copy of this book.");
                }

                LocalDate today = LocalDate.now();
                IssueRecord record = new IssueRecord(bId, mId, today, today.plusDays(ALLOWED_DAYS));
                issueDAO.insertIssue(con, record);
                if (!bookDAO.decreaseAvailable(con, bId)) {
                    throw new LibraryException("Could not reduce the available copies.");
                }

                con.commit();   // both changes are saved together
                totalIssuesThisRun++;
                return book.getAvailableCopies() - 1;
            } catch (LibraryException | SQLException e) {
                con.rollback(); // undo everything if any step failed
                throw e;
            }
        }
    }

    // Returns a book. The returned IssueRecord holds the return date and the fine.
    public IssueRecord returnBook(String memberId, String bookId) throws LibraryException, SQLException {
        String mId = requireId(memberId, "Member ID");
        String bId = requireId(bookId, "Book ID");

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);   // start the transaction
            try {
                IssueRecord record = issueDAO.findLatestIssueForUpdate(con, mId, bId);
                if (record == null) {
                    throw new LibraryException("No issue record found for this member and book.");
                }
                if (Status.RETURNED.equals(record.getStatus())) {
                    throw new LibraryException("This book was already returned on "
                            + record.getReturnDate() + ".");
                }

                LocalDate today = LocalDate.now();
                double fine = calculateFine(record.getIssueDate(), today);

                if (!issueDAO.markReturned(con, record.getIssueId(), today, fine)) {
                    throw new LibraryException("Could not update the issue record.");
                }
                if (!bookDAO.increaseAvailable(con, bId)) {
                    throw new LibraryException("Could not increase the available copies.");
                }

                con.commit();
                record.setReturnDate(today);
                record.setFineAmount(fine);
                record.setStatus(Status.RETURNED);
                return record;
            } catch (LibraryException | SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ======================= REPORTS =======================

    public List<IssueRecord> getCurrentIssues() throws SQLException {
        return issueDAO.getActiveIssues();
    }

    public List<IssueRecord> getBorrowingHistory(String memberId) throws LibraryException, SQLException {
        Member member = findMember(memberId);
        return issueDAO.getHistoryByMember(member.getId());
    }

    // Fines already stored on the member's issue records.
    public double getRecordedFines(String memberId) throws LibraryException, SQLException {
        Member member = findMember(memberId);
        return issueDAO.sumFineByMember(member.getId());
    }

    // Fines building up today on books that are still issued and overdue.
    public double getAccruedFines(List<IssueRecord> history) {
        double total = 0;
        for (IssueRecord record : history) {
            if (Status.ISSUED.equals(record.getStatus())) {
                total = total + calculateFine(record.getIssueDate(), LocalDate.now());
            }
        }
        return total;
    }

    // Persistent total: counts every row in issue_record.
    public int getTotalBooksIssued() throws SQLException {
        return issueDAO.countAllIssues();
    }
}
