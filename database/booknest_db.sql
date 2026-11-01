-- =====================================================================
-- BookNest - Library Management System
-- MySQL database script (tables, constraints and sample data)
--
-- Run it with:   mysql -u root -p < database/booknest_db.sql
-- Safe to run more than once: tables are created only if missing and the
-- sample rows are inserted with INSERT IGNORE.
-- Needs MySQL 8.0.16 or newer so that CHECK constraints are enforced.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS booknest_db;
USE booknest_db;

-- ---------------------------------------------------------------------
-- BOOK
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS book (
    book_id          VARCHAR(20)  PRIMARY KEY,
    title            VARCHAR(150) NOT NULL,
    author           VARCHAR(100) NOT NULL,
    total_copies     INT          NOT NULL,
    available_copies INT          NOT NULL,
    status           VARCHAR(15)  NOT NULL DEFAULT 'Active',
    CONSTRAINT chk_book_copies CHECK (total_copies >= 0
                                      AND available_copies >= 0
                                      AND available_copies <= total_copies),
    CONSTRAINT chk_book_status CHECK (status IN ('Active', 'Inactive'))
);

-- ---------------------------------------------------------------------
-- MEMBER  (email is optional but unique when given; MySQL allows many NULLs)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS member (
    member_id   VARCHAR(20)  PRIMARY KEY,
    member_name VARCHAR(100) NOT NULL,
    email       VARCHAR(100) UNIQUE,
    phone       VARCHAR(15),
    status      VARCHAR(15)  NOT NULL DEFAULT 'Active',
    CONSTRAINT chk_member_status CHECK (status IN ('Active', 'Inactive'))
);

-- ---------------------------------------------------------------------
-- LIBRARIAN (the staff user shown when the program starts)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS librarian (
    librarian_id   VARCHAR(20)  PRIMARY KEY,
    librarian_name VARCHAR(100) NOT NULL,
    username       VARCHAR(50)  UNIQUE
);

-- ---------------------------------------------------------------------
-- ISSUE_RECORD (one row every time a book is issued)
-- No ON DELETE CASCADE on purpose: history must never disappear.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS issue_record (
    issue_id    BIGINT        AUTO_INCREMENT PRIMARY KEY,
    book_id     VARCHAR(20)   NOT NULL,
    member_id   VARCHAR(20)   NOT NULL,
    issue_date  DATE          NOT NULL,
    due_date    DATE          NOT NULL,
    return_date DATE          NULL,
    fine_amount DECIMAL(8,2)  NOT NULL DEFAULT 0.00,
    status      VARCHAR(15)   NOT NULL DEFAULT 'Issued',
    CONSTRAINT fk_issue_book   FOREIGN KEY (book_id)   REFERENCES book(book_id),
    CONSTRAINT fk_issue_member FOREIGN KEY (member_id) REFERENCES member(member_id),
    CONSTRAINT chk_issue_status CHECK (status IN ('Issued', 'Returned')),
    CONSTRAINT chk_issue_fine   CHECK (fine_amount >= 0),
    CONSTRAINT chk_issue_dates  CHECK (due_date >= issue_date)
);

-- =====================================================================
-- SAMPLE DATA
-- =====================================================================
INSERT IGNORE INTO librarian (librarian_id, librarian_name, username) VALUES
    ('L001', 'Library Admin', 'admin');

INSERT IGNORE INTO book (book_id, title, author, total_copies, available_copies, status) VALUES
    ('B001', 'Head First Java',              'Kathy Sierra',            3, 2, 'Active'),
    ('B002', 'Database System Concepts',     'Abraham Silberschatz',    2, 2, 'Active'),
    ('B003', 'Operating System Concepts',    'Abraham Silberschatz',    4, 4, 'Active'),
    ('B004', 'Let Us C',                     'Yashavant Kanetkar',      1, 1, 'Active'),
    ('B005', 'Data Structures Using C',      'Reema Thareja',           3, 2, 'Active');

INSERT IGNORE INTO member (member_id, member_name, email, phone, status) VALUES
    ('M101', 'Asha Raj',    'asha@example.com',  '9876543210', 'Active'),
    ('M102', 'Ravi Kumar',  'ravi@example.com',  '9123456780', 'Active'),
    ('M103', 'Meena S',     NULL,                NULL,         'Active');

-- Sample issue records (dates are relative to the day you run this script):
--  1) M101 has B005 for 12 days (already late) -> returning it today gives Rs. 10 fine
--  2) M102 has B001 for 3 days (not late)
--  3) M102 returned B002 after 10 days -> Rs. 6 fine already stored
INSERT IGNORE INTO issue_record
    (issue_id, book_id, member_id, issue_date, due_date, return_date, fine_amount, status) VALUES
    (1, 'B005', 'M101', DATE_SUB(CURDATE(), INTERVAL 12 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY),
        NULL, 0.00, 'Issued'),
    (2, 'B001', 'M102', DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 4 DAY),
        NULL, 0.00, 'Issued'),
    (3, 'B002', 'M102', DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 13 DAY),
        DATE_SUB(CURDATE(), INTERVAL 10 DAY), 6.00, 'Returned');

-- To start again from scratch, run:  DROP DATABASE booknest_db;  and run this script again.
