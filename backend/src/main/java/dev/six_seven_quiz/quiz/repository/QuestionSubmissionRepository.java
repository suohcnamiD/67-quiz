package dev.six_seven_quiz.quiz.repository;

import dev.six_seven_quiz.quiz.model.QuestionSubmission;
import dev.six_seven_quiz.quiz.model.QuestionSubmissionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionSubmissionRepository extends JpaRepository<QuestionSubmission, QuestionSubmissionId> {
}