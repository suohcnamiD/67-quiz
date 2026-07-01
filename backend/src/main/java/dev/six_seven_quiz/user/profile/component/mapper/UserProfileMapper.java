package dev.six_seven_quiz.user.profile.component.mapper;

import dev.six_seven_quiz.authorization.AdminChecker;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.profile.dto.AuthorSummaryDto;
import dev.six_seven_quiz.user.profile.dto.UserProfileDto;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public UserProfileDto toDto(
            ApplicationUser user,
            int quizzesAuthored,
            int attemptsTaken,
            Integer averageScorePercent,
            boolean isYou
    ) {
        // isAdmin only leaks on the caller's own profile — it's a bit of
        // identity meant for the FE to decide whether to render admin
        // affordances, not something to broadcast about other users.
        boolean isAdmin = isYou && AdminChecker.isAdmin(user);
        return new UserProfileDto(
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarPath() != null,
                quizzesAuthored,
                attemptsTaken,
                averageScorePercent,
                isYou,
                isAdmin
        );
    }

    public AuthorSummaryDto toAuthorSummary(ApplicationUser user) {
        return new AuthorSummaryDto(
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarPath() != null
        );
    }
}
