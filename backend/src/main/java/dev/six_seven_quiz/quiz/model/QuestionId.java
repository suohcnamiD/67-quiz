package dev.six_seven_quiz.quiz.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import java.util.UUID;

@Embeddable
public class QuestionId {

    @Column(name = "quiz_id")
    private UUID quizId;

    @Column(name = "question_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID questionId;
}
