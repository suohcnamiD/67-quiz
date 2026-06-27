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
            String text,
            QuestionType type
    ) {
        this.quiz = quiz;
        this.options = new ArrayList<>();
        this.text = text;
        this.type = type;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "text", nullable = false)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private QuestionType type = QuestionType.MULTI_CHOICE;

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

    /**
     * Replace the entire options list. Used by the edit-question flow.
     * orphanRemoval on the @OneToMany will delete the rows from the previous
     * list once the parent is saved.
     */
    public void replaceOptions(List<Option> newOptions) {
        if (this.options == null) {
            this.options = new ArrayList<>();
        }
        this.options.clear();
        this.options.addAll(newOptions);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }
}
