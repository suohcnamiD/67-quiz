package dev.six_seven_quiz.quiz.model;


import dev.six_seven_quiz.quiz.dto.request.OptionData;
import dev.six_seven_quiz.quiz.dto.QuestionData;
import dev.six_seven_quiz.quiz.exception.BlankIndexedOptionException;
import dev.six_seven_quiz.shared.component.Utilities;
import jakarta.persistence.*;
import dev.six_seven_quiz.user.ApplicationUser;

import java.time.Duration;
import java.time.LocalDateTime;
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

    @Column(name = "duration")
    private Duration duration;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
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

    public List<Question> getQuestions() {
        return questions;
    }

    public Attempt start(ApplicationUser user) {
        return new Attempt(user, this, LocalDateTime.now().plus(this.duration));
    }

    public void addQuestion(QuestionData questionData) {
        Question question = new Question(this, questionData.text());

        int i = 0;

        for (OptionData optionData : questionData.options()) {
            if (optionData.text().isBlank()) throw new BlankIndexedOptionException(i);
            i++;
        }

        List<Option> createdOptions = new ArrayList<>();
        for (OptionData optionData : questionData.options()) {
            Option rawOption = new Option(question, optionData.text(), optionData.correct());
            createdOptions.add(rawOption);
        }
        question.addOptions(createdOptions);
        this.questions.add(question);
    }

    public Duration getDuration() {
        return duration;
    }
}
