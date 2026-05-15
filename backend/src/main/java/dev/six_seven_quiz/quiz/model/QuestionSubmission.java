package dev.six_seven_quiz.quiz.model;

import jakarta.persistence.*;
import dev.six_seven_quiz.user.ApplicationUser;

import java.util.List;

@Entity
@Table(name = "question_submissions")
public class QuestionSubmission {

    @EmbeddedId
    private QuestionSubmissionId id;

    @ManyToOne
    @JoinColumn(name = "question_id", referencedColumnName = "question_id")
    @MapsId("questionId")
    private Question question;

    @ManyToOne
    @JoinColumn(name = "quiz_id", referencedColumnName = "quiz_id")
    @MapsId("quizId")
    private Quiz quiz;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    @MapsId("userId")
    private ApplicationUser user;

    @ManyToOne
    @JoinColumn(name = "attempt_id", referencedColumnName = "attempt_id")
    @MapsId("attemptId")
    private QuizAttempt attempt;



    @ManyToMany
    @JoinTable(
            name = "submission_selected_options",
            joinColumns = {
                    @JoinColumn(name = "quiz_id", referencedColumnName = "quiz_id"),
                    @JoinColumn(name = "question_id", referencedColumnName = "question_id"),
                    @JoinColumn(name = "attempt_id", referencedColumnName = "attempt_id"),
                    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "quiz_id", referencedColumnName = "quiz_id", insertable = false, updatable = false),
                    @JoinColumn(name = "question_id", referencedColumnName = "question_id", insertable = false, updatable = false),
                    @JoinColumn(name = "option_id", referencedColumnName = "option_id")
            }
    )
    private List<Option> selectedOptions;




    public Question getQuestion() {
        return question;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public ApplicationUser getUser() {
        return user;
    }

    public QuizAttempt getAttempt() {
        return attempt;
    }

    public List<Option> getSelectedOptions() {
        return selectedOptions;
    }


}
