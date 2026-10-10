package com.oasis.library.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Advance booking of a book that is currently out of stock.
 * WAITING -> READY (a copy came back and is held for this user) -> FULFILLED (user borrowed it).
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    public enum Status { WAITING, READY, FULFILLED, CANCELLED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Book book;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private Status status = Status.WAITING;

    /** Last day a READY copy is held for the user. */
    private LocalDate holdUntil;

    public Reservation() {}

    public Reservation(User user, Book book) {
        this.user = user;
        this.book = book;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Book getBook() { return book; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDate getHoldUntil() { return holdUntil; }
    public void setHoldUntil(LocalDate holdUntil) { this.holdUntil = holdUntil; }
    public boolean isActive() { return status == Status.WAITING || status == Status.READY; }
}
