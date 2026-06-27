package dev.six_seven_quiz.quiz.repository;

import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.user.ApplicationUser;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    @NonNull
    Page<Quiz> findAll(@NonNull Pageable pageable);

    int countByAuthor(ApplicationUser author);

    Page<Quiz> findByAuthorOrderByNameAsc(ApplicationUser author, Pageable pageable);

    Page<Quiz> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);
}
