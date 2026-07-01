package dev.six_seven_quiz.quiz.dto.request;

/**
 * Ordering options for GET /quiz. Pinned quizzes always float to the top
 * regardless of the selected sort — this enum only controls the secondary
 * order within the unpinned bucket.
 */
public enum QuizSort {
    /** Alphabetical by name (case-insensitive). Default. */
    NAME,
    /** Newest first — quizzes.created_at DESC. */
    NEWEST,
    /** Highest average rating first; unrated quizzes sort last. */
    RATING
}
