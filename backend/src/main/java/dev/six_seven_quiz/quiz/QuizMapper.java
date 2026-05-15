package dev.six_seven_quiz.quiz;

import dev.six_seven_quiz.quiz.dto.response.QuizDto;
import dev.six_seven_quiz.quiz.model.Quiz;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface QuizMapper {
    QuizDto toDto(Quiz quiz);
}
