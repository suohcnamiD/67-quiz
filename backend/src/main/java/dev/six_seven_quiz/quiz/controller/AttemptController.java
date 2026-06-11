package dev.six_seven_quiz.quiz.controller;

import dev.six_seven_quiz.quiz.dto.request.AttemptQuizRequest;
import dev.six_seven_quiz.quiz.dto.request.CommitAttemptActionsRequest;
import dev.six_seven_quiz.quiz.dto.response.AttemptDto;
import dev.six_seven_quiz.quiz.service.AttemptService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attempt")
public class AttemptController {


    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping
    public AttemptDto attemptQuiz(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid AttemptQuizRequest request
    ) {
        return attemptService.attemptQuizAsUser(userDetails, request.quizId());
    }

     @PatchMapping("/commit")
    public AttemptDto commitAttemptActions(
             @AuthenticationPrincipal UserDetails userDetails,
             @RequestBody @Valid CommitAttemptActionsRequest request
    ) {
        return attemptService.commitAttemptActionsAsUser(userDetails, request);
    }
}
