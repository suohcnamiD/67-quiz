package dev.six_seven_quiz.quiz.component.mapper;

import dev.six_seven_quiz.quiz.dto.response.AttemptQuestionDto;
import dev.six_seven_quiz.quiz.dto.response.OptionDto;
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
    AttemptQuestionDto toDto(AttemptQuestion question, List<OptionDto> options);
}
