# Digital Library Management System

This is a comprehensive, web-based digital library management system built with Java and Spring Boot. I developed this project for Task 5 of the Oasis Infobyte Java Development internship. It is designed to handle all core operations of a library, offering separate interfaces for administrators and regular members.

## Core Technologies

- **Backend:** Java 17, Spring Boot 3.5, Spring Data JPA, Spring Security
- **Frontend:** Thymeleaf templates, plain HTML/CSS (featuring a modern glassmorphism design)
- **Database:** SQLite (configured for low-concurrency environments with a dedicated connection pool setup)
- **Build Tool:** Maven

## Features

### For Members
- **Catalogue Browsing:** Users can view all available books, search by title or author, and filter by category.
- **Circulation:** Members can issue available books, return books they are currently reading, and renew items before they become overdue.
- **Advance Bookings:** If a book is out of stock, users can reserve the next available copy. The system automatically places them in a queue and holds the book when it is returned.
- **Dashboard:** A personalized view showing active loans, due dates, outstanding fines, and borrowing history.
- **Support Queries:** A contact form allows users to send messages directly to the librarians.

### For Administrators
- **Dashboard Overview:** A snapshot of library metrics, including total books, active members, current issues, overdue items, and collected fines.
- **Book Management:** Full CRUD operations for the catalogue. Admins can add new titles, update existing records, and adjust stock quantities.
- **Circulation Desk:** Track all active and overdue loans. Admins can manually process returns and review the complete history of book movements.
- **Member Management:** View all registered users, their borrowing history, and outstanding fines. Admins have the ability to deactivate or delete accounts if necessary.
- **Fine Management:** The system automatically accrues fines for overdue books at a fixed daily rate. Admins can track unpaid fines, mark them as paid, or waive them entirely.
- **Query Management:** A dedicated panel to read and reply to user support messages.

## How to Run

1. Ensure you have Java 17 or higher installed.
2. Open a terminal in the project root directory.
3. Run the application using the Maven wrapper:
   - On Windows: `.\mvnw.cmd spring-boot:run`
   - On Mac/Linux: `./mvnw spring-boot:run`
4. Once the server starts, open your web browser and navigate to `http://localhost:8080`.

### Demo Credentials

The application automatically seeds a few demo accounts and a starter catalogue on the first launch. You can log in using:

**Administrator Access**
- Email: admin@library.com
- Password: admin123

**Member Access**
- Email: user@library.com
- Password: user1234

## Technical Notes

I chose to use SQLite for this project to keep the deployment simple and self-contained, meaning you do not need to install or configure a separate database server like MySQL to test the application. However, because SQLite is a file-based database, I configured the Hikari connection pool size to 1 in the application properties to prevent database locking issues during concurrent writes.

The user interface was built using a custom vanilla CSS design system, entirely avoiding external frameworks to ensure complete control over the layout and aesthetic.
