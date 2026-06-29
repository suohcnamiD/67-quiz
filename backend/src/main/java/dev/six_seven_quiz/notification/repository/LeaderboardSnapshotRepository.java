package dev.six_seven_quiz.notification.repository;

import dev.six_seven_quiz.notification.model.LeaderboardBoard;
import dev.six_seven_quiz.notification.model.LeaderboardSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LeaderboardSnapshotRepository extends JpaRepository<LeaderboardSnapshot, UUID> {

    /**
     * Latest snapshot the job has recorded for a user on a given board, used
     * as the "previous" value when comparing against the new rank.
     */
    @Query("""
            SELECT s FROM LeaderboardSnapshot s
            WHERE s.user.id = :userId AND s.board = :board
            ORDER BY s.snapshotAt DESC
            LIMIT 1
            """)
    Optional<LeaderboardSnapshot> findLatest(@Param("userId") UUID userId, @Param("board") LeaderboardBoard board);
}
