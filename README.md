# BookNest - Library Management System

A menu-driven **Java console application** that uses **MySQL** (through **JDBC**) to manage a college library:
books, members, issuing and returning books, searching, late fines and borrowing history.

Built for the *OOPs + DBMS Integrated Project*. It uses core Java only: no Spring, no Hibernate, no GUI.

---

## 1. Requirements

| Software | Version |
| --- | --- |
| JDK | 11 or newer (developed against JDK 21) |
| MySQL Server | 8.0.16 or newer (older versions ignore `CHECK` constraints) |
| MySQL Connector/J | 8.x (a single `.jar` file) |

## 2. Project structure

```
BookNest/
├── README.md
├── .gitignore
├── database/
│   └── booknest_db.sql          <- creates booknest_db, tables, sample data
├── lib/
│   └── (put mysql-connector-j-x.y.z.jar here)
└── src/booknest/
    ├── LibraryApp.java          <- main(), menu and keyboard input only
    ├── Library.java             <- business rules and validation
    ├── Person.java              <- abstract parent class
    ├── Member.java              <- extends Person
    ├── Librarian.java           <- extends Person
    ├── Book.java
    ├── IssueRecord.java
    ├── Status.java              <- "Active", "Inactive", "Issued", "Returned"
    ├── LibraryException.java    <- custom exception for rule violations
    ├── DatabaseConnection.java  <- creates JDBC connections
    ├── BookDAO.java             <- SQL for the book table
    ├── MemberDAO.java           <- SQL for the member table
    ├── IssueDAO.java            <- SQL for the issue_record table
    └── LibrarianDAO.java        <- SQL for the librarian table
```

## 3. Database setup

1. Start MySQL.
2. From the project folder run:
   ```
   mysql -u root -p < database/booknest_db.sql
   ```
   (or open the file in MySQL Workbench and run it).

This creates `booknest_db` with the tables `book`, `member`, `librarian` and `issue_record`, plus a few sample rows.
The script can be run again safely. Table and column names are lowercase because MySQL on Linux is case-sensitive.

### Tables

| Table | Primary key | Foreign keys | Main constraints |
| --- | --- | --- | --- |
| `book` | `book_id` | - | `NOT NULL` title/author; `CHECK` copies >= 0 and available <= total; status Active/Inactive |
| `member` | `member_id` | - | `NOT NULL` name; `UNIQUE` email; status Active/Inactive |
| `librarian` | `librarian_id` | - | `UNIQUE` username |
| `issue_record` | `issue_id` (auto-increment) | `book_id` -> `book`, `member_id` -> `member` | status Issued/Returned; fine >= 0; due date >= issue date |

One book and one member can each have many issue records (one-to-many). There is no `ON DELETE CASCADE`, so history is never lost.

## 4. Download the JDBC driver

Download **MySQL Connector/J** (choose *Platform Independent*, then unzip) from
<https://dev.mysql.com/downloads/connector/j/> and copy the `mysql-connector-j-x.y.z.jar` file into the `lib/` folder.
The jar is listed in `.gitignore`, so it is not uploaded to GitHub.

## 5. Database login (no passwords in the code)

`DatabaseConnection.java` reads these **environment variables**. All are optional:

| Variable | Default |
| --- | --- |
| `BOOKNEST_DB_URL` | `jdbc:mysql://localhost:3306/booknest_db?useSSL=false&allowPublicKeyRetrieval=true` |
| `BOOKNEST_DB_USER` | `root` |
| `BOOKNEST_DB_PASSWORD` | not set: the program asks for the password when it starts |

Example (Linux/macOS): `export BOOKNEST_DB_PASSWORD=yourpassword`
Example (Windows PowerShell): `$env:BOOKNEST_DB_PASSWORD="yourpassword"`

## 6. Compile and run

From the project folder:

**Linux / macOS**
```
javac -d out src/booknest/*.java
java -cp "out:lib/*" booknest.LibraryApp
```

**Windows (Command Prompt)**
```
javac -d out src\booknest\*.java
java -cp "out;lib/*" booknest.LibraryApp
```

If the program says *No suitable driver found*, the Connector/J jar is not in `lib/`.

## 7. Try it (using the sample data)

| Try this | Expected result |
| --- | --- |
| `6` -> `1` | Lists books B001-B005 |
| `4` -> Member `M101`, Book `B005` | Book was issued 12 days before the script ran, so: late by 5 days, **Fine = Rs. 10** (if run on the same day as the SQL script) |
| `3` -> Member `M103`, Book `B003` | Issued, copies left 3 |
| Issue `B004`, `B001`, then `B002` to `M103` | The 4th book is rejected: limit of 3 books |
| `1` with an existing Book ID | "Book ID already exists" |
| `1` with copies `-5` or `abc` | Rejected with a clear message |
| Type `abc` at the main menu | "Invalid choice" and the menu appears again |
| `8` -> Book `B002` | Has history, so it is marked **Inactive** (not deleted) |
| `8` -> a book that is currently issued | Refused |
| `9` -> Member `M102` | History, stored fine Rs. 6.00 and any fines building up |

## 8. Requirement coverage

