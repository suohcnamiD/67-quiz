package dev.six_seven_quiz.notification.model;

import dev.six_seven_quiz.user.ApplicationUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private ApplicationUser recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    /**
     * Type-specific context, serialised as a small JSON blob. Each consumer
     * (the FE renderer) knows which keys to look for based on the type.
     * Examples:
     *   COMMENT_RECEIVED → {actor: "alice", commentId: "..."}
     *   QUIZ_RATED       → {actor: "alice", quizId: "...", score: 8}
     *   QUIZ_ATTEMPTED   → {actor: "alice", quizId: "...", attemptId: "..."}
     *   RANK_DROPPED     → {board: "PLAYERS", from: 3, to: 7}
     */
    @Column(name = "payload", nullable = false, length = 2000)
    private String payload;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {}

    public Notification(ApplicationUser recipient, NotificationType type, String payload) {
        this.recipient = recipient;
        this.type = type;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public void markRead() {
        if (this.readAt == null) this.readAt = Instant.now();
    }

    public UUID getId() { return id; }
    public ApplicationUser getRecipient() { return recipient; }
    public NotificationType getType() { return type; }
    public String getPayload() { return payload; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isRead() { return readAt != null; }
}
