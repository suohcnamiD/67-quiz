package dev.six_seven_quiz.quiz.controller;

import dev.six_seven_quiz.quiz.dto.request.AttemptQuizRequest;
import dev.six_seven_quiz.quiz.dto.request.CommitAttemptActionsRequest;
import dev.six_seven_quiz.quiz.dto.request.FinishAttemptRequest;
import dev.six_seven_quiz.quiz.dto.response.attempt.AttemptInProgressDto;
import dev.six_seven_quiz.quiz.dto.response.viewing.FinishedAttemptSummaryDto;
import dev.six_seven_quiz.quiz.dto.response.viewing.FinishedOptionDto;
import dev.six_seven_quiz.quiz.service.AttemptService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attempt")
public class AttemptController {


    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping
    public AttemptInProgressDto attemptQuiz(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid AttemptQuizRequest request
    ) {
        return attemptService.attemptQuizAsUser(userDetails, request.quizId());
    }

    @GetMapping("/in-progress")
    public PagedModel<EntityModel<AttemptInProgressDto>> getAttemptsInProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam (defaultValue = "0") int page,
            @Parameter(hidden = true) PagedResourcesAssembler<AttemptInProgressDto> assembler
    ) {
        return assembler.toModel(attemptService.getAttemptsInProgressAsUser(userDetails, page));
    }

    @GetMapping("/finished")
    public PagedModel<EntityModel<FinishedAttemptSummaryDto>> getFinishedAttempts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam (defaultValue = "0") int page,
            @Parameter(hidden = true) PagedResourcesAssembler<FinishedAttemptSummaryDto> assembler
    ) {
        return assembler.toModel(attemptService.getFinishedAttemptsAsUser(userDetails, page));
    }

    @PatchMapping("/finish")
    public FinishedAttemptSummaryDto finishAttempt(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid FinishAttemptRequest request
    ) {
        return attemptService.finishAttemptAsUser(userDetails, request);
    }

    @PatchMapping("/commit")
    public AttemptInProgressDto commitAttemptActions(
             @AuthenticationPrincipal UserDetails userDetails,
             @RequestBody @Valid CommitAttemptActionsRequest request
    ) {
        return attemptService.commitAttemptActionsAsUser(userDetails, request);
    }
}
