package dev.six_seven_quiz.quiz.model;

import dev.six_seven_quiz.quiz.exception.AttemptFinishedException;
import dev.six_seven_quiz.quiz.exception.OptionNotFoundException;
import jakarta.persistence.*;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "attempt_questions")
public class AttemptQuestion {

    private transient Map<UUID, Option> optionsById;

    public AttemptQuestion() {}
    public AttemptQuestion(
        Question question
    ) {
        this.question = question;
    }

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "answered", nullable = false)
    private Boolean answered = false;

    @ManyToMany
    @JoinTable(
            name = "attempt_question_selected_options",
            joinColumns = @JoinColumn(name = "attempt_question_id"),
            inverseJoinColumns = @JoinColumn(name = "option_id")
    )
    private List<Option> selectedOptions;

    public List<Option> getSelectedOptions() {
        return selectedOptions;
    }

    public List<Option> getOptions() {
        return question.getOptions();
    }

    public int getCorrectlySelectedOptionCount() {
        return (int) selectedOptions.stream().filter(Option::isCorrect).count();
    }

    public Question getQuestion() {
        return question;
    }

    public Boolean isAnswered() {
        return answered;
    }

    public UUID getId() {
        return id;
    }

    private Optional<Option> findOption(UUID id) {
        if (optionsById == null) optionsById = question.getOptions().stream().collect(Collectors.toMap(Option::getId, option -> option));
        return Optional.ofNullable(optionsById.get(id));
    }

    public void selectOption(UUID optionId) {
        Option option = findOption(optionId).orElseThrow(() -> new OptionNotFoundException(optionId));
        if (!selectedOptions.contains(option)) selectedOptions.add(option);
        this.answered = true;
    }

    public void deselectOption(UUID optionId) {
        Option option = findOption(optionId).orElseThrow(() -> new OptionNotFoundException(optionId));
        selectedOptions.remove(option);
        this.answered = true;
    }
}
