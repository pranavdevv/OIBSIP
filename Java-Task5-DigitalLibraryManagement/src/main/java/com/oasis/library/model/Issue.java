package com.oasis.library.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** A loan of one copy of a book to a member. */
@Entity
@Table(name = "issues")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Book book;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate returnDate;

    /** Final fine, fixed at return time. */
    private Integer fine = 0;

    private boolean finePaid = false;

    private LocalDate finePaidOn;

    public Issue() {}

    public Issue(User user, Book book, LocalDate issueDate, LocalDate dueDate) {
        this.user = user;
        this.book = book;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Book getBook() { return book; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public Integer getFine() { return fine; }
    public void setFine(Integer fine) { this.fine = fine; }
    public boolean isFinePaid() { return finePaid; }
    public void setFinePaid(boolean finePaid) { this.finePaid = finePaid; }
    public LocalDate getFinePaidOn() { return finePaidOn; }
    public void setFinePaidOn(LocalDate finePaidOn) { this.finePaidOn = finePaidOn; }

    public boolean isReturned() { return returnDate != null; }

    public boolean isOverdue() {
        return !isReturned() && LocalDate.now().isAfter(dueDate);
    }

    public long getDaysOverdue() {
        LocalDate end = isReturned() ? returnDate : LocalDate.now();
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, end));
    }

    public long getDaysLeft() {
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    /** 0-100: how much of the loan period has elapsed (for the progress bar). */
    public int getLoanProgress() {
        long total = Math.max(1, ChronoUnit.DAYS.between(issueDate, dueDate));
        long used = ChronoUnit.DAYS.between(issueDate, LocalDate.now());
        return (int) Math.min(100, Math.max(0, used * 100 / total));
    }

    /** Fine accrued so far for an open loan, or the final fine for a returned one. */
    public int currentFine(int perDay) {
        return isReturned() ? fine : (int) getDaysOverdue() * perDay;
    }

    public String getStatus() {
        if (isReturned()) return "RETURNED";
        return isOverdue() ? "OVERDUE" : "ISSUED";
    }
}
