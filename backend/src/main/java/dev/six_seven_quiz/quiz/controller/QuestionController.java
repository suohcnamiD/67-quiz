package dev.six_seven_quiz.quiz.controller;

import dev.six_seven_quiz.quiz.dto.request.AddQuestionRequest;
import dev.six_seven_quiz.quiz.dto.request.DeleteQuestionRequest;
import dev.six_seven_quiz.quiz.dto.response.viewing.QuestionSummaryDto;
import dev.six_seven_quiz.quiz.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @DeleteMapping("/question")
    public List<QuestionSummaryDto> deleteQuizQuestion(

            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid DeleteQuestionRequest request
    ) {
        return questionService.deleteAsUser(userDetails, request);
    }
}
