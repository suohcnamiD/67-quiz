package dev.six_seven_quiz.quiz.model;


import dev.six_seven_quiz.quiz.dto.request.OptionData;
import dev.six_seven_quiz.quiz.dto.QuestionData;
import dev.six_seven_quiz.quiz.exception.BlankIndexedOptionException;
import dev.six_seven_quiz.quiz.exception.InvalidQuestionShapeException;
import dev.six_seven_quiz.shared.component.Utilities;
import jakarta.persistence.*;
import dev.six_seven_quiz.user.ApplicationUser;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quizzes")
public class Quiz {

    public Quiz() {}

    public Quiz(String name, ApplicationUser author, Duration duration) {
        this.name = name;
        this.author = author;
        this.duration = duration;
        this.createdAt = Instant.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID quizId;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private ApplicationUser author;

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration")
    private Duration duration;

    @Column(name = "cover_image_path")
    private String coverImagePath;

    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE})
    @JoinTable(
            name = "quiz_questions",
            joinColumns = @JoinColumn(name = "quiz_id"),
            inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    @OrderColumn(name = "position")
    private List<Question> questions;

    public ApplicationUser getAuthor() {
        return author;
    }

    public void setAuthor(ApplicationUser author) {
        this.author = author;
    }

    public UUID getId() {
        return quizId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public Attempt start(ApplicationUser user) {
        return new Attempt(user, this, Instant.now().plus(this.duration));
    }

    public int getMaximumScore() {
        return questions.stream()
                .mapToInt(question ->
                        question.getType() == QuestionType.SINGLE_CHOICE
                                ? 1
                                : question.getOptions().size())
                .sum();
    }

    public void addQuestion(QuestionData questionData) {
        validateQuestionShape(questionData);

        Question question = new Question(this, questionData.text(), questionData.type());
        List<Option> createdOptions = optionsFromData(question, questionData);
        question.addOptions(createdOptions);
        this.questions.add(question);
    }

    /**
     * Apply an edit to an existing question that already belongs to this
     * quiz. Mirrors {@link #addQuestion(QuestionData)} for validation so
     * single-choice "exactly one correct" + ">=2 options" rules apply both
     * on create and edit.
     */
    public void editQuestion(Question question, QuestionData questionData) {
        validateQuestionShape(questionData);

        question.setText(questionData.text());
        question.setType(questionData.type());
        question.replaceOptions(optionsFromData(question, questionData));
    }

    private static void validateQuestionShape(QuestionData questionData) {
        if (questionData.type() == null) {
            throw new InvalidQuestionShapeException("Question type is required");
        }
        List<OptionData> options = questionData.options();
        if (options == null || options.size() < 2) {
            throw new InvalidQuestionShapeException("A question needs at least two options");
        }
        int i = 0;
        long correctCount = 0;
        for (OptionData optionData : options) {
            if (optionData.text() == null || optionData.text().isBlank()) {
                throw new BlankIndexedOptionException(i);
            }
            if (optionData.correct()) correctCount++;
            i++;
        }
        if (questionData.type() == QuestionType.SINGLE_CHOICE && correctCount != 1) {
            throw new InvalidQuestionShapeException(
                    "Single-choice questions need exactly one correct option (got " + correctCount + ")"
            );
        }
    }

    private static List<Option> optionsFromData(Question owner, QuestionData questionData) {
        List<Option> created = new ArrayList<>();
        for (OptionData optionData : questionData.options()) {
            created.add(new Option(owner, optionData.text(), optionData.correct()));
        }
        return created;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getCoverImagePath() {
        return coverImagePath;
    }

    public void setCoverImagePath(String coverImagePath) {
        this.coverImagePath = coverImagePath;
    }
}
