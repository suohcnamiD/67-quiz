package dev.six_seven_quiz.quiz.controller;

import dev.six_seven_quiz.quiz.dto.request.AddQuestionRequest;
import dev.six_seven_quiz.quiz.dto.request.CreateQuizRequest;
import dev.six_seven_quiz.quiz.dto.response.QuestionSummaryDto;
import dev.six_seven_quiz.quiz.dto.response.QuizDto;
import dev.six_seven_quiz.quiz.service.AttemptService;
import dev.six_seven_quiz.quiz.service.QuestionService;
import dev.six_seven_quiz.quiz.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    private final QuizService quizService;
    private final QuestionService questionService;
    private final AttemptService attemptService;

    public QuizController(QuizService quizService, QuestionService questionService, AttemptService attemptService) {
        this.quizService = quizService;
        this.questionService = questionService;
        this.attemptService = attemptService;
    }

    @PostMapping("/question")
    public List<QuestionSummaryDto> addQuizQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid AddQuestionRequest request
    ) {
        return questionService.addQuizQuestionAsUser(userDetails, request);
    }

    @GetMapping
    public Page<QuizDto> getQuizzes(
            @RequestParam (defaultValue = "0") int page
    ) {
        return quizService.getQuizzes(page);
    }

    @PostMapping
    public QuizDto createQuiz(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CreateQuizRequest request
    ) {
        return quizService.createQuiz(userDetails, request);
    }
}
