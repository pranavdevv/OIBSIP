package com.oasis.library.service;

import com.oasis.library.model.*;
import com.oasis.library.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Core circulation rules: issuing, returning, fines and advance bookings.
 * All methods throw {@link LibraryException} with a user-friendly message on rule violations.
 */
@Service
@Transactional
public class LibraryService {

    private static final List<Reservation.Status> ACTIVE = List.of(Reservation.Status.WAITING, Reservation.Status.READY);
    private static final int HOLD_DAYS = 3;

    private final BookRepository books;
    private final IssueRepository issues;
    private final ReservationRepository reservations;
    private final UserRepository users;

    @Value("${library.loan-days:14}")       private int loanDays;
    @Value("${library.fine-per-day:5}")     private int finePerDay;
    @Value("${library.max-active-loans:3}") private int maxActiveLoans;

    public LibraryService(BookRepository books, IssueRepository issues,
                          ReservationRepository reservations, UserRepository users) {
        this.books = books;
        this.issues = issues;
        this.reservations = reservations;
        this.users = users;
    }

    public int getFinePerDay() { return finePerDay; }
    public int getLoanDays() { return loanDays; }
    public int getMaxActiveLoans() { return maxActiveLoans; }

    // ------------------------------------------------------------------ issue

    public Issue issueBook(User user, Long bookId) {
        expireHolds();
        Book book = books.findById(bookId).orElseThrow(() -> new LibraryException("Book not found."));

        if (issues.existsByUserAndBookAndReturnDateIsNull(user, book))
            throw new LibraryException("You already have a copy of this book.");
        if (issues.countByUserAndReturnDateIsNull(user) >= maxActiveLoans)
            throw new LibraryException("Loan limit reached: you can hold at most " + maxActiveLoans + " books at a time.");
        long unpaid = outstandingFines(user);
        if (unpaid > 0)
            throw new LibraryException("Please clear your outstanding fine of ₹" + unpaid + " before borrowing.");

        Optional<Reservation> mine = reservations.findFirstByUserAndBookAndStatusIn(
                user, book, List.of(Reservation.Status.READY));
        long heldForOthers = reservations.countByBookAndStatus(book, Reservation.Status.READY) - (mine.isPresent() ? 1 : 0);

        if (book.getAvailable() - heldForOthers <= 0)
            throw new LibraryException("No copies available right now. You can place an advance booking instead.");

        mine.ifPresent(r -> r.setStatus(Reservation.Status.FULFILLED));
        // a WAITING reservation by this user becomes fulfilled too
        reservations.findFirstByUserAndBookAndStatusIn(user, book, List.of(Reservation.Status.WAITING))
                .ifPresent(r -> r.setStatus(Reservation.Status.FULFILLED));

        book.setAvailable(book.getAvailable() - 1);
        LocalDate today = LocalDate.now();
        return issues.save(new Issue(user, book, today, today.plusDays(loanDays)));
    }

    // ----------------------------------------------------------------- return

    /** Returns a loan. Users may only return their own; admins may return any. */
    public Issue returnBook(User actor, Long issueId) {
        Issue issue = issues.findById(issueId).orElseThrow(() -> new LibraryException("Loan not found."));
        if (!actor.isAdmin() && !issue.getUser().getId().equals(actor.getId()))
            throw new LibraryException("You can only return your own books.");
        if (issue.isReturned())
            throw new LibraryException("This book was already returned.");

        issue.setReturnDate(LocalDate.now());
        issue.setFine((int) issue.getDaysOverdue() * finePerDay);

        Book book = issue.getBook();
        book.setAvailable(book.getAvailable() + 1);
        promoteNextReservation(book);
        return issue;
    }

    /** Renew a loan by 7 days (only if not overdue and nobody is waiting). */
    public void renew(User actor, Long issueId) {
        Issue issue = issues.findById(issueId).orElseThrow(() -> new LibraryException("Loan not found."));
        if (!actor.isAdmin() && !issue.getUser().getId().equals(actor.getId()))
            throw new LibraryException("Not allowed.");
        if (issue.isReturned()) throw new LibraryException("Loan already closed.");
        if (issue.isOverdue()) throw new LibraryException("Overdue books cannot be renewed. Please return it.");
        if (!reservations.findByBookAndStatusInOrderByCreatedAtAsc(issue.getBook(), ACTIVE).isEmpty())
            throw new LibraryException("Cannot renew: other members are waiting for this book.");
        issue.setDueDate(issue.getDueDate().plusDays(7));
    }

    // ------------------------------------------------------------ reservation

    public Reservation reserve(User user, Long bookId) {
        expireHolds();
        Book book = books.findById(bookId).orElseThrow(() -> new LibraryException("Book not found."));
        if (issues.existsByUserAndBookAndReturnDateIsNull(user, book))
            throw new LibraryException("You already have this book.");
        if (reservations.findFirstByUserAndBookAndStatusIn(user, book, ACTIVE).isPresent())
            throw new LibraryException("You already have an active booking for this book.");
        long ready = reservations.countByBookAndStatus(book, Reservation.Status.READY);
        if (book.getAvailable() - ready > 0)
            throw new LibraryException("This book is available — you can issue it directly.");
        return reservations.save(new Reservation(user, book));
    }

    public void cancelReservation(User user, Long id) {
        Reservation r = reservations.findById(id).orElseThrow(() -> new LibraryException("Booking not found."));
        if (!user.isAdmin() && !r.getUser().getId().equals(user.getId()))
            throw new LibraryException("Not allowed.");
        if (!r.isActive()) throw new LibraryException("Booking is no longer active.");
        boolean wasReady = r.getStatus() == Reservation.Status.READY;
        r.setStatus(Reservation.Status.CANCELLED);
        if (wasReady) promoteNextReservation(r.getBook());
    }

