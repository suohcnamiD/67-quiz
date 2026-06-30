package dev.six_seven_quiz.notification.controller;

import dev.six_seven_quiz.notification.dto.NotificationDto;
import dev.six_seven_quiz.notification.dto.UnreadCountDto;
import dev.six_seven_quiz.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public PagedModel<EntityModel<NotificationDto>> listNotifications(
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(hidden = true) PagedResourcesAssembler<NotificationDto> pagedResourcesAssembler
    ) {
        return pagedResourcesAssembler.toModel(notificationService.list(userDetails, page));
    }

    @GetMapping("/unread-count")
    public UnreadCountDto unreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        return new UnreadCountDto(notificationService.unreadCount(userDetails));
    }

    @PostMapping("/{id}/read")
    public NotificationDto markNotificationRead(
            @PathVariable @NotNull UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return notificationService.markRead(id, userDetails);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Integer> markAllNotificationsRead(@AuthenticationPrincipal UserDetails userDetails) {
        return Map.of("updated", notificationService.markAllRead(userDetails));
    }
}
