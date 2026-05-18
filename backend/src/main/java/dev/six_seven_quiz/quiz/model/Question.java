package dev.six_seven_quiz.quiz.model;


import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "questions")
public class Question {

    public Question() {}
    public Question(
            Quiz quiz,
            List<Option> options
    ) {
        this.quiz = quiz;
        this.options = new ArrayList<>(options);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @OneToOne
    @JoinColumn(name = "next_question_id")
    private Question nextQuestion;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Option> options;

    @ManyToMany
    @JoinTable(
            name = "question_correct_options",
            joinColumns = {
                    @JoinColumn(name = "quiz_id", referencedColumnName = "quiz_id"),
                    @JoinColumn(name = "question_id", referencedColumnName = "id")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "option_id", referencedColumnName = "id")
            }
    )
    private List<Option> correctOptions;

    public UUID getId() {
        return id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public Question getNextQuestion() {
        return nextQuestion;
    }

    public List<Option> getOptions() {
        return options;
    }

    public List<Option> getCorrectOptions() {
        return correctOptions;
    }
}
