package dev.six_seven_quiz.quiz.repository;

import dev.six_seven_quiz.quiz.model.Attempt;
import dev.six_seven_quiz.user.ApplicationUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<Attempt, UUID> {
    Page<Attempt> findByUserAndFinishedIsTrue(ApplicationUser user, Pageable pageable);
    Page<Attempt> findByUserAndFinishedIsFalse(ApplicationUser user, Pageable pageable);

    int countByUserAndFinishedIsTrue(ApplicationUser user);

    List<Attempt> findAllByUserAndFinishedIsTrue(ApplicationUser user);

    boolean existsByUser_IdAndQuiz_IdAndFinishedIsTrue(UUID userId, UUID quizId);

    /**
     * Bulk-flip past-deadline unfinished attempts for a user to finished.
     * Conditional on {@code finished = false} so concurrent finishers (the
     * explicit finish endpoint, polling list calls) don't race each other —
     * MariaDB's snapshot isolation kicks back a "record has changed" error
     * when two transactions both load + update the same row, which is what
     * we used to do via in-memory mutation.
     */
    @Modifying
    @Query("UPDATE Attempt a SET a.finished = true " +
            "WHERE a.user.id = :userId AND a.finished = false AND a.finishDeadline < :now")
    int markPastDeadlineFinished(@Param("userId") UUID userId, @Param("now") Instant now);
}