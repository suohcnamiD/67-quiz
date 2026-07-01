package dev.six_seven_quiz.authorization;

import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * On startup, ensure every username configured in {@code app.admin.usernames}
 * has the ADMIN role. The ADMIN row itself is seeded in changelog 011, so we
 * only need to attach it to the users here.
 *
 * Runs at ApplicationReadyEvent (rather than {@code @PostConstruct}) so
 * Liquibase has already applied the schema by the time we look up the ADMIN
 * row. If a listed user doesn't exist yet we simply skip — they'll be
 * promoted the next time the app starts after they register.
 */
@Component
public class AdminBootstrapper {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapper.class);

    private final AdminProperties properties;
    private final ApplicationUserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminBootstrapper(AdminProperties properties, ApplicationUserRepository userRepository, RoleRepository roleRepository) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void promoteConfiguredAdmins() {
        if (properties.usernames().isEmpty()) return;
        Optional<Role> adminRoleOpt = roleRepository.findByName("ADMIN");
        if (adminRoleOpt.isEmpty()) {
            log.warn("ADMIN role missing — skipping admin promotion. Did changelog 011 apply?");
            return;
        }
        Role adminRole = adminRoleOpt.get();
        for (String username : properties.usernames()) {
            String cleaned = username == null ? null : username.trim();
            if (cleaned == null || cleaned.isEmpty()) continue;
            Optional<ApplicationUser> userOpt = userRepository.findByUsername(cleaned);
            if (userOpt.isEmpty()) {
                log.info("Admin bootstrap: user '{}' not found, skipping.", cleaned);
                continue;
            }
            ApplicationUser user = userOpt.get();
            Set<Role> current = user.getRoles();
            if (current != null && current.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()))) continue;
            Set<Role> next = current == null ? new HashSet<>() : new HashSet<>(current);
            next.add(adminRole);
            user.setRoles(next);
            userRepository.save(user);
            log.info("Admin bootstrap: promoted '{}' to ADMIN.", cleaned);
        }
    }
}
