package dev.six_seven_quiz.quiz.component.mapper;

import dev.six_seven_quiz.quiz.dto.response.attempt.AttemptOptionDto;
import dev.six_seven_quiz.quiz.dto.response.attempt.AttemptQuestionDto;
import dev.six_seven_quiz.quiz.dto.response.viewing.FinishedOptionDto;
import dev.six_seven_quiz.quiz.dto.response.viewing.FinishedQuestionDto;
import dev.six_seven_quiz.quiz.model.AttemptQuestion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface AttemptQuestionMapper {
    @Mapping(source = "options", target = "options")
    @Mapping(target = "hasImage", expression = "java(question.getQuestion().getImagePath() != null)")
    AttemptQuestionDto toDto(AttemptQuestion question, List<AttemptOptionDto> options);

    @Mapping(source = "options", target = "options")
    @Mapping(target = "hasImage", expression = "java(question.getQuestion().getImagePath() != null)")
    FinishedQuestionDto toFinishedDto(AttemptQuestion question, List<FinishedOptionDto> options);
}
