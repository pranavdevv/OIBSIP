package com.oasis.library.repository;

import com.oasis.library.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserOrderByCreatedAtDesc(User user);
    List<Reservation> findByBookAndStatusInOrderByCreatedAtAsc(Book book, List<Reservation.Status> statuses);
    List<Reservation> findByStatusInOrderByCreatedAtAsc(List<Reservation.Status> statuses);
    Optional<Reservation> findFirstByUserAndBookAndStatusIn(User user, Book book, List<Reservation.Status> statuses);
    long countByBookAndStatus(Book book, Reservation.Status status);
    long countByStatusIn(List<Reservation.Status> statuses);
    List<Reservation> findByBook(Book book);
    List<Reservation> findByUser(User user);
}

