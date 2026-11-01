package booknest;

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Menu display and keyboard input ONLY. All rules are in Library.
 */
public class LibraryApp {

    private static final Scanner scanner = new Scanner(System.in);
    private static final Library library = new Library();

    public static void main(String[] args) {
        System.out.println("Welcome to BookNest - Library Management System");

        if (!DatabaseConnection.hasPassword()) {
            DatabaseConnection.setPassword(readLine("Enter MySQL password (press Enter if none): "));
        }

        try {
            library.loadLibrarian();   // also tests the database connection
        } catch (SQLException e) {
            System.out.println("Could not connect to the database: " + e.getMessage());
            System.out.println("Check that MySQL is running, booknest_db.sql was executed, "
                    + "the password is correct and the Connector/J jar is on the classpath.");
            return;
        }

        // Polymorphism: a Librarian object is used through a Person reference.
        Person staff = library.getLibrarian();
        if (staff != null) {
            System.out.println("Logged in as:");
            Person.printHeader();
            staff.displayDetails();
        }

        int choice = 0;
        try {
            do {
                showMenu();
                choice = readChoice("Enter choice: ");
                try {
                    handleChoice(choice);
                } catch (LibraryException e) {
                    System.out.println("Error: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Database error: " + e.getMessage());
                }
            } while (choice != 10);
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed. Goodbye!");
        }
        scanner.close();
    }

    // ======================= MENU =======================

    private static void showMenu() {
        System.out.println();
        System.out.println("===== BOOKNEST LIBRARY =====");
        System.out.println("1. Add Book       2. Register Member");
        System.out.println("3. Issue Book     4. Return Book");
        System.out.println("5. Search         6. Show All");
        System.out.println("7. Update Record  8. Delete / Deactivate");
        System.out.println("9. Borrowing History  10. Exit");
    }

    private static void handleChoice(int choice) throws LibraryException, SQLException {
        switch (choice) {
            case 1:
                addBook();
                break;
            case 2:
                registerMember();
                break;
            case 3:
                issueBook();
                break;
            case 4:
                returnBook();
                break;
            case 5:
                searchBooks();
                break;
            case 6:
                showAll();
                break;
            case 7:
                updateRecord();
                break;
            case 8:
                deleteRecord();
                break;
            case 9:
                showBorrowingHistory();
                break;
            case 10:
                System.out.println("Thank you for using BookNest. Goodbye!");
                break;
            default:
                System.out.println("Invalid choice. Please enter a number from 1 to 10.");
        }
    }

    // ======================= INPUT HELPERS =======================

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    // Returns -1 if the text is not a whole number.
    private static int readChoice(String prompt) {
        String text = readLine(prompt);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int readInt(String prompt) throws LibraryException {
        String text = readLine(prompt);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new LibraryException("Please enter a valid whole number.");
        }
    }

