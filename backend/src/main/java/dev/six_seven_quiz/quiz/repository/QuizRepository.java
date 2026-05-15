package dev.six_seven_quiz.quiz.repository;

import dev.six_seven_quiz.quiz.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
}
