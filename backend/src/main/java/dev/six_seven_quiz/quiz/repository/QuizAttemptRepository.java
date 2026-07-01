package dev.six_seven_quiz.quiz.repository;

import dev.six_seven_quiz.quiz.model.Attempt;
import dev.six_seven_quiz.user.ApplicationUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<Attempt, UUID> {
    Page<Attempt> findByUserAndFinishedIsTrue(ApplicationUser user, Pageable pageable);
    Page<Attempt> findByUserAndFinishedIsFalse(ApplicationUser user, Pageable pageable);

    /**
     * In-progress attempts filtered to those still within their deadline —
     * past-deadline rows with {@code finished=false} are excluded from the
     * "active" list so the caller doesn't need to sweep them first. Prevents
     * the concurrent-UPDATE race on the sweep path.
     */
    @Query("SELECT a FROM Attempt a WHERE a.user = :user AND a.finished = false AND a.finishDeadline >= :now")
    Page<Attempt> findActiveByUser(@Param("user") ApplicationUser user, @Param("now") Instant now, Pageable pageable);

    /**
     * Finished OR effectively-finished (past-deadline) attempts. Mirrors the
     * above — no write needed to include stale unfinished rows in the
     * "past results" list.
     */
    @Query("SELECT a FROM Attempt a WHERE a.user = :user AND (a.finished = true OR a.finishDeadline < :now)")
    Page<Attempt> findFinishedOrExpiredByUser(@Param("user") ApplicationUser user, @Param("now") Instant now, Pageable pageable);

    int countByUserAndFinishedIsTrue(ApplicationUser user);

    List<Attempt> findAllByUserAndFinishedIsTrue(ApplicationUser user);

    boolean existsByUser_IdAndQuiz_IdAndFinishedIsTrue(UUID userId, UUID quizId);

    /**
     * Load the attempt with a row-level write lock (SELECT … FOR UPDATE).
     * Used by the explicit finish path so we hold the row until commit —
     * concurrent transactions touching the same row wait rather than
     * tripping MariaDB's "record has changed since last read" (Error 1020)
     * on flush.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Attempt a WHERE a.id = :id")
    Optional<Attempt> findByIdForUpdate(@Param("id") UUID id);
}