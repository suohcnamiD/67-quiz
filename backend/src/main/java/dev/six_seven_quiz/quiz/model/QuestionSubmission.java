package dev.six_seven_quiz.quiz.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "question_submissions")
public class QuestionSubmission {

    public QuestionSubmission() {}
    public QuestionSubmission(QuizAttempt attempt, Question question, List<Option> selectedOptions) {
        this.attempt = attempt;
        this.question = question;
        this.selectedOptions = selectedOptions;
        this.id = new QuestionSubmissionId(attempt.getId(), question.getId());
    }

    @EmbeddedId
    private QuestionSubmissionId id;

    @ManyToOne
    @JoinColumn(name = "question_id", referencedColumnName = "id")
    @MapsId("questionId")
    private Question question;

    @ManyToOne
    @JoinColumn(name = "attempt_id", referencedColumnName = "id")
    @MapsId("quizAttemptId")
    private QuizAttempt attempt;

    @ManyToMany
    @JoinTable(
            name = "submission_selected_options",
            joinColumns = {
                    @JoinColumn(name = "attempt_id", referencedColumnName = "attempt_id"),
                    @JoinColumn(name = "question_id", referencedColumnName = "question_id")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "option_id", referencedColumnName = "id")
            }
    )
    private List<Option> selectedOptions;


    public Question getQuestion() {
        return question;
    }

    public QuizAttempt getAttempt() {
        return attempt;
    }

    public List<Option> getSelectedOptions() {
        return selectedOptions;
    }


}
