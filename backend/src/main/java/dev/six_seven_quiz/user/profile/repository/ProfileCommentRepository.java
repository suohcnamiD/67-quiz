package dev.six_seven_quiz.user.profile.repository;

import dev.six_seven_quiz.user.profile.model.ProfileComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProfileCommentRepository extends JpaRepository<ProfileComment, UUID> {

    Page<ProfileComment> findByTarget_UsernameOrderByCreatedAtDesc(String username, Pageable pageable);
}
