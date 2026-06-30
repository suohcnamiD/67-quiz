package dev.six_seven_quiz.search.service;

import dev.six_seven_quiz.quiz.dto.response.viewing.QuizSummaryDto;
import dev.six_seven_quiz.quiz.service.QuizService;
import dev.six_seven_quiz.search.dto.SearchResponseDto;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserRepository;
import dev.six_seven_quiz.user.ApplicationUserService;
import dev.six_seven_quiz.user.profile.component.mapper.UserProfileMapper;
import dev.six_seven_quiz.user.profile.dto.AuthorSummaryDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    /** Minimum query length after trim. Below this we return empty so a single
     *  keystroke doesn't dump the whole user list. */
    private static final int MIN_QUERY_LENGTH = 2;

    /** Max results per side (quizzes / users). Search is a peek, not paged
     *  browsing — for the latter we have /quiz and /users/{u}/quizzes. */
    private static final int RESULTS_PER_SIDE = 10;

    private final QuizService quizService;
    private final ApplicationUserRepository applicationUserRepository;
    private final ApplicationUserService applicationUserService;
    private final UserProfileMapper userProfileMapper;

    public SearchService(
            QuizService quizService,
            ApplicationUserRepository applicationUserRepository,
            ApplicationUserService applicationUserService,
            UserProfileMapper userProfileMapper
    ) {
        this.quizService = quizService;
        this.applicationUserRepository = applicationUserRepository;
        this.applicationUserService = applicationUserService;
        this.userProfileMapper = userProfileMapper;
    }

    public SearchResponseDto search(String rawQuery, UserDetails callerDetails) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < MIN_QUERY_LENGTH) {
            return new SearchResponseDto(List.of(), List.of(), query);
        }

        ApplicationUser caller = applicationUserService.getAuthenticatedUserFromDetails(callerDetails);

        List<QuizSummaryDto> quizzes = quizService
                .searchQuizzesByName(query, caller, 0, RESULTS_PER_SIDE)
                .getContent();

        List<AuthorSummaryDto> users = applicationUserRepository
                .findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
                        query, query, PageRequest.of(0, RESULTS_PER_SIDE))
                .map(userProfileMapper::toAuthorSummary)
                .getContent();

        return new SearchResponseDto(quizzes, users, query);
    }
}
