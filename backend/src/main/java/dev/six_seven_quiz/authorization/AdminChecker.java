package dev.six_seven_quiz.authorization;

import dev.six_seven_quiz.user.ApplicationUser;

/**
 * Single source of truth for the "is this user an admin?" question.
 * Both quiz and comment authorisation paths call this so behaviour stays
 * consistent when we tweak the role model later (e.g. add MODERATOR).
 */
public final class AdminChecker {
    private AdminChecker() {}

    public static boolean isAdmin(ApplicationUser user) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()));
    }
}
