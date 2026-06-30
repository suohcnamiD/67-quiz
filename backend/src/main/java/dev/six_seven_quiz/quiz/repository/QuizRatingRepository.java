package dev.six_seven_quiz.quiz.repository;

import dev.six_seven_quiz.quiz.model.QuizRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface QuizRatingRepository extends JpaRepository<QuizRating, UUID> {

    Optional<QuizRating> findByQuiz_IdAndUser_Id(UUID quizId, UUID userId);

    Page<QuizRating> findByQuiz_IdOrderByCreatedAtDesc(UUID quizId, Pageable pageable);

    @Query("SELECT AVG(r.score) FROM QuizRating r WHERE r.quiz.id = :quizId")
    Optional<Double> averageScoreForQuiz(@Param("quizId") UUID quizId);

    @Query("SELECT COUNT(r) FROM QuizRating r WHERE r.quiz.id = :quizId")
    long countForQuiz(@Param("quizId") UUID quizId);
}
