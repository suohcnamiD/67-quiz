package dev.six_seven_quiz.quiz.model;

import dev.six_seven_quiz.user.ApplicationUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    public QuizAttempt() {}
    public QuizAttempt(ApplicationUser user, Quiz quiz) {
        this.user = user;
        this.quiz = quiz;
        this.startedAt = LocalDateTime.now();
        this.questionSubmissions = new ArrayList<>();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "finished", nullable = false)
    private boolean finished = false;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private ApplicationUser user;

    @ManyToOne
    @JoinColumn(name = "quiz_id", referencedColumnName = "id")
    private Quiz quiz;

    @Column(name = "start_time")
    private LocalDateTime startedAt;

    @OneToMany(mappedBy = "attempt")
    private List<QuestionSubmission> questionSubmissions;


    public UUID getId() {
        return id;
    }

    public ApplicationUser getUser() {
        return user;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public List<QuestionSubmission> getQuestionSubmissions() {
        return questionSubmissions;
    }

    public boolean isFinished() {
        return finished;
    }

    public void finish() {
        this.finished = true;
    }
}
