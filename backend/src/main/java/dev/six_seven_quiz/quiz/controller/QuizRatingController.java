package dev.six_seven_quiz.quiz.controller;

import dev.six_seven_quiz.quiz.dto.request.UpsertRatingRequest;
import dev.six_seven_quiz.quiz.dto.response.QuizRatingDto;
import dev.six_seven_quiz.quiz.dto.response.QuizRatingSummaryDto;
import dev.six_seven_quiz.quiz.service.QuizRatingService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/quiz/{quizId}/ratings")
public class QuizRatingController {

    private final QuizRatingService ratingService;

    public QuizRatingController(QuizRatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PutMapping("/me")
    public QuizRatingDto upsertMine(
            @PathVariable @NotNull UUID quizId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid UpsertRatingRequest request
    ) {
        return ratingService.upsert(quizId, userDetails, request.score(), request.comment());
    }

    @GetMapping("/me")
    public ResponseEntity<QuizRatingDto> getMine(
            @PathVariable @NotNull UUID quizId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ratingService.getMine(quizId, userDetails)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public PagedModel<EntityModel<QuizRatingDto>> list(
            @PathVariable @NotNull UUID quizId,
            @RequestParam(defaultValue = "0") int page,
            @Parameter(hidden = true) PagedResourcesAssembler<QuizRatingDto> pagedResourcesAssembler
    ) {
        return pagedResourcesAssembler.toModel(ratingService.list(quizId, page));
    }

    @GetMapping("/summary")
    public QuizRatingSummaryDto summary(@PathVariable @NotNull UUID quizId) {
        QuizRatingService.Summary s = ratingService.aggregate(quizId);
        return new QuizRatingSummaryDto(s.average(), s.count());
    }
}
