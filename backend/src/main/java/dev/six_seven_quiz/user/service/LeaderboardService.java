package dev.six_seven_quiz.user.service;

import dev.six_seven_quiz.quiz.model.Attempt;
import dev.six_seven_quiz.quiz.repository.QuizAttemptRepository;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserRepository;
import dev.six_seven_quiz.user.ApplicationUserService;
import dev.six_seven_quiz.user.dto.LeaderboardEntryDto;
import dev.six_seven_quiz.user.dto.LeaderboardPageDto;
import dev.six_seven_quiz.user.profile.component.mapper.UserProfileMapper;
import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    public static final int ENTRIES_PER_PAGE = 20;
    public static final long MIN_PLAYER_ATTEMPTS = 3;
    public static final long MIN_AUTHOR_RATINGS = 5;
    /**
     * Strength of the Bayesian prior for the players board, in "virtual
     * attempts". Higher = a user's adjusted score moves more slowly toward
     * their true average as they accumulate attempts. K=5 means a 100%/1-attempt
     * user sits closer to the prior (50%) than to their reported average,
     * while a user with 20+ attempts is essentially at their true mean.
     */
    public static final int PLAYER_PRIOR_K = 5;
    public static final double PLAYER_PRIOR_MEAN = 50.0;

    private final ApplicationUserService applicationUserService;
    private final ApplicationUserRepository applicationUserRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserProfileMapper userProfileMapper;

    public LeaderboardService(
            ApplicationUserService applicationUserService,
            ApplicationUserRepository applicationUserRepository,
            QuizAttemptRepository quizAttemptRepository,
            UserProfileMapper userProfileMapper
    ) {
        this.applicationUserService = applicationUserService;
        this.applicationUserRepository = applicationUserRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.userProfileMapper = userProfileMapper;
    }

    /**
     * Adjusted attempt-score average across each user's finished attempts.
     * The averaging happens in Java because {@code Attempt.maximumScore} is
     * computed from the quiz's questions rather than stored on the row.
     *
     * Ranking uses Bayesian shrinkage:
     *   adjusted = (sumOfPercentages + K * priorMean) / (attempts + K)
     * which pulls light players toward the prior (50%) so a 100% / 1-attempt
     * user can't outrank a 90% / 50-attempt user. Once a user has many
     * attempts, the prior washes out and the score approaches the true mean.
     */
    @Transactional
    public LeaderboardPageDto topPlayers(int page, UserDetails callerDetails) {
        ApplicationUser caller = applicationUserService.getAuthenticatedUserFromDetails(callerDetails);

        List<UUID> candidateIds = applicationUserRepository.findUserIdsWithFinishedAttempts();
        Map<UUID, ApplicationUser> usersById = applicationUserRepository.findAllById(candidateIds).stream()
                .collect(Collectors.toMap(ApplicationUser::getId, u -> u));

        List<PlayerRow> rows = new ArrayList<>();
        for (UUID userId : candidateIds) {
            ApplicationUser user = usersById.get(userId);
            if (user == null) continue;
            List<Attempt> attempts = quizAttemptRepository.findAllByUserAndFinishedIsTrue(user);
            double sumPercent = 0;
            int counted = 0;
            for (Attempt attempt : attempts) {
                int max = attempt.getMaximumScore();
                if (max == 0) continue;
                sumPercent += (100.0 * attempt.getEarnedScore()) / max;
                counted++;
            }
            if (counted < MIN_PLAYER_ATTEMPTS) continue;
            double adjusted = (sumPercent + PLAYER_PRIOR_K * PLAYER_PRIOR_MEAN) / (counted + PLAYER_PRIOR_K);
            rows.add(new PlayerRow(user, adjusted, counted));
        }
        // Sort by adjusted DESC, attempts DESC. Build the natural-order
        // comparator first, then reverse the composed result — chaining
        // .reversed() per key would flip the second key back to ascending.
        rows.sort(
                Comparator.comparingDouble(PlayerRow::adjusted)
                        .thenComparingLong(PlayerRow::attempts)
                        .reversed()
        );

        return paginate(rows, page, caller, PlayerRow::user, PlayerRow::adjusted, r -> (long) r.attempts());
    }

    /**
     * Avg rating across each author's owned quizzes, computed at the DB level
     * via JPQL aggregation.
     */
    @Transactional
    public LeaderboardPageDto topAuthors(int page, UserDetails callerDetails) {
        ApplicationUser caller = applicationUserService.getAuthenticatedUserFromDetails(callerDetails);

        List<Object[]> raw = applicationUserRepository.findAuthorRankings(MIN_AUTHOR_RATINGS);
        List<UUID> ids = raw.stream().map(r -> (UUID) r[0]).toList();
        Map<UUID, ApplicationUser> usersById = applicationUserRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ApplicationUser::getId, u -> u));

        List<AuthorRow> rows = new ArrayList<>();
        for (Object[] r : raw) {
            UUID id = (UUID) r[0];
            ApplicationUser user = usersById.get(id);
            if (user == null) continue;
            double avg = ((Number) r[1]).doubleValue();
            long ratingCount = ((Number) r[2]).longValue();
            rows.add(new AuthorRow(user, avg, ratingCount));
        }
        // The JPQL query already sorts; preserve that order. Stable.

        return paginate(rows, page, caller, AuthorRow::user, AuthorRow::avg, AuthorRow::ratings);
    }

    private <R> LeaderboardPageDto paginate(
            List<R> rows,
            int page,
            ApplicationUser caller,
            java.util.function.Function<R, ApplicationUser> userOf,
            java.util.function.ToDoubleFunction<R> primaryOf,
            java.util.function.ToLongFunction<R> secondaryOf
    ) {
        long total = rows.size();
        int totalPages = (int) Math.max(1, Math.ceil(total / (double) ENTRIES_PER_PAGE));
        int safePage = Math.max(0, page);
        int from = Math.min(safePage * ENTRIES_PER_PAGE, rows.size());
        int to = Math.min(from + ENTRIES_PER_PAGE, rows.size());

        List<LeaderboardEntryDto> entries = new ArrayList<>(to - from);
        for (int i = from; i < to; i++) {
            R row = rows.get(i);
            entries.add(new LeaderboardEntryDto(
                    i + 1,
                    userProfileMapper.toAuthorSummary(userOf.apply(row)),
                    primaryOf.applyAsDouble(row),
                    secondaryOf.applyAsLong(row)
            ));
        }

        LeaderboardEntryDto you = null;
        for (int i = 0; i < rows.size(); i++) {
            R row = rows.get(i);
            if (userOf.apply(row).getId().equals(caller.getId())) {
                you = new LeaderboardEntryDto(
                        i + 1,
                        userProfileMapper.toAuthorSummary(caller),
                        primaryOf.applyAsDouble(row),
                        secondaryOf.applyAsLong(row)
                );
                break;
            }
        }

        return new LeaderboardPageDto(entries, safePage, totalPages, total, you);
    }

    private record PlayerRow(ApplicationUser user, double adjusted, int attempts) {}
    private record AuthorRow(ApplicationUser user, double avg, long ratings) {}
}
