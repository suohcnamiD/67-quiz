package dev.six_seven_quiz.quiz.component.mapper;

import dev.six_seven_quiz.quiz.dto.response.attempt.AttemptInProgressDto;
import dev.six_seven_quiz.quiz.dto.response.attempt.AttemptQuestionDto;
import dev.six_seven_quiz.quiz.dto.response.viewing.FinishedAttemptSummaryDto;
import dev.six_seven_quiz.quiz.dto.response.viewing.FinishedQuestionDto;
import dev.six_seven_quiz.quiz.model.Attempt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface AttemptMapper {
    @Mapping(source = "attempt.id", target = "id")
    @Mapping(source = "questions", target = "questions")
    AttemptInProgressDto toDto(Attempt attempt, List<AttemptQuestionDto> questions);

    @Mapping(source = "questions", target = "questions")
    FinishedAttemptSummaryDto toFinishedSummary(Attempt attempt, List<FinishedQuestionDto> questions, int score, int maximumScore);
}
