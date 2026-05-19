package dev.six_seven_quiz.quiz.repository;

import dev.six_seven_quiz.quiz.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
}