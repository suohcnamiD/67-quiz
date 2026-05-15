package dev.six_seven_quiz.quiz.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import java.util.UUID;

@Embeddable
public class QuizAttemptId {

    @Column(name = "quiz_id")
    private UUID quizId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "attempt_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID attemptId;
}
