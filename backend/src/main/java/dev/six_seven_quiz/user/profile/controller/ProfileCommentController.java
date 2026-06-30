package dev.six_seven_quiz.user.profile.controller;

import dev.six_seven_quiz.user.profile.dto.PostCommentRequest;
import dev.six_seven_quiz.user.profile.dto.ProfileCommentDto;
import dev.six_seven_quiz.user.profile.service.ProfileCommentService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users/{username}/comments")
public class ProfileCommentController {

    private final ProfileCommentService commentService;

    public ProfileCommentController(ProfileCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public PagedModel<EntityModel<ProfileCommentDto>> listComments(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(hidden = true) PagedResourcesAssembler<ProfileCommentDto> pagedResourcesAssembler
    ) {
        Page<ProfileCommentDto> comments = commentService.list(username, userDetails, page);
        return pagedResourcesAssembler.toModel(comments);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileCommentDto postComment(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid PostCommentRequest request
    ) {
        return commentService.post(username, userDetails, request.body());
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable String username,
            @PathVariable @NotNull UUID commentId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        commentService.delete(commentId, userDetails);
    }
}
