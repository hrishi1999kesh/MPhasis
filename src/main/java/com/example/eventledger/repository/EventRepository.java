package com.example.eventledger.repository;

import com.example.eventledger.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEventId(String eventId);

    List<Event> findByAccountIdOrderByEventTimestampAsc(String accountId);

    @Query(value = """
            SELECT COALESCE(
                SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END),
                0
            )
            FROM events
            WHERE account_id = :accountId
            """, nativeQuery = true)
    BigDecimal calculateBalance(String accountId);
}