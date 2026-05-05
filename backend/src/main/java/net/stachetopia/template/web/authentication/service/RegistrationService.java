package net.stachetopia.template.web.authentication.service;

import net.stachetopia.template.web.authentication.dto.request.LoginRequest;
import net.stachetopia.template.web.authentication.exception.DuplicateUsernameException;
import net.stachetopia.template.web.authentication.exception.InvalidUsernameException;
import net.stachetopia.template.web.authentication.exception.PasswordTooShortException;
import net.stachetopia.template.web.user.ApplicationUser;
import net.stachetopia.template.web.user.ApplicationUserMapper;
import net.stachetopia.template.web.user.ApplicationUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class RegistrationService {
    private static final int MINIMAL_PASSWORD_LENGTH = 8;

    private final ApplicationUserRepository applicationUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ZoneId timeZoneId;
    private final ApplicationUserMapper applicationUserMapper;
    private final Pattern usernamePattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{5,16}$");
    private final LogInService logInService;

    @PersistenceContext
    private EntityManager entityManager;

    public RegistrationService(ApplicationUserRepository applicationUserRepository, PasswordEncoder passwordEncoder, ZoneId timeZoneId, ApplicationUserMapper applicationUserMapper, LogInService logInService) {
        this.applicationUserRepository = applicationUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.timeZoneId = timeZoneId;
        this.applicationUserMapper = applicationUserMapper;
        this.logInService = logInService;
    }

    private boolean isUsernameValid(String username) {
        return username != null && usernamePattern.matcher(username).matches();
    }

    public List<String> registerUser(String username, String password, HttpServletRequest request) throws DuplicateUsernameException, InvalidUsernameException, PasswordTooShortException {
        applicationUserRepository.findByUsername(username).ifPresent(_ -> {throw new DuplicateUsernameException();});
        if (!isUsernameValid(username)) throw new InvalidUsernameException();
        if (password.length() < MINIMAL_PASSWORD_LENGTH) throw new PasswordTooShortException(MINIMAL_PASSWORD_LENGTH);

        String encodedPassword = passwordEncoder.encode(password);
        ApplicationUser rawNewUser = new ApplicationUser(username, encodedPassword, username);

        applicationUserRepository.save(rawNewUser);

        entityManager.clear(); // To refetch the user
        return logInService.loginUser(new LoginRequest(username, password), request);
    }
}
