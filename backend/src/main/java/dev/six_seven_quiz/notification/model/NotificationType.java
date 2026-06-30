package dev.six_seven_quiz.notification.model;

public enum NotificationType {
    /** Someone left a comment on the recipient's profile. */
    COMMENT_RECEIVED,
    /** Someone rated a quiz the recipient authored. */
    QUIZ_RATED,
    /** Someone finished an attempt of a quiz the recipient authored. */
    QUIZ_ATTEMPTED,
    /** The recipient's rank on a leaderboard worsened since the last snapshot. */
    RANK_DROPPED,
}
