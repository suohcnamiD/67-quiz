package dev.six_seven_quiz.authentication;

import dev.six_seven_quiz.authentication.dto.response.LoginResponse;
import dev.six_seven_quiz.authentication.exception.UserNotAuthenticatedException;
import dev.six_seven_quiz.authentication.service.LogInService;
import dev.six_seven_quiz.shared.dto.Failure;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required — see errors[].code",
                    content = @Content(schema = @Schema(implementation = Failure.class)))
    })
    public ResponseEntity<LoginResponse> getAuthenticationState(@AuthenticationPrincipal @Nullable UserDetails userDetails)
            throws UserNotAuthenticatedException {
        logInService.verifyUserAuthenticated(userDetails);
        return ResponseEntity.ok(new LoginResponse(logInService.getUserRoles(userDetails)));
    }
}
