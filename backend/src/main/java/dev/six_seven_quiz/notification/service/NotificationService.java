package dev.six_seven_quiz.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.six_seven_quiz.notification.dto.NotificationDto;
import dev.six_seven_quiz.notification.exception.NotificationNotFoundException;
import dev.six_seven_quiz.notification.model.Notification;
import dev.six_seven_quiz.notification.model.NotificationType;
import dev.six_seven_quiz.notification.repository.NotificationRepository;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    public static final int PER_PAGE = 20;

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final ApplicationUserService applicationUserService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NotificationService(
            NotificationRepository notificationRepository,
            ApplicationUserService applicationUserService
    ) {
        this.notificationRepository = notificationRepository;
        this.applicationUserService = applicationUserService;
    }

    /**
     * Push a notification. Internal — callers are other services emitting on
     * domain events. Silently drops if recipient == actor when caller passed
     * the actor in payload (the FE-facing rule is "no self-notifications"),
     * but the caller is the source of truth for that gate, so this method
     * just persists what it's given.
     */
    @Transactional
    public Notification create(ApplicationUser recipient, NotificationType type, Map<String, Object> payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise notification payload for type {}", type, e);
            json = "{}";
        }
        return notificationRepository.save(new Notification(recipient, type, json));
    }

    @Transactional
    public Page<NotificationDto> list(UserDetails callerDetails, int page) {
        ApplicationUser me = applicationUserService.getAuthenticatedUserFromDetails(callerDetails);
        Pageable pageable = PageRequest.of(page, PER_PAGE);
        return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(me.getId(), pageable)
                .map(this::toDto);
    }

    @Transactional
    public long unreadCount(UserDetails callerDetails) {
        ApplicationUser me = applicationUserService.getAuthenticatedUserFromDetails(callerDetails);
        return notificationRepository.countByRecipient_IdAndReadAtIsNull(me.getId());
    }

    @Transactional
    public NotificationDto markRead(UUID notificationId, UserDetails callerDetails) {
        ApplicationUser me = applicationUserService.getAuthenticatedUserFromDetails(callerDetails);
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (!n.getRecipient().getId().equals(me.getId())) {
            throw new NotificationNotFoundException(notificationId);
        }
        n.markRead();
        return toDto(n);
    }

    @Transactional
    public int markAllRead(UserDetails callerDetails) {
        ApplicationUser me = applicationUserService.getAuthenticatedUserFromDetails(callerDetails);
        return notificationRepository.markAllRead(me.getId(), Instant.now());
    }

    private NotificationDto toDto(Notification n) {
        Map<String, Object> parsed;
        try {
            parsed = n.getPayload() == null || n.getPayload().isEmpty()
                    ? Map.of()
                    : objectMapper.readValue(n.getPayload(), new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Notification {} has unparseable payload", n.getId());
            parsed = new HashMap<>();
        }
        return new NotificationDto(
                n.getId(),
                n.getType(),
                parsed,
                n.isRead(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}
