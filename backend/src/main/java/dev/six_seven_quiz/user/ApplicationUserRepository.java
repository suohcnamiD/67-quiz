package dev.six_seven_quiz.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, UUID> {
    Optional<ApplicationUser> findByUsername(String username);

    Page<ApplicationUser> findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String usernameNeedle,
            String displayNameNeedle,
            Pageable pageable
    );

    /**
     * Authors who have at least {@code minRatings} ratings across all the
     * quizzes they own, ordered by average rating descending. Returned as
     * raw rows (userId, avgRating, ratingCount, quizCount) so the service
     * can hydrate the {@link ApplicationUser} and assemble a leaderboard
     * entry without N+1 fetches.
     */
    @Query("""
            SELECT u.id, AVG(r.score), COUNT(r), COUNT(DISTINCT q.quizId)
            FROM Quiz q
            JOIN q.author u
            JOIN QuizRating r ON r.quiz = q
            GROUP BY u.id
            HAVING COUNT(r) >= :minRatings
            ORDER BY AVG(r.score) DESC, COUNT(r) DESC
            """)
    List<Object[]> findAuthorRankings(@Param("minRatings") long minRatings);

    /**
     * IDs of users with at least one finished attempt. The actual averaging
     * happens in the service because {@code Attempt.maximumScore} is
     * computed from the quiz's questions in code, not stored on the row.
     */
    @Query("""
            SELECT DISTINCT a.user.id
            FROM Attempt a
            WHERE a.finished = TRUE
            """)
    List<UUID> findUserIdsWithFinishedAttempts();
}
