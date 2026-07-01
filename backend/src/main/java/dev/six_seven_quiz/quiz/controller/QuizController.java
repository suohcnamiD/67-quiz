package dev.six_seven_quiz.quiz.controller;

import dev.six_seven_quiz.quiz.dto.request.CreateQuizRequest;
import dev.six_seven_quiz.quiz.dto.request.RenameQuizRequest;
import dev.six_seven_quiz.quiz.dto.request.ReorderQuestionsRequest;
import dev.six_seven_quiz.quiz.dto.request.UpdateQuizDescriptionRequest;
import dev.six_seven_quiz.quiz.dto.response.authoring.QuizDto;
import dev.six_seven_quiz.quiz.dto.response.viewing.QuizSummaryDto;
import dev.six_seven_quiz.quiz.service.QuizService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }


    @DeleteMapping("/{quizId}")
    public void deleteQuiz(
            @PathVariable @NotNull UUID quizId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        quizService.deleteAsUser(quizId, userDetails);
    }

    @GetMapping("/authoring/{quizId}")
    public QuizDto getQuiz(
            @PathVariable @NotNull UUID quizId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return quizService.getAsAuthor(quizId, userDetails);
    }

    @GetMapping
    public PagedModel<EntityModel<QuizSummaryDto>> getQuizzes(
            @RequestParam (defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(hidden = true) PagedResourcesAssembler<QuizSummaryDto> pagedResourcesAssembler
    ) {
        return pagedResourcesAssembler.toModel(quizService.getQuizzes(page, userDetails));
    }

    @PostMapping
    public QuizDto createQuiz(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CreateQuizRequest request
    ) {
        return quizService.createQuiz(userDetails, request);
    }

    @PatchMapping("/{quizId}")
    public QuizDto renameQuiz(
            @PathVariable @NotNull UUID quizId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid RenameQuizRequest request
    ) {
        return quizService.renameAsUser(quizId, userDetails, request);
    }

    @PatchMapping("/{quizId}/description")
    public QuizDto updateQuizDescription(
            @PathVariable @NotNull UUID quizId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid UpdateQuizDescriptionRequest request
    ) {
        return quizService.updateDescriptionAsUser(quizId, userDetails, request);
    }

    @PatchMapping("/{quizId}/reorder")
    public QuizDto reorderQuestions(
            @PathVariable @NotNull UUID quizId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid ReorderQuestionsRequest request
    ) {
        return quizService.reorderAsUser(quizId, userDetails, request);
    }
}
