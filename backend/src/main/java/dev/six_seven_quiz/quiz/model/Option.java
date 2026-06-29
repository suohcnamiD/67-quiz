package dev.six_seven_quiz.quiz.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "options")
public class Option {

    public Option() {}
    public Option(Question question, String text, Boolean isCorrect) {
        this.question = question;
        this.text = text;
        this.correct = isCorrect;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "correct", nullable = false)
    private Boolean correct;

    @ManyToOne
    @JoinColumn(name = "question_id", referencedColumnName = "id", nullable = false, updatable = false)
    private Question question;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "image_path")
    private String imagePath;

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

    public Boolean isCorrect() {
        return correct;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        Option option = (Option) o;

        return id.equals(option.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


}
