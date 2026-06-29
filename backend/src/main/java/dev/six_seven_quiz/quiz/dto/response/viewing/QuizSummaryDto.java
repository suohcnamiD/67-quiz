package dev.six_seven_quiz.quiz.dto.response.viewing;

import dev.six_seven_quiz.quiz.dto.response.QuizRatingSummaryDto;
import dev.six_seven_quiz.user.profile.dto.AuthorSummaryDto;
import org.springframework.hateoas.server.core.Relation;

import java.time.Duration;
import java.util.UUID;

@Relation(collectionRelation = "quizzes", itemRelation = "quiz")
public record QuizSummaryDto(
    String name,
    int questionCount,
    int maximumScore,
    Duration duration,
    UUID id,
    boolean youAreAuthor,
    boolean hasCover,
    AuthorSummaryDto author,
    QuizRatingSummaryDto ratingSummary
) {
}
