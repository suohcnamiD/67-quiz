package dev.six_seven_quiz.user.profile.service;

import dev.six_seven_quiz.notification.model.NotificationType;
import dev.six_seven_quiz.notification.service.NotificationService;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserRepository;
import dev.six_seven_quiz.user.ApplicationUserService;
import dev.six_seven_quiz.user.profile.component.mapper.UserProfileMapper;
import dev.six_seven_quiz.user.profile.dto.ProfileCommentDto;
import dev.six_seven_quiz.user.profile.exception.CommentNotFoundException;
import dev.six_seven_quiz.user.profile.exception.InvalidCommentException;
import dev.six_seven_quiz.user.profile.exception.NoAccessToCommentException;
import dev.six_seven_quiz.user.profile.exception.UnknownUsernameException;
import dev.six_seven_quiz.user.profile.model.ProfileComment;
import dev.six_seven_quiz.user.profile.repository.ProfileCommentRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ProfileCommentService {

    public static final int COMMENTS_PER_PAGE = 10;
    public static final int BODY_MAX = 1000;

    private final ApplicationUserService applicationUserService;
    private final ApplicationUserRepository applicationUserRepository;
    private final ProfileCommentRepository commentRepository;
    private final UserProfileMapper userProfileMapper;
    private final NotificationService notificationService;

    public ProfileCommentService(
            ApplicationUserService applicationUserService,
            ApplicationUserRepository applicationUserRepository,
            ProfileCommentRepository commentRepository,
            UserProfileMapper userProfileMapper,
            NotificationService notificationService
    ) {
        this.applicationUserService = applicationUserService;
        this.applicationUserRepository = applicationUserRepository;
        this.commentRepository = commentRepository;
        this.userProfileMapper = userProfileMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public ProfileCommentDto post(String targetUsername, UserDetails callerDetails, String body) {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidCommentException("Comment body must not be empty");
        }
        if (trimmed.length() > BODY_MAX) {
            throw new InvalidCommentException("Comment body must be at most " + BODY_MAX + " characters");
        }
        ApplicationUser author = applicationUserService.getAuthenticatedUserFromDetails(callerDetails);
        ApplicationUser target = applicationUserRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new UnknownUsernameException(targetUsername));
        ProfileComment saved = commentRepository.save(new ProfileComment(target, author, trimmed));

        // Notify the profile owner unless they're commenting on themselves.
        if (!target.getId().equals(author.getId())) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("actorUsername", author.getUsername());
            payload.put("actorDisplayName", author.getDisplayName());
            payload.put("commentId", saved.getId().toString());
            payload.put("preview", trimmed.length() > 140 ? trimmed.substring(0, 140) + "…" : trimmed);
            notificationService.create(target, NotificationType.COMMENT_RECEIVED, payload);
        }

        return toDto(saved, author);
    }

    @Transactional
    public Page<ProfileCommentDto> list(String targetUsername, UserDetails callerDetails, int page) {
        if (!applicationUserRepository.findByUsername(targetUsername).isPresent()) {
            throw new UnknownUsernameException(targetUsername);
        }
        ApplicationUser caller = applicationUserService.getAuthenticatedUserFromDetails(callerDetails);
        Pageable pageable = PageRequest.of(page, COMMENTS_PER_PAGE);
        return commentRepository.findByTarget_UsernameOrderByCreatedAtDesc(targetUsername, pageable)
                .map(comment -> toDto(comment, caller));
    }

    @Transactional
    public void delete(UUID commentId, UserDetails callerDetails) {
        ApplicationUser caller = applicationUserService.getAuthenticatedUserFromDetails(callerDetails);
        ProfileComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        boolean isAuthor = comment.getAuthor().getId().equals(caller.getId());
        boolean isOwner = comment.getTarget().getId().equals(caller.getId());
        if (!isAuthor && !isOwner) {
            throw new NoAccessToCommentException(commentId);
        }
        commentRepository.delete(comment);
    }

    private ProfileCommentDto toDto(ProfileComment comment, ApplicationUser viewer) {
        boolean canDelete = comment.getAuthor().getId().equals(viewer.getId())
                || comment.getTarget().getId().equals(viewer.getId());
        return new ProfileCommentDto(
                comment.getId(),
                comment.getBody(),
                comment.getCreatedAt(),
                userProfileMapper.toAuthorSummary(comment.getAuthor()),
                canDelete
        );
    }
}
