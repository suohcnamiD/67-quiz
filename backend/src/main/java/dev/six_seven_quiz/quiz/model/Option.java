package dev.six_seven_quiz.quiz.model;

import jakarta.persistence.*;

@Entity
@Table(name = "options")
public class Option {

    @EmbeddedId
    private OptionId id;

    @Column(name = "text", nullable = false)
    private String text;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    @MapsId("quizId")
    private Quiz quiz;

    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "quiz_id", referencedColumnName = "quiz_id", insertable = false, updatable = false),
            @JoinColumn(name = "question_id", referencedColumnName = "question_id", insertable = false, updatable = false)
    })
    private Question question;

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }


    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }
}
