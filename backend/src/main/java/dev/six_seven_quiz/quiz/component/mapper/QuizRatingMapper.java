package dev.six_seven_quiz.quiz.component.mapper;

import dev.six_seven_quiz.quiz.dto.response.QuizRatingDto;
import dev.six_seven_quiz.quiz.model.QuizRating;
import dev.six_seven_quiz.user.profile.dto.AuthorSummaryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface QuizRatingMapper {

    @Mapping(source = "rating.id", target = "id")
    @Mapping(source = "rating.score", target = "score")
    @Mapping(source = "rating.comment", target = "comment")
    @Mapping(source = "rating.createdAt", target = "createdAt")
    @Mapping(source = "rating.updatedAt", target = "updatedAt")
    @Mapping(source = "author", target = "author")
    QuizRatingDto toDto(QuizRating rating, AuthorSummaryDto author);
}
