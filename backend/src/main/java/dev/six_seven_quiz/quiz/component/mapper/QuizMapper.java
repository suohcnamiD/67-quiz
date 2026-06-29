package dev.six_seven_quiz.quiz.component.mapper;

import dev.six_seven_quiz.quiz.dto.response.QuizRatingSummaryDto;
import dev.six_seven_quiz.quiz.dto.response.authoring.QuizDto;
import dev.six_seven_quiz.quiz.dto.response.viewing.QuizSummaryDto;
import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.user.profile.dto.AuthorSummaryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface QuizMapper {
    QuizDto toDto(Quiz quiz, int questionCount, boolean youAreAuthor);

    @Mapping(source = "author", target = "author")
    @Mapping(source = "ratingSummary", target = "ratingSummary")
    QuizSummaryDto toSummary(Quiz quiz, int questionCount, boolean youAreAuthor, AuthorSummaryDto author, QuizRatingSummaryDto ratingSummary);
}
