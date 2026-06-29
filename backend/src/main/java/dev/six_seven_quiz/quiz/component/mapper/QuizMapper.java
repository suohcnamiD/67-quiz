package dev.six_seven_quiz.quiz.component.mapper;

import dev.six_seven_quiz.quiz.dto.response.QuizRatingSummaryDto;
import dev.six_seven_quiz.quiz.dto.response.authoring.OptionDto;
import dev.six_seven_quiz.quiz.dto.response.authoring.QuestionDto;
import dev.six_seven_quiz.quiz.dto.response.authoring.QuizDto;
import dev.six_seven_quiz.quiz.dto.response.viewing.QuizSummaryDto;
import dev.six_seven_quiz.quiz.model.Option;
import dev.six_seven_quiz.quiz.model.Question;
import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.user.profile.dto.AuthorSummaryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface QuizMapper {
    @Mapping(target = "hasCover", expression = "java(quiz.getCoverImagePath() != null)")
    QuizDto toDto(Quiz quiz, int questionCount, boolean youAreAuthor);

    @Mapping(source = "author", target = "author")
    @Mapping(source = "ratingSummary", target = "ratingSummary")
    @Mapping(target = "hasCover", expression = "java(quiz.getCoverImagePath() != null)")
    QuizSummaryDto toSummary(Quiz quiz, int questionCount, boolean youAreAuthor, AuthorSummaryDto author, QuizRatingSummaryDto ratingSummary);

    /**
     * Element mapper picked up implicitly when generating QuizDto.questions —
     * adds the hasImage flag derived from the entity's imagePath column.
     */
    @Mapping(target = "hasImage", expression = "java(question.getImagePath() != null)")
    QuestionDto questionToDto(Question question);

    @Mapping(target = "hasImage", expression = "java(option.getImagePath() != null)")
    @Mapping(target = "correct", expression = "java(option.isCorrect() != null && option.isCorrect())")
    OptionDto optionToDto(Option option);
}
