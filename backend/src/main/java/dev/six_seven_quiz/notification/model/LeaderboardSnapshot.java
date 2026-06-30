package dev.six_seven_quiz.notification.model;

import dev.six_seven_quiz.user.ApplicationUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leaderboard_snapshots")
public class LeaderboardSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private ApplicationUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "board", nullable = false, length = 16)
    private LeaderboardBoard board;

    @Column(name = "rank", nullable = false)
    private int rank;

    @Column(name = "snapshot_at", nullable = false, updatable = false)
    private Instant snapshotAt;

    protected LeaderboardSnapshot() {}

    public LeaderboardSnapshot(ApplicationUser user, LeaderboardBoard board, int rank) {
        this.user = user;
        this.board = board;
        this.rank = rank;
        this.snapshotAt = Instant.now();
    }

    public UUID getId() { return id; }
    public ApplicationUser getUser() { return user; }
    public LeaderboardBoard getBoard() { return board; }
    public int getRank() { return rank; }
    public Instant getSnapshotAt() { return snapshotAt; }
}
