package dev.six_seven_quiz.user.profile.component.mapper;

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
        return new UserProfileDto(
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarPath() != null,
                quizzesAuthored,
                attemptsTaken,
                averageScorePercent,
                isYou
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
