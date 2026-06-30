package dev.six_seven_quiz.quiz.controller;

import dev.six_seven_quiz.quiz.dto.request.AddQuestionRequest;
import dev.six_seven_quiz.quiz.dto.request.DeleteQuestionRequest;
import dev.six_seven_quiz.quiz.dto.request.EditQuestionRequest;
import dev.six_seven_quiz.quiz.dto.response.viewing.QuestionSummaryDto;
import dev.six_seven_quiz.quiz.service.QuestionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/question")
public class QuestionController {
    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    public List<QuestionSummaryDto> addQuizQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid AddQuestionRequest request
    ) {
        return questionService.addQuizQuestionAsUser(userDetails, request);
    }

    @PatchMapping("/{id}")
    public List<QuestionSummaryDto> editQuizQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable @NotNull @Valid UUID id,
            @RequestBody @Valid EditQuestionRequest request
    ) {
        return questionService.editAsUser(userDetails, id, request);
    }

    @DeleteMapping("/{id}")
    public List<QuestionSummaryDto> deleteQuizQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable @NotNull @Valid UUID id
    ) {
        return questionService.deleteAsUser(userDetails, new DeleteQuestionRequest(id));
    }
}