    public int queuePosition(Reservation r) {
        if (r.getStatus() != Reservation.Status.WAITING) return 0;
        List<Reservation> q = reservations.findByBookAndStatusInOrderByCreatedAtAsc(
                r.getBook(), List.of(Reservation.Status.WAITING));
        for (int i = 0; i < q.size(); i++) if (q.get(i).getId().equals(r.getId())) return i + 1;
        return 0;
    }

    /** If a free copy exists, hold it for the earliest waiting member. */
    private void promoteNextReservation(Book book) {
        long ready = reservations.countByBookAndStatus(book, Reservation.Status.READY);
        long free = book.getAvailable() - ready;
        if (free <= 0) return;
        List<Reservation> waiting = reservations.findByBookAndStatusInOrderByCreatedAtAsc(
                book, List.of(Reservation.Status.WAITING));
        for (int i = 0; i < Math.min(free, waiting.size()); i++) {
            Reservation r = waiting.get(i);
            r.setStatus(Reservation.Status.READY);
            r.setHoldUntil(LocalDate.now().plusDays(HOLD_DAYS));
        }
    }

    /** READY holds not collected in time expire and pass to the next person. */
    public void expireHolds() {
        for (Reservation r : reservations.findByStatusInOrderByCreatedAtAsc(List.of(Reservation.Status.READY))) {
            if (r.getHoldUntil() != null && LocalDate.now().isAfter(r.getHoldUntil())) {
                r.setStatus(Reservation.Status.EXPIRED);
                promoteNextReservation(r.getBook());
            }
        }
    }

    // ------------------------------------------------------------------ fines

    /** Settled fines not yet paid + fines accruing on currently overdue loans. */
    public long outstandingFines(User user) {
        long accruing = issues.findByUserOrderByIssueDateDesc(user).stream()
                .filter(Issue::isOverdue).mapToLong(i -> i.currentFine(finePerDay)).sum();
        return issues.unpaidFines(user) + accruing;
    }

    public void markFinePaid(Long issueId) {
        Issue i = issues.findById(issueId).orElseThrow(() -> new LibraryException("Record not found."));
        if (!i.isReturned()) throw new LibraryException("Book must be returned before the fine can be settled.");
        if (i.getFine() <= 0) throw new LibraryException("No fine on this record.");
        if (i.isFinePaid()) throw new LibraryException("Fine already paid.");
        i.setFinePaid(true);
        i.setFinePaidOn(LocalDate.now());
    }

    public void waiveFine(Long issueId) {
        Issue i = issues.findById(issueId).orElseThrow(() -> new LibraryException("Record not found."));
        i.setFine(0);
    }

    // ------------------------------------------------------------------ books

    public Book saveBook(Book form) {
        String isbn = form.getIsbn().trim();
        Optional<Book> clash = books.findByIsbn(isbn);
        if (clash.isPresent() && !clash.get().getId().equals(form.getId()))
            throw new LibraryException("Another book already uses ISBN " + isbn + ".");

        if (form.getId() == null) {
            form.setIsbn(isbn);
            form.setAvailable(form.getQuantity());
            return books.save(form);
        }
        Book existing = books.findById(form.getId()).orElseThrow(() -> new LibraryException("Book not found."));
        int issued = existing.getIssuedCount();
        if (form.getQuantity() < issued)
            throw new LibraryException("Quantity cannot be less than the " + issued + " copies currently issued.");
        existing.setTitle(form.getTitle().trim());
        existing.setAuthor(form.getAuthor().trim());
        existing.setIsbn(isbn);
        existing.setCategory(form.getCategory().trim());
        existing.setPublishedYear(form.getPublishedYear());
        existing.setDescription(form.getDescription());
        existing.setQuantity(form.getQuantity());
        existing.setAvailable(form.getQuantity() - issued);
        promoteNextReservation(existing);
        return existing;
    }

    public void deleteBook(Long id) {
        Book b = books.findById(id).orElseThrow(() -> new LibraryException("Book not found."));
        if (issues.existsByBookAndReturnDateIsNull(b))
            throw new LibraryException("Cannot delete: copies of this book are still issued.");
        List<Issue> history = issues.findByBook(b);
        if (history.stream().anyMatch(i -> i.getFine() > 0 && !i.isFinePaid()))
            throw new LibraryException("Cannot delete: there are unpaid fines linked to this book.");
        reservations.deleteAll(reservations.findByBook(b));
        issues.deleteAll(history);
        books.delete(b);
    }

    // ---------------------------------------------------------------- members

    public void toggleMember(Long id, User admin) {
        User u = users.findById(id).orElseThrow(() -> new LibraryException("Member not found."));
        if (u.getId().equals(admin.getId())) throw new LibraryException("You cannot disable your own account.");
        u.setEnabled(!u.isEnabled());
    }

    public void deleteMember(Long id, User admin) {
        User u = users.findById(id).orElseThrow(() -> new LibraryException("Member not found."));
        if (u.getId().equals(admin.getId())) throw new LibraryException("You cannot delete your own account.");
        if (issues.existsByUserAndReturnDateIsNull(u))
            throw new LibraryException("Member still has books issued. Process returns first.");
        if (issues.unpaidFines(u) > 0)
            throw new LibraryException("Member has unpaid fines.");
        reservations.deleteAll(reservations.findByUser(u));
        issues.deleteAll(issues.findByUser(u));
        users.delete(u);
    }
}
