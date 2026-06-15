package dev.six_seven_quiz.quiz.component.mapper;

import dev.six_seven_quiz.quiz.dto.response.attempt.AttemptDto;
import dev.six_seven_quiz.quiz.dto.response.attempt.AttemptQuestionDto;
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
    AttemptDto toDto(Attempt attempt, List<AttemptQuestionDto> questions);
}
