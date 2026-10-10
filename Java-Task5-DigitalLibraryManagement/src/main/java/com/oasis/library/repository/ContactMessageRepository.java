package com.oasis.library.repository;

import com.oasis.library.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    List<ContactMessage> findAllByOrderByResolvedAscCreatedAtDesc();
    List<ContactMessage> findByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
    long countByResolvedFalse();
}

