package dev.six_seven_quiz.quiz.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "options")
public class Option {

    public Option() {}
    public Option(Question question, String text) {
        this.question = question;
        this.text = text;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false, insertable = false, updatable = false)
    private Question question;

    @Column(name = "text", nullable = false)
    private String text;

    public void setText(String text) {
        this.text = text;
    }

    public UUID getId() {
        return id;
    }

    public Question getQuestion() {
        return question;
    }

    public String getText() {
        return text;
    }


}
