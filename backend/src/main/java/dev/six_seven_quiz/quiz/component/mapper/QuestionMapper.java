package dev.six_seven_quiz.quiz.component.mapper;

import dev.six_seven_quiz.quiz.dto.response.OptionDto;
import dev.six_seven_quiz.quiz.dto.response.QuestionDto;
import dev.six_seven_quiz.quiz.dto.response.QuestionSummaryDto;
import dev.six_seven_quiz.quiz.model.Question;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface QuestionMapper {
    QuestionSummaryDto toSummaryDto(Question question);
    @Mapping(target = "options", source = "options")
    QuestionDto toDto(Question question, List<OptionDto> options);
}
