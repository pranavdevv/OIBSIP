package com.oasis.library.config;

import com.oasis.library.model.*;
import com.oasis.library.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.File;
import java.time.LocalDate;

/** Seeds an admin, demo members and a starter catalogue on first launch. */
@Configuration
public class DataSeeder {

    static {
        // make sure the SQLite folder exists before the DataSource starts
        new File("data").mkdirs();
    }

    @Bean
    CommandLineRunner seed(UserRepository users, BookRepository books, IssueRepository issues,
                           ReservationRepository reservations, ContactMessageRepository messages,
                           PasswordEncoder enc) {
        return args -> {
            if (users.count() > 0) return;

            User admin = users.save(new User("Library Admin", "admin@library.com", "9000000000", enc.encode("admin123"), User.Role.ADMIN));
            User pranav = users.save(new User("Pranav Kumar", "user@library.com", "9876543210", enc.encode("user1234"), User.Role.USER));
            User ananya = users.save(new User("Ananya Sharma", "ananya@library.com", "9123456780", enc.encode("user1234"), User.Role.USER));

            Object[][] data = {
                {"Clean Code", "Robert C. Martin", "9780132350884", "Programming", 3, 2008, "A handbook of agile software craftsmanship."},
                {"Effective Java", "Joshua Bloch", "9780134685991", "Programming", 2, 2018, "Best practices for the Java platform."},
                {"Head First Java", "Kathy Sierra", "9780596009205", "Programming", 4, 2005, "A brain-friendly guide to Java."},
                {"The Pragmatic Programmer", "Andrew Hunt", "9780135957059", "Programming", 2, 2019, "Your journey to mastery."},
                {"Design Patterns", "Erich Gamma", "9780201633610", "Programming", 1, 1994, "Elements of reusable object-oriented software."},
                {"Introduction to Algorithms", "Thomas H. Cormen", "9780262046305", "Computer Science", 2, 2022, "The classic CLRS algorithms text."},
                {"Operating System Concepts", "Abraham Silberschatz", "9781119800361", "Computer Science", 2, 2021, "The dinosaur book."},
                {"Computer Networks", "Andrew S. Tanenbaum", "9780132126953", "Computer Science", 1, 2010, "Top-down look at networking."},
                {"Database System Concepts", "Henry F. Korth", "9780078022159", "Computer Science", 2, 2019, "Foundations of database systems."},
                {"Wings of Fire", "A.P.J. Abdul Kalam", "9788173711466", "Biography", 3, 1999, "Autobiography of India's Missile Man."},
                {"Steve Jobs", "Walter Isaacson", "9781451648539", "Biography", 1, 2011, "The exclusive biography."},
                {"The Alchemist", "Paulo Coelho", "9780062315007", "Fiction", 3, 1988, "A fable about following your dream."},
                {"1984", "George Orwell", "9780451524935", "Fiction", 2, 1949, "A dystopian social science fiction novel."},
                {"To Kill a Mockingbird", "Harper Lee", "9780061120084", "Fiction", 2, 1960, "A novel about justice and growing up."},
                {"The God of Small Things", "Arundhati Roy", "9780812979657", "Fiction", 1, 1997, "Booker Prize winning debut novel."},
                {"Sapiens", "Yuval Noah Harari", "9780062316097", "History", 2, 2011, "A brief history of humankind."},
                {"The Discovery of India", "Jawaharlal Nehru", "9780143031031", "History", 1, 1946, "Written during imprisonment at Ahmednagar Fort."},
                {"A Brief History of Time", "Stephen Hawking", "9780553380163", "Science", 2, 1988, "From the Big Bang to black holes."},
                {"Cosmos", "Carl Sagan", "9780345539434", "Science", 1, 1980, "A personal voyage through the universe."},
                {"Atomic Habits", "James Clear", "9780735211292", "Self-Help", 3, 2018, "Tiny changes, remarkable results."},
                {"Deep Work", "Cal Newport", "9781455586691", "Self-Help", 1, 2016, "Rules for focused success."},
            };
            for (Object[] d : data) {
                books.save(new Book((String) d[0], (String) d[1], (String) d[2], (String) d[3], (Integer) d[4], (Integer) d[5], (String) d[6]));
            }

            LocalDate today = LocalDate.now();
            // Pranav: one overdue loan (fine accruing), one on time, one returned late with unpaid fine
            loan(issues, books, pranav, "9780132350884", today.minusDays(20), today.minusDays(6), null);
            loan(issues, books, pranav, "9780735211292", today.minusDays(3), today.plusDays(11), null);
            Issue late = loan(issues, books, pranav, "9780062315007", today.minusDays(30), today.minusDays(16), today.minusDays(12));
            late.setFine(20);
            issues.save(late);

            // Ananya holds the only copy of Design Patterns -> Pranav can demo an advance booking
            loan(issues, books, ananya, "9780201633610", today.minusDays(2), today.plusDays(12), null);
            // Ananya returned a book late and already paid
            Issue paid = loan(issues, books, ananya, "9780553380163", today.minusDays(25), today.minusDays(11), today.minusDays(8));
            paid.setFine(15); paid.setFinePaid(true); paid.setFinePaidOn(today.minusDays(8));
            issues.save(paid);

            ContactMessage m = new ContactMessage();
            m.setName("Ananya Sharma"); m.setEmail("ananya@library.com");
            m.setSubject("Request: more copies of Clean Code");
            m.setMessage("Clean Code is always issued. Could the library add two more copies, please?");
            messages.save(m);
        };
    }

    private Issue loan(IssueRepository issues, BookRepository books, User u, String isbn,
                       LocalDate issued, LocalDate due, LocalDate returned) {
        Book b = books.findByIsbn(isbn).orElseThrow();
        Issue i = new Issue(u, b, issued, due);
        i.setReturnDate(returned);
        if (returned == null) {
            b.setAvailable(b.getAvailable() - 1);
            books.save(b);
        }
        return issues.save(i);
    }
}
