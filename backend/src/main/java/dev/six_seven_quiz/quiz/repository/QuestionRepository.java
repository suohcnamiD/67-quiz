package dev.six_seven_quiz.quiz.repository;

import dev.six_seven_quiz.quiz.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    Optional<Question> getByQuiz_quizIdAndNextQuestionIsNull(UUID quizId);
}