| ID | Requirement | Where |
| --- | --- | --- |
| FR-1 | Menu loop until Exit | `LibraryApp.main()` (do-while) |
| FR-2 | Add book, unique ID, saved in MySQL | `Library.addBook()` -> `BookDAO.addBook()` |
| FR-3 | Register member | `Library.registerMember()` -> `MemberDAO.addMember()` |
| FR-4 | Issue only if copy available and member holds < 3 books | `Library.issueBook()` |
| FR-5 | Return, update availability, fine Rs. 2/day after 7 days | `Library.returnBook()`, `Library.calculateFine()` |
| FR-6 | Reject duplicate IDs, negative copies, empty names, non-numeric menu choice | `Library` validation helpers, `LibraryApp.readChoice()/readInt()` |
| FR-7 | Overloaded `searchBook()` | `Library.searchBook(String)` and `Library.searchBook(String, boolean)` |
| FR-8 | Total books issued (DB query + static counter) | `IssueDAO.countAllIssues()`, `Library.getTotalIssuesThisRun()` |
| FR-9 | Show all books, members, current issues | Menu 6 |
| FR-10 | Update books and members | Menu 7 |
| FR-11 | Delete or deactivate when no active issue blocks it | Menu 8 |
| FR-12 | Borrowing history and fines | Menu 9 |

## 9. How the code works

### Program flow
`LibraryApp` (menu) -> `Library` (rules) -> DAO classes (SQL via JDBC) -> MySQL.
Results come back as `Book`, `Member` or `IssueRecord` objects.

### Where each OOP concept is used

| Concept | Where |
| --- | --- |
| Classes and objects | `Book`, `Member`, `Librarian`, `IssueRecord`, `Library` |
| Constructors and `this` | Every model class; `this(...)` chains the shorter constructor in `Book`, `Member`, `IssueRecord` |
| Encapsulation | All model fields are `private` with getters/setters |
| Abstraction | `abstract class Person` with abstract `displayDetails()` |
| Inheritance | `Member` and `Librarian` extend `Person` |
| Overriding | `displayDetails()` in `Member` and `Librarian` |
| Polymorphism | `LibraryApp` uses `Person` references (the logged-in librarian, and *Show All -> Librarian and members*), so each object prints itself differently |
| Overloading | `searchBook(...)` in `Library`; overloaded constructors; `getBookById(...)` / `getMemberById(...)` in the DAOs |
| Static members | `Library.totalIssuesThisRun`, `calculateFine()`, `DatabaseConnection` |
| Exception handling | `LibraryException` for rule violations; `SQLException` for database errors; both caught in one place in `main()` |

### How JDBC connects Java to MySQL
1. `DatabaseConnection.getConnection()` calls `DriverManager.getConnection(url, user, password)`. The Connector/J jar registers itself automatically.
2. A DAO writes SQL with `?` placeholders and creates a `PreparedStatement`.
3. Values are set with `setString()`, `setInt()`, `setDate()`. This prevents SQL injection.
4. `executeUpdate()` runs INSERT/UPDATE/DELETE; `executeQuery()` runs SELECT and returns a `ResultSet`.
5. Each row of the `ResultSet` is converted to a Java object in a `mapRow()` method.
6. `try-with-resources` closes the Connection, PreparedStatement and ResultSet automatically.

### CRUD summary

| Operation | Example in BookNest | SQL |
| --- | --- | --- |
| Create | Add book, register member, issue a book | `INSERT` |
| Read | Show all, search, history | `SELECT` |
| Update | Edit details, re-activate, return a book (date, fine, status) | `UPDATE` |
| Delete | Remove a book/member with no history; otherwise mark Inactive | `DELETE` / `UPDATE` |

### Transactions
`Library.issueBook()` turns auto-commit off, checks the member and book, counts the member's active issues, inserts the issue record and reduces `available_copies`. Then it calls `commit()`. If anything fails, `rollback()` undoes every step. `returnBook()` works the same way (update record + fine, increase copies). Rows are read with `FOR UPDATE` so two librarians cannot issue the last copy at the same time.

## 10. Decisions where the problem statement was unclear

1. **Book ID type.** The statement suggests `searchBook(int or String bookId)`, but Book IDs are `VARCHAR` (for example `B005`). So the two overloads are `searchBook(String titleKeyword)` and `searchBook(String bookId, boolean exactMatch)`.
2. **"Outstanding fines."** The `issue_record` table has no "fine paid" column, so there is no way to know if a fine was paid. Menu 9 therefore shows (a) fines stored on returned books, (b) fines building up today on overdue books, and (c) the total.
3. **Return date.** The return date is always today's date (`LocalDate.now()`); the librarian does not type it.
4. **Same book twice.** A member cannot hold two copies of the same book at once. This is an extra rule so that "member + book" identifies one issue record for returns.
5. **Zero copies.** `0` copies is allowed when adding a book, because the statement only forbids negative numbers.
6. **IDs.** IDs are trimmed and stored in capital letters (`b005` becomes `B005`).
7. **Delete or deactivate.** The program chooses automatically: refuse if something is currently issued; mark `Inactive` if there is history; physically delete only if there is no history.
8. **Librarian.** The `librarian` table is optional in the statement. It is used only to show who is logged in; there is no login screen.

## 11. Testing status

- All Java files compile without errors or warnings (checked with JDK 21).
- Checked without a database: fine calculation (7 days = Rs. 0, 8 days = Rs. 2, 12 days = Rs. 10) and the input validation messages (empty IDs/names/titles, negative copies, bad email or phone, empty search text).
- The "cannot connect" path was checked: the program prints a helpful message and exits.
- **Not tested here:** the SQL script and every feature that talks to MySQL (CRUD, issue/return transactions, 3-book limit, deletion rules). No MySQL server was available while this project was written. Please run through the table in section 7 on your own machine and report any error message you see.
