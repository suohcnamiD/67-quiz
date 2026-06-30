package dev.six_seven_quiz.authentication;

import dev.six_seven_quiz.authentication.dto.response.LoginResponse;
import dev.six_seven_quiz.authentication.exception.UserNotAuthenticatedException;
import dev.six_seven_quiz.authentication.service.LogInService;
import jakarta.annotation.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authentication")
public class AuthenticationStateController {

    private final LogInService logInService;

    public AuthenticationStateController(LogInService logInService) {
        this.logInService = logInService;
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> getAuthenticationState(@AuthenticationPrincipal @Nullable UserDetails userDetails)
            throws UserNotAuthenticatedException {
        logInService.verifyUserAuthenticated(userDetails);
        return ResponseEntity.ok(new LoginResponse(logInService.getUserRoles(userDetails)));
    }
}
