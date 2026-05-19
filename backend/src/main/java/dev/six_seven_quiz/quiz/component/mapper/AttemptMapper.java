package dev.six_seven_quiz.quiz.component.mapper;

import dev.six_seven_quiz.quiz.dto.response.AttemptDto;
import dev.six_seven_quiz.quiz.dto.response.QuestionDto;
import dev.six_seven_quiz.quiz.dto.response.QuizDto;
import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.quiz.model.QuizAttempt;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface AttemptMapper {
    AttemptDto toDto(QuizAttempt attempt, List<QuestionDto> questions);
}
