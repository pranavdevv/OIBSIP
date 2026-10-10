package com.oasis.library.repository;

import com.oasis.library.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByUserOrderByIssueDateDesc(User user);
    List<Issue> findByReturnDateIsNullOrderByDueDateAsc();
    List<Issue> findAllByOrderByIssueDateDesc();
    long countByUserAndReturnDateIsNull(User user);
    boolean existsByUserAndBookAndReturnDateIsNull(User user, Book book);
    boolean existsByBook(Book book);
    boolean existsByBookAndReturnDateIsNull(Book book);
    boolean existsByUserAndReturnDateIsNull(User user);
    List<Issue> findByUser(User user);
    List<Issue> findByBookAndReturnDateIsNull(Book book);
    List<Issue> findByBook(Book book);

    @Query("select i from Issue i where i.fine > 0 order by i.finePaid asc, i.returnDate desc")
    List<Issue> findWithFines();

    @Query("select coalesce(sum(i.fine),0) from Issue i where i.user = :u and i.fine > 0 and i.finePaid = false")
    long unpaidFines(@Param("u") User u);

    @Query("select coalesce(sum(i.fine),0) from Issue i where i.fine > 0 and i.finePaid = true")
    long collectedFines();

    long countByReturnDateIsNull();
}


