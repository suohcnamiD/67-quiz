package dev.six_seven_quiz.quiz.validator;

import dev.six_seven_quiz.quiz.exception.NoAccessToQuizException;
import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.user.ApplicationUser;

public class QuizValidator {
    public static void requireOwner(Quiz quiz, ApplicationUser currentUser) {
        if (isOwnerOrAdmin(quiz, currentUser)) return;
        throw new NoAccessToQuizException(quiz.getId());
    }

    public static boolean isOwnerOrAdmin(Quiz quiz, ApplicationUser currentUser) {
        if (quiz.getAuthor().equals(currentUser)) return true;
        return isAdmin(currentUser);
    }

    public static boolean isAdmin(ApplicationUser user) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()));
    }
}
