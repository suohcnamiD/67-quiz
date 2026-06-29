package dev.six_seven_quiz.quiz.model;

import dev.six_seven_quiz.user.ApplicationUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_ratings")
public class QuizRating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private ApplicationUser user;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected QuizRating() {}

    public QuizRating(Quiz quiz, ApplicationUser user, int score, String comment) {
        this.quiz = quiz;
        this.user = user;
        this.score = score;
        this.comment = comment;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(int score, String comment) {
        this.score = score;
        this.comment = comment;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public ApplicationUser getUser() { return user; }
    public int getScore() { return score; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
