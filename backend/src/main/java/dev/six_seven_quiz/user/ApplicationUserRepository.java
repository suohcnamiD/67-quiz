package dev.six_seven_quiz.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, UUID> {
    Optional<ApplicationUser> findByUsername(String username);

    Page<ApplicationUser> findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String usernameNeedle,
            String displayNameNeedle,
            Pageable pageable
    );
}
