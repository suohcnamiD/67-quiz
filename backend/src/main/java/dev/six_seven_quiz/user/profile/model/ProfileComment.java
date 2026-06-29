package dev.six_seven_quiz.user.profile.model;

import dev.six_seven_quiz.user.ApplicationUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile_comments")
public class ProfileComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false)
    private ApplicationUser target;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false)
    private ApplicationUser author;

    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProfileComment() {}

    public ProfileComment(ApplicationUser target, ApplicationUser author, String body) {
        this.target = target;
        this.author = author;
        this.body = body;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public ApplicationUser getTarget() { return target; }
    public ApplicationUser getAuthor() { return author; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
