package com.example.bankcards.repository;

import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.enums.CardBlockRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardBlockRequestRepository
        extends JpaRepository<CardBlockRequest, Long> {

    boolean existsByCard_IdAndStatus(
            Long cardId,
            CardBlockRequestStatus Status
    );

    Page<CardBlockRequest> findAllByStatus(
            CardBlockRequestStatus Status,
            Pageable pageable
    );
}