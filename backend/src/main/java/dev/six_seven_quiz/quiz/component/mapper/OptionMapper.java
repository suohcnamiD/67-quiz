package dev.six_seven_quiz.quiz.component.mapper;

import dev.six_seven_quiz.quiz.dto.response.OptionDto;
import dev.six_seven_quiz.quiz.model.Option;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface OptionMapper {
    OptionDto toDto(Option option, boolean selected);
}
