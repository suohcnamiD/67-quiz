package dev.six_seven_quiz.quiz.controller;

import dev.six_seven_quiz.quiz.service.QuizImageService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Cover / question / option image management. Upload + delete require the
 * caller to own the quiz (enforced inside QuizImageService). GET endpoints
 * are public so any signed-in user taking the quiz can fetch the images.
 */
@RestController
public class QuizImageController {

    private final QuizImageService imageService;

    public QuizImageController(QuizImageService imageService) {
        this.imageService = imageService;
    }

    // ----- Quiz cover ------------------------------------------------------

    @PutMapping(value = "/quiz/{quizId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadQuizCover(
            @PathVariable @NotNull UUID quizId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file
    ) {
        imageService.uploadCover(quizId, userDetails, file);
    }

    @DeleteMapping("/quiz/{quizId}/cover")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuizCover(
            @PathVariable @NotNull UUID quizId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        imageService.deleteCover(quizId, userDetails);
    }

    @GetMapping(value = "/quiz/{quizId}/cover", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getQuizCover(@PathVariable @NotNull UUID quizId) {
        byte[] bytes = imageService.readCover(quizId);
        return ResponseEntity.ok()
                .header("Cache-Control", "max-age=300, public")
                .body(bytes);
    }

    // ----- Question image --------------------------------------------------

    @PutMapping(value = "/question/{questionId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadQuestionImage(
            @PathVariable @NotNull UUID questionId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file
    ) {
        imageService.uploadQuestionImage(questionId, userDetails, file);
    }

    @DeleteMapping("/question/{questionId}/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestionImage(
            @PathVariable @NotNull UUID questionId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        imageService.deleteQuestionImage(questionId, userDetails);
    }

    @GetMapping(value = "/question/{questionId}/image", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getQuestionImage(@PathVariable @NotNull UUID questionId) {
        byte[] bytes = imageService.readQuestionImage(questionId);
        return ResponseEntity.ok()
                .header("Cache-Control", "max-age=300, public")
                .body(bytes);
    }

    // ----- Option image ----------------------------------------------------

    @PutMapping(value = "/option/{optionId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadOptionImage(
            @PathVariable @NotNull UUID optionId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file
    ) {
        imageService.uploadOptionImage(optionId, userDetails, file);
    }

    @DeleteMapping("/option/{optionId}/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOptionImage(
            @PathVariable @NotNull UUID optionId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        imageService.deleteOptionImage(optionId, userDetails);
    }

    @GetMapping(value = "/option/{optionId}/image", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getOptionImage(@PathVariable @NotNull UUID optionId) {
        byte[] bytes = imageService.readOptionImage(optionId);
        return ResponseEntity.ok()
                .header("Cache-Control", "max-age=300, public")
                .body(bytes);
    }
}
