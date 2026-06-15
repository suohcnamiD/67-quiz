package dev.six_seven_quiz.quiz.model;


import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "questions")
public class Question {

    public Question() {}
    public Question(
            Quiz quiz,
            String text
    ) {
        this.quiz = quiz;
        this.options = new ArrayList<>();
        this.text = text;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "text", nullable = false)
    private String text;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Option> options;

    public UUID getId() {
        return id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void addOptions(List<Option> newOptions) {
        if (this.options == null) {
            this.options = new ArrayList<>();
        }

        this.options.addAll(newOptions);
    }

    public String getText() {
        return text;
    }
}
