package dev.six_seven_quiz.user.profile.controller;

import dev.six_seven_quiz.user.profile.dto.UpdateProfileRequest;
import dev.six_seven_quiz.user.profile.dto.UserProfileDto;
import dev.six_seven_quiz.user.profile.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public UserProfileDto getOwnProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return userProfileService.getOwnProfile(userDetails);
    }

    @PatchMapping("/me")
    public UserProfileDto updateOwnProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid UpdateProfileRequest request
    ) {
        return userProfileService.updateOwnProfile(userDetails, request);
    }

    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileDto uploadAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("file") MultipartFile file
    ) {
        return userProfileService.uploadAvatar(userDetails, file);
    }

    @DeleteMapping("/me/avatar")
    public UserProfileDto deleteAvatar(@AuthenticationPrincipal UserDetails userDetails) {
        return userProfileService.deleteAvatar(userDetails);
    }

    @GetMapping("/{username}")
    public UserProfileDto getProfileByUsername(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return userProfileService.getProfileByUsername(username, userDetails);
    }

    @GetMapping(value = "/{username}/avatar", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getAvatar(@PathVariable String username) {
        byte[] bytes = userProfileService.readAvatarByUsername(username);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=300, public")
                .body(bytes);
    }
}
