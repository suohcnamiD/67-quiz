package dev.six_seven_quiz.quiz.repository;

import dev.six_seven_quiz.quiz.model.Attempt;
import dev.six_seven_quiz.user.ApplicationUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<Attempt, UUID> {
    Page<Attempt> findByUserAndFinishedIsTrue(ApplicationUser user, Pageable pageable);
    Page<Attempt> findByUserAndFinishedIsFalse(ApplicationUser user, Pageable pageable);

    List<Attempt> findByUser_IdAndFinishedIsFalseAndFinishDeadlineBefore(UUID userId, LocalDateTime finishDeadline);
}