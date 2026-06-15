package dev.six_seven_quiz.quiz.component.mapper;

import dev.six_seven_quiz.quiz.dto.response.viewing.QuestionSummaryDto;
import dev.six_seven_quiz.quiz.model.Question;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface QuestionMapper {
    QuestionSummaryDto toSummaryDto(Question question);

//    @Mapping(target = "options", source = "options")
//    AttemptQuestionD toDto(Question question, List<OptionDto> options);
}
