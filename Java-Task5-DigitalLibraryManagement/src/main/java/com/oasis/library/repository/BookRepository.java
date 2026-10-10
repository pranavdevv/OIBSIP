package com.oasis.library.repository;

import com.oasis.library.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    boolean existsByIsbn(String isbn);
    Optional<Book> findByIsbn(String isbn);

    @Query("select distinct b.category from Book b order by b.category")
    List<String> findCategories();

    @Query("""
           select b from Book b
           where (:category is null or b.category = :category)
             and (:q is null or lower(b.title) like lower(concat('%', :q, '%'))
                             or lower(b.author) like lower(concat('%', :q, '%'))
                             or b.isbn like concat('%', :q, '%'))
           order by b.title
           """)
    List<Book> search(@Param("category") String category, @Param("q") String q);

    @Query("select coalesce(sum(b.quantity),0) from Book b")
    long totalCopies();
}

