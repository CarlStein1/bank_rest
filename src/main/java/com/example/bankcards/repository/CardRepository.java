package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository
        extends JpaRepository<Card, Long> {

    Page<Card> findAllByUser_Id(
            Long userId,
            Pageable pageable
    );

    Page<Card> findAllByUser_IdAndNumberLastFour(
            Long userId,
            String numberLastFour,
            Pageable pageable
    );

    Page<Card> findAllByUser_IdAndStatus(
            Long userId,
            CardStatus status,
            Pageable pageable
    );

    Page<Card> findAllByUser_IdAndNumberLastFourAndStatus(
            Long userId,
            String numberLastFour,
            CardStatus status,
            Pageable pageable
    );

    Optional<Card> findByIdAndUser_Id(
            Long cardId,
            Long userId
    );
}