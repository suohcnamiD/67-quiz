package dev.six_seven_quiz.authentication;

import dev.six_seven_quiz.authentication.dto.request.LoginRequest;
import dev.six_seven_quiz.authentication.dto.request.RegistrationRequest;
import dev.six_seven_quiz.authentication.dto.response.LoginResponse;
import dev.six_seven_quiz.authentication.exception.UserNotAuthenticatedException;
import dev.six_seven_quiz.authentication.service.LogInService;
import dev.six_seven_quiz.authentication.service.RegistrationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/authentication")
public class AuthenticationController {

    private final LogInService logInService;
    private final RegistrationService registrationService;

    public AuthenticationController(LogInService logInService, RegistrationService registrationService) {
        this.logInService = logInService;
        this.registrationService = registrationService;
    }

    // Automatically logs in the user as well
    @SecurityRequirements({})
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegistrationRequest request, HttpServletRequest httpRequest) {
        List<String> userRoles = registrationService.registerUser(request.username(), request.password(), httpRequest);
        return ResponseEntity.ok(new LoginResponse(userRoles));
    }

    @SecurityRequirements({})
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest)
            throws UserNotAuthenticatedException {
        List<String> userRoles = logInService.loginUser(request, httpRequest);
        return ResponseEntity.ok(new LoginResponse(userRoles));
    }
}
