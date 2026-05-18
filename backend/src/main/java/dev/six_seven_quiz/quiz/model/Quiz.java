package dev.six_seven_quiz.quiz.model;


import jakarta.persistence.*;
import dev.six_seven_quiz.user.ApplicationUser;

import java.util.UUID;

@Entity
@Table(name = "quizzes")
public class Quiz {

    public Quiz() {}

    public Quiz(String name, ApplicationUser author) {
        this.name = name;
        this.author = author;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID quizId;

    @OneToOne
    @JoinColumn(name = "first_question_id")
    private Question firstQuestion;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private ApplicationUser author;

    @Column(name = "name")
    private String name;


    public Question getFirstQuestion() {
        return firstQuestion;
    }

    public void setFirstQuestion(Question firstQuestion) {
        this.firstQuestion = firstQuestion;
    }

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
}