    // Blank input returns null, which means "keep the old value".
    private static Integer readOptionalInt(String prompt) throws LibraryException {
        String text = readLine(prompt);
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            throw new LibraryException("Please enter a valid whole number.");
        }
    }

    // ======================= MENU ACTIONS =======================

    private static void addBook() throws LibraryException, SQLException {
        System.out.println("--- Add Book ---");
        String bookId = readLine("Enter Book ID: ");
        String title = readLine("Enter title: ");
        String author = readLine("Enter author: ");
        int copies = readInt("Enter number of copies: ");
        library.addBook(bookId, title, author, copies);
        System.out.println("Book added successfully. Saved to MySQL.");
    }

    private static void registerMember() throws LibraryException, SQLException {
        System.out.println("--- Register Member ---");
        String memberId = readLine("Enter Member ID: ");
        String name = readLine("Enter name: ");
        String email = readLine("Enter email (optional): ");
        String phone = readLine("Enter phone (optional): ");
        library.registerMember(memberId, name, email, phone);
        System.out.println("Member registered successfully. Saved to MySQL.");
    }

    private static void issueBook() throws LibraryException, SQLException {
        System.out.println("--- Issue Book ---");
        String memberId = readLine("Enter Member ID: ");
        String bookId = readLine("Enter Book ID: ");
        int copiesLeft = library.issueBook(memberId, bookId);
        System.out.println("Book issued successfully.");
        System.out.println("Copies left: " + copiesLeft);
        System.out.println("Issue record saved to MySQL. Return within "
                + Library.ALLOWED_DAYS + " days to avoid a fine.");
    }

    private static void returnBook() throws LibraryException, SQLException {
        System.out.println("--- Return Book ---");
        String memberId = readLine("Enter Member ID: ");
        String bookId = readLine("Enter Book ID: ");
        IssueRecord record = library.returnBook(memberId, bookId);
        System.out.println("Return date recorded: " + record.getReturnDate());
        if (record.getLateDays() > 0) {
            System.out.println("Late by " + record.getLateDays() + " days. Fine = Rs. "
                    + (int) record.getFineAmount());
        } else {
            System.out.println("Returned on time. No fine.");
        }
        System.out.println("Book returned successfully. Database updated.");
    }

    private static void searchBooks() throws LibraryException, SQLException {
        System.out.println("--- Search Books ---");
        System.out.println("1. By title keyword");
        System.out.println("2. By Book ID (exact)");
        System.out.println("3. By part of Book ID");
        int option = readChoice("Choose search type: ");

        List<Book> results;
        if (option == 1) {
            results = library.searchBook(readLine("Enter title keyword: "));
        } else if (option == 2) {
            results = library.searchBook(readLine("Enter Book ID: "), true);
        } else if (option == 3) {
            results = library.searchBook(readLine("Enter part of Book ID: "), false);
        } else {
            System.out.println("Invalid search option.");
            return;
        }
        printBooks(results);
    }

    private static void showAll() throws LibraryException, SQLException {
        System.out.println("--- Show All ---");
        System.out.println("1. Books");
        System.out.println("2. Librarian and members");
        System.out.println("3. Current issue records");
        System.out.println("4. Total books issued");
        int option = readChoice("Choose: ");

        if (option == 1) {
            printBooks(library.getAllBooks());
        } else if (option == 2) {
            // Polymorphism: each Person object prints itself in its own way.
            List<Person> people = library.getAllPeople();
            Person.printHeader();
            for (Person person : people) {
                person.displayDetails();
            }
        } else if (option == 3) {
            printIssues(library.getCurrentIssues());
        } else if (option == 4) {
            System.out.println("Total books issued (from MySQL, all time): " + library.getTotalBooksIssued());
            System.out.println("Books issued during this run (static counter): "
                    + Library.getTotalIssuesThisRun());
        } else {
            System.out.println("Invalid option.");
        }
    }

    private static void updateRecord() throws LibraryException, SQLException {
        System.out.println("--- Update Record ---");
        System.out.println("1. Update book details");
        System.out.println("2. Update member details");
        System.out.println("3. Re-activate a book");
        System.out.println("4. Re-activate a member");
        int option = readChoice("Choose: ");

        if (option == 1) {
            String bookId = readLine("Enter Book ID to update: ");
            System.out.println("Press Enter to keep the current value.");
            String title = readLine("New title: ");
            String author = readLine("New author: ");
            Integer copies = readOptionalInt("New total copies: ");
            library.updateBook(bookId, title, author, copies);
            System.out.println("Book updated successfully.");
        } else if (option == 2) {
            String memberId = readLine("Enter Member ID to update: ");
            System.out.println("Press Enter to keep the current value.");
            String name = readLine("New name: ");
            String email = readLine("New email: ");
            String phone = readLine("New phone: ");
            library.updateMember(memberId, name, email, phone);
            System.out.println("Member updated successfully.");
        } else if (option == 3) {
            library.reactivateBook(readLine("Enter Book ID: "));
            System.out.println("Book is Active again.");
        } else if (option == 4) {
            library.reactivateMember(readLine("Enter Member ID: "));
            System.out.println("Member is Active again.");
        } else {
            System.out.println("Invalid option.");
        }
    }

    private static void deleteRecord() throws LibraryException, SQLException {
        System.out.println("--- Delete / Deactivate ---");
        System.out.println("1. Book");
        System.out.println("2. Member");
        int option = readChoice("Choose: ");

        if (option == 1) {
            System.out.println(library.removeBook(readLine("Enter Book ID: ")));
        } else if (option == 2) {
            System.out.println(library.removeMember(readLine("Enter Member ID: ")));
        } else {
            System.out.println("Invalid option.");
        }
    }

    private static void showBorrowingHistory() throws LibraryException, SQLException {
        System.out.println("--- Borrowing History ---");
        String memberId = readLine("Enter Member ID: ");
        Member member = library.findMember(memberId);
        List<IssueRecord> history = library.getBorrowingHistory(memberId);

        System.out.println("Member: " + member.getId() + " - " + member.getName());
        printIssues(history);

        double recorded = library.getRecordedFines(memberId);
        double accruing = library.getAccruedFines(history);
        System.out.printf("Fines stored on returned books : Rs. %.2f%n", recorded);
        System.out.printf("Fines building up today        : Rs. %.2f%n", accruing);
        System.out.printf("Total fines                    : Rs. %.2f%n", recorded + accruing);
    }

    // ======================= PRINT HELPERS =======================

    private static void printBooks(List<Book> books) {
        if (books.isEmpty()) {
            System.out.println("No books found.");
            return;
        }
        Book.printHeader();
        for (Book book : books) {
            book.displayDetails();
        }
    }

    private static void printIssues(List<IssueRecord> records) {
        if (records.isEmpty()) {
            System.out.println("No issue records found.");
            return;
        }
        IssueRecord.printHeader();
        for (IssueRecord record : records) {
            record.displayDetails();
        }
    }
}
