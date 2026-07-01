package dev.six_seven_quiz.quiz.repository;

import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.user.ApplicationUser;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    @NonNull
    Page<Quiz> findAll(@NonNull Pageable pageable);

    int countByAuthor(ApplicationUser author);

    Page<Quiz> findByAuthorOrderByNameAsc(ApplicationUser author, Pageable pageable);

    Page<Quiz> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);

    /**
     * Rating-ordered listing: pinned first, then by average rating desc
     * (unrated quizzes sort last via COALESCE to -1). Name is the final
     * tie-breaker so the order is stable across reloads.
     */
    @Query("""
        SELECT q FROM Quiz q
        LEFT JOIN dev.six_seven_quiz.quiz.model.QuizRating r ON r.quiz = q
        GROUP BY q
        ORDER BY q.pinned DESC, COALESCE(AVG(r.score), -1) DESC, q.name ASC
        """)
    Page<Quiz> findAllOrderByRating(Pageable pageable);
}
