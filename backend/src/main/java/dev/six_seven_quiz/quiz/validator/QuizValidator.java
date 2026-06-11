package dev.six_seven_quiz.quiz.validator;

import dev.six_seven_quiz.quiz.exception.NoAccessToQuizException;
import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.user.ApplicationUser;

public class QuizValidator {
    public static void requireOwner(Quiz quiz, ApplicationUser currentUser) {
        if (quiz.getAuthor().equals(currentUser)) return;
        throw new NoAccessToQuizException(quiz.getId());
    }
}
