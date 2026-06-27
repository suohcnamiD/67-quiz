package dev.six_seven_quiz.quiz.model;

import dev.six_seven_quiz.quiz.dto.response.viewing.FinishedOptionDto;
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

    @ManyToOne
    @JoinColumn(name = "attempt_id", insertable = false, updatable = false)
    private Attempt attempt;

    @ManyToMany(fetch = FetchType.EAGER)
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

    public List<FinishedOptionDto> getFinishedOptions() {
        return getOptions().stream().map(option -> new FinishedOptionDto(
            option.getId(),
            option.getText(),
            option.isCorrect(),
            selectedOptions.contains(option)
        )).toList();
    }

    public String getText() {
        return getQuestion().getText();
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

    public Attempt getAttempt() {
        return attempt;
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

    public void clearSelections() {
        selectedOptions.clear();
    }

    public QuestionType getType() {
        return question.getType();
    }

    /**
     * Score the user earned on this question. For multi-choice this is the
     * per-option count of correctly classified picks (the existing rule).
     * For single-choice it's 1 iff the unique correct option is among the
     * user's picks, else 0.
     */
    public int getEarnedScore() {
        if (getType() == QuestionType.SINGLE_CHOICE) {
            return question.getOptions().stream()
                    .filter(Option::isCorrect)
                    .anyMatch(selectedOptions::contains) ? 1 : 0;
        }
        int earned = 0;
        for (Option option : question.getOptions()) {
            if (option.isCorrect() == selectedOptions.contains(option)) earned++;
        }
        return earned;
    }

    /**
     * Maximum score this question can yield. Multi-choice = option count
     * (each option scored independently); single-choice = 1.
     */
    public int getMaximumScore() {
        return getType() == QuestionType.SINGLE_CHOICE ? 1 : question.getOptions().size();
    }
}
