package dev.six_seven_quiz.quiz.model;

import dev.six_seven_quiz.quiz.exception.AttemptFinishedException;
import dev.six_seven_quiz.quiz.exception.QuestionNotFoundException;
import dev.six_seven_quiz.user.ApplicationUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Entity
@Table(name = "quiz_attempts")
public class Attempt {

    public Attempt() {}
    public Attempt(ApplicationUser user, Quiz quiz, Instant finishDeadline) {
        this.user = user;
        this.quiz = quiz;
        this.startedAt = Instant.now();
        this.questions = new ArrayList<>(quiz.getQuestions().stream().map(AttemptQuestion::new).toList());
        this.finishDeadline = finishDeadline;
    }

    private transient Map<UUID, AttemptQuestion> questionsById;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "finished", nullable = false)
    private boolean finished = false;

    @Column(name = "finish_deadline")
    private Instant finishDeadline;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private ApplicationUser user;

    @ManyToOne
    @JoinColumn(name = "quiz_id", referencedColumnName = "id")
    private Quiz quiz;

    @Column(name = "start_time")
    private Instant startedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "attempt_id", nullable = false)
    @OrderColumn(name = "position", nullable = false)
    private List<AttemptQuestion> questions;

    public ApplicationUser getUser() {
        return user;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public List<AttemptQuestion> getQuestions() {
        return questions;
    }

    public boolean isFinished() {
        return finished;
    }

    public void finish() {
        this.finished = true;
    }

    public int getMaximumScore() {
        return questions.stream().mapToInt(AttemptQuestion::getMaximumScore).sum();
    }

    public int getEarnedScore() {
        return questions.stream().mapToInt(AttemptQuestion::getEarnedScore).sum();
    }

    private Optional<AttemptQuestion> findQuestion(UUID questionId) {
        if (questionsById == null) {
            questionsById = questions.stream()
                    .collect(Collectors.toMap(AttemptQuestion::getId, question -> question));
        }
        return Optional.ofNullable(questionsById.get(questionId));
    }

    public void selectOption(UUID questionId, UUID optionId) {
        AttemptQuestion question = findQuestion(questionId).orElseThrow(() -> new QuestionNotFoundException(questionId));
        // Single-choice questions enforce at most one selection at the model
        // layer so a misbehaving client can't accumulate picks.
        if (question.getType() == QuestionType.SINGLE_CHOICE) {
            question.clearSelections();
        }
        question.selectOption(optionId);
    }

    public void deselectOption(UUID questionId, UUID optionId) {
        AttemptQuestion question = findQuestion(questionId).orElseThrow(() -> new QuestionNotFoundException(questionId));
        question.deselectOption(optionId);
    }

    public UUID getId() {
        return id;
    }

    public Instant getFinishDeadline() {
        return finishDeadline;
    }
}
