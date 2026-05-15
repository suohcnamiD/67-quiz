package dev.six_seven_quiz.quiz.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

import java.util.UUID;

@Embeddable
public class QuestionSubmissionId {

    @Column(name = "question_id")
    private UUID questionId;

    @Embedded
    private QuizAttemptId quizAttemptId;
}
