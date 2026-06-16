package dev.six_seven_quiz.quiz.repository;

import dev.six_seven_quiz.quiz.model.AttemptQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttemptQuestionRepository extends JpaRepository<AttemptQuestion, UUID> {
    int countByAttempt_Id(UUID attemptId);
}