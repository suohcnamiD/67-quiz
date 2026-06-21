package dev.six_seven_quiz.user.profile.service;

import dev.six_seven_quiz.quiz.model.Attempt;
import dev.six_seven_quiz.quiz.repository.QuizAttemptRepository;
import dev.six_seven_quiz.quiz.repository.QuizRepository;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserRepository;
import dev.six_seven_quiz.user.ApplicationUserService;
import dev.six_seven_quiz.user.profile.component.mapper.UserProfileMapper;
import dev.six_seven_quiz.user.profile.dto.UpdateProfileRequest;
import dev.six_seven_quiz.user.profile.dto.UserProfileDto;
import dev.six_seven_quiz.user.profile.exception.InvalidDisplayNameException;
import dev.six_seven_quiz.user.profile.exception.UnknownUsernameException;
import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class UserProfileService {

    private static final int DISPLAY_NAME_MIN = 1;
    private static final int DISPLAY_NAME_MAX = 32;
    private static final int BIO_MAX = 280;

    private final ApplicationUserService applicationUserService;
    private final ApplicationUserRepository applicationUserRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserProfileMapper userProfileMapper;
    private final AvatarStorageService avatarStorageService;

    public UserProfileService(
            ApplicationUserService applicationUserService,
            ApplicationUserRepository applicationUserRepository,
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository,
            UserProfileMapper userProfileMapper,
            AvatarStorageService avatarStorageService
    ) {
        this.applicationUserService = applicationUserService;
        this.applicationUserRepository = applicationUserRepository;
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.userProfileMapper = userProfileMapper;
        this.avatarStorageService = avatarStorageService;
    }

    @Transactional
    public UserProfileDto getOwnProfile(UserDetails userDetails) {
        ApplicationUser me = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        return toProfile(me, me);
    }

    @Transactional
    public UserProfileDto getProfileByUsername(String username, UserDetails callerDetails) {
        ApplicationUser caller = applicationUserService.getAuthenticatedUserFromDetails(callerDetails);
        ApplicationUser target = applicationUserRepository.findByUsername(username)
                .orElseThrow(() -> new UnknownUsernameException(username));
        return toProfile(target, caller);
    }

    @Transactional
    public UserProfileDto updateOwnProfile(UserDetails userDetails, UpdateProfileRequest request) {
        ApplicationUser me = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        if (request.displayName() != null) {
            String trimmed = request.displayName().trim();
            if (trimmed.length() < DISPLAY_NAME_MIN || trimmed.length() > DISPLAY_NAME_MAX) {
                throw new InvalidDisplayNameException(
                        "Display name must be " + DISPLAY_NAME_MIN + "–" + DISPLAY_NAME_MAX + " characters"
                );
            }
            me.setDisplayName(trimmed);
        }
        if (request.bio() != null) {
            String bio = request.bio();
            if (bio.length() > BIO_MAX) {
                throw new InvalidDisplayNameException(
                        "Bio must be at most " + BIO_MAX + " characters"
                );
            }
            me.setBio(bio.isEmpty() ? null : bio);
        }
        applicationUserRepository.save(me);
        return toProfile(me, me);
    }

    @Transactional
    public UserProfileDto uploadAvatar(UserDetails userDetails, MultipartFile file) {
        ApplicationUser me = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        String path = avatarStorageService.store(me, file);
        me.setAvatarPath(path);
        applicationUserRepository.save(me);
        return toProfile(me, me);
    }

    @Transactional
    public UserProfileDto deleteAvatar(UserDetails userDetails) {
        ApplicationUser me = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        avatarStorageService.delete(me);
        me.setAvatarPath(null);
        applicationUserRepository.save(me);
        return toProfile(me, me);
    }

    @Transactional
    public byte[] readAvatarByUsername(String username) {
        ApplicationUser user = applicationUserRepository.findByUsername(username)
                .orElseThrow(() -> new UnknownUsernameException(username));
        return avatarStorageService.read(user);
    }

    private UserProfileDto toProfile(ApplicationUser target, ApplicationUser caller) {
        int quizzesAuthored = quizRepository.countByAuthor(target);
        int attemptsTaken = quizAttemptRepository.countByUserAndFinishedIsTrue(target);
        Integer averageScorePercent = attemptsTaken == 0
                ? null
                : computeAverageScorePercent(target);
        return userProfileMapper.toDto(
                target,
                quizzesAuthored,
                attemptsTaken,
                averageScorePercent,
                target.equals(caller)
        );
    }

    private Integer computeAverageScorePercent(ApplicationUser user) {
        List<Attempt> attempts = quizAttemptRepository.findAllByUserAndFinishedIsTrue(user);
        if (attempts.isEmpty()) return null;
        double sumPercent = 0;
        int counted = 0;
        for (Attempt attempt : attempts) {
            int max = attempt.getMaximumScore();
            if (max == 0) continue; // Skip attempts on empty quizzes — they'd divide by zero.
            sumPercent += (100.0 * attempt.getEarnedScore()) / max;
            counted++;
        }
        if (counted == 0) return null;
        return (int) Math.round(sumPercent / counted);
    }
}
