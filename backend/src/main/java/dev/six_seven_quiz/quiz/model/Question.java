package dev.six_seven_quiz.quiz.model;


import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "questions")
public class Question {

    @EmbeddedId
    private QuestionId id;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    @MapsId("quizId")
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
                    @JoinColumn(name = "question_id", referencedColumnName = "question_id")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "quiz_id", referencedColumnName = "quiz_id", insertable = false, updatable = false),
                    @JoinColumn(name = "option_id", referencedColumnName = "option_id")
            }
    )
    private List<Option> correctOptions;

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }
}
