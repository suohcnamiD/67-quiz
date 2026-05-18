package dev.six_seven_quiz.quiz.model;

import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class QuestionSubmissionId {

    public QuestionSubmissionId() {}
    public QuestionSubmissionId(
        UUID quizAttemptId,
        UUID questionId
    ) {
        this.quizAttemptId = quizAttemptId;
        this.questionId = questionId;
    }

    private UUID quizAttemptId;
    private UUID questionId;

}
