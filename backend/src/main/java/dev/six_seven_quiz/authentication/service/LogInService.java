package dev.six_seven_quiz.authentication.service;

import dev.six_seven_quiz.authentication.dto.request.LoginRequest;
import dev.six_seven_quiz.authentication.exception.UserNotAuthenticatedException;
import dev.six_seven_quiz.shared.component.Utilities;
import dev.six_seven_quiz.user.ApplicationUserRepository;
import dev.six_seven_quiz.user.ApplicationUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogInService {

    private final ApplicationUserRepository applicationUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final Utilities utilities;
    private final ApplicationUserService applicationUserService;

    public LogInService(ApplicationUserRepository applicationUserRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, Utilities utilities, ApplicationUserService applicationUserService) {
        this.applicationUserRepository = applicationUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.utilities = utilities;
        this.applicationUserService = applicationUserService;
    }

    public void verifyUserAuthenticated(UserDetails userDetails) {
        if (userDetails == null) throw new UserNotAuthenticatedException();
    }

    public List<String> getUserRoles(UserDetails userDetails) {
        if (userDetails == null) return List.of();
        return applicationUserService.getUserRolesAsStringList(userDetails);
    }

    public List<String> loginUser(LoginRequest request, HttpServletRequest httpRequest) throws UserNotAuthenticatedException {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );
        } catch (Exception e) {
            throw new UserNotAuthenticatedException();
        }

        if (!authentication.isAuthenticated()) throw new UserNotAuthenticatedException();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails == null ? List.of() : applicationUserService.getUserRolesAsStringList(userDetails);
    }
}
