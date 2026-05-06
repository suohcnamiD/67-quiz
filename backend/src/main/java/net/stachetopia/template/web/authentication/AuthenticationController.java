package net.stachetopia.template.web.authentication;

import net.stachetopia.template.web.authentication.dto.request.LoginRequest;
import net.stachetopia.template.web.authentication.dto.request.RegistrationRequest;
import net.stachetopia.template.web.authentication.dto.response.LoginResponse;
import net.stachetopia.template.web.authentication.service.LogInService;
import net.stachetopia.template.web.authentication.service.RegistrationService;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> getAuthenticationState(@AuthenticationPrincipal @Nullable UserDetails userDetails) {
        logInService.verifyUserAuthenticated(userDetails);
        return ResponseEntity.ok(new LoginResponse(logInService.getUserRoles(userDetails)));
    }

    // Automatically logs in the user as well
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegistrationRequest request, HttpServletRequest httpRequest) {
        List<String> userRoles = registrationService.registerUser(request.username(), request.password(), httpRequest);
        return ResponseEntity.ok(new LoginResponse(userRoles));
    }

    @PostMapping("/loginbak")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        List<String> userRoles = logInService.loginUser(request, httpRequest);
        return ResponseEntity.ok(new LoginResponse(userRoles));
    }

//    @PostMapping("/register")
//    public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest request) {
//        try {
//            registrationService.registerWorker(request);
//        } catch (DuplicateUsernameException e) {
//            return utilities.buildFailure(HttpStatus.CONFLICT, List.of(
//                    new Utilities.Error("USERNAME_ALREADY_TAKEN", "The username is already taken", "The username '%s' is already in use".formatted(request.username()))
//            ));
//        } catch (InvalidUsernameException e) {
//            return utilities.buildFailure(HttpStatus.BAD_REQUEST, List.of(
//                    new Utilities.Error("INVALID_USERNAME", "The username is invalid", "The username '%s' does not meet the required format".formatted(request.username()))
//            ));
//        } catch (InvalidRegistrationTokenException e) {
//            return utilities.buildFailure(HttpStatus.BAD_REQUEST, List.of(
//                    new Utilities.Error("INVALID_REGISTRATION_TOKEN", "The registration token is invalid", "The provided registration token is invalid or has expired")
//            ));
//        }
//        return utilities.buildSuccess(HttpStatus.CREATED);
//    }
}
