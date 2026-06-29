package dev.six_seven_quiz.notification.service;

import dev.six_seven_quiz.notification.model.LeaderboardBoard;
import dev.six_seven_quiz.notification.model.LeaderboardSnapshot;
import dev.six_seven_quiz.notification.model.NotificationType;
import dev.six_seven_quiz.notification.repository.LeaderboardSnapshotRepository;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserRepository;
import dev.six_seven_quiz.user.service.LeaderboardService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runs on a schedule, captures each qualifying user's leaderboard rank, and
 * emits a RANK_DROPPED notification when a user's rank worsened compared to
 * the most recent snapshot for that board.
 *
 * "Worsened" means the rank number grew (e.g. #3 → #7). New entries with no
 * prior snapshot don't notify — there's nothing to compare against.
 */
@Service
public class LeaderboardSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardSnapshotService.class);

    private final LeaderboardService leaderboardService;
    private final LeaderboardSnapshotRepository snapshotRepository;
    private final ApplicationUserRepository applicationUserRepository;
    private final NotificationService notificationService;

    public LeaderboardSnapshotService(
            LeaderboardService leaderboardService,
            LeaderboardSnapshotRepository snapshotRepository,
            ApplicationUserRepository applicationUserRepository,
            NotificationService notificationService
    ) {
        this.leaderboardService = leaderboardService;
        this.snapshotRepository = snapshotRepository;
        this.applicationUserRepository = applicationUserRepository;
        this.notificationService = notificationService;
    }

    /**
     * Default cadence is 15 minutes — rank churn isn't a real-time concern
     * and the job loads the whole qualifying population on each run. Override
     * with {@code notifications.snapshot.interval-ms} for tests.
     */
    @Scheduled(fixedDelayString = "${notifications.snapshot.interval-ms:900000}",
               initialDelayString = "${notifications.snapshot.initial-delay-ms:60000}")
    @Transactional
    public void run() {
        snapshotBoard(LeaderboardBoard.PLAYERS, leaderboardService.rankedPlayerIds());
        snapshotBoard(LeaderboardBoard.AUTHORS, leaderboardService.rankedAuthorIds());
    }

    /** Trigger a single snapshot pass on demand (used by tests). */
    @Transactional
    public void runOnce() {
        run();
    }

    private void snapshotBoard(LeaderboardBoard board, List<UUID> rankedIds) {
        for (int i = 0; i < rankedIds.size(); i++) {
            UUID userId = rankedIds.get(i);
            int newRank = i + 1;
            snapshotRepository.findLatest(userId, board).ifPresentOrElse(
                    prev -> {
                        if (newRank > prev.getRank()) {
                            emitRankDrop(userId, board, prev.getRank(), newRank);
                        }
                        persist(userId, board, newRank);
                    },
                    () -> persist(userId, board, newRank)
            );
        }
    }

    private void persist(UUID userId, LeaderboardBoard board, int rank) {
        ApplicationUser user = applicationUserRepository.findById(userId).orElse(null);
        if (user == null) return;
        snapshotRepository.save(new LeaderboardSnapshot(user, board, rank));
    }

    private void emitRankDrop(UUID userId, LeaderboardBoard board, int from, int to) {
        ApplicationUser user = applicationUserRepository.findById(userId).orElse(null);
        if (user == null) return;
        Map<String, Object> payload = new HashMap<>();
        payload.put("board", board.name());
        payload.put("from", from);
        payload.put("to", to);
        notificationService.create(user, NotificationType.RANK_DROPPED, payload);
        log.debug("Rank drop on {} for user {}: #{} → #{}", board, user.getUsername(), from, to);
    }
}
