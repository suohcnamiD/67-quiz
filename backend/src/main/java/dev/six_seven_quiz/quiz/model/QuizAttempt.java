package dev.six_seven_quiz.quiz.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {


    @EmbeddedId
    private QuizAttemptId id;

    @OneToMany(mappedBy = "attemptId")
    private List<QuestionSubmission> questionSubmissions;
}
