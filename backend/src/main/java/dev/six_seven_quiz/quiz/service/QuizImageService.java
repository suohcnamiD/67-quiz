package dev.six_seven_quiz.quiz.service;

import dev.six_seven_quiz.quiz.exception.OptionNotFoundException;
import dev.six_seven_quiz.quiz.exception.QuestionNotFoundException;
import dev.six_seven_quiz.quiz.exception.QuizNotFoundException;
import dev.six_seven_quiz.quiz.model.Option;
import dev.six_seven_quiz.quiz.model.Question;
import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.quiz.repository.OptionRepository;
import dev.six_seven_quiz.quiz.repository.QuestionRepository;
import dev.six_seven_quiz.quiz.repository.QuizRepository;
import dev.six_seven_quiz.quiz.validator.QuizValidator;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserService;
import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class QuizImageService {

    private final ApplicationUserService applicationUserService;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final QuizImageStorageService storage;

    public QuizImageService(
            ApplicationUserService applicationUserService,
            QuizRepository quizRepository,
            QuestionRepository questionRepository,
            OptionRepository optionRepository,
            QuizImageStorageService storage
    ) {
        this.applicationUserService = applicationUserService;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.storage = storage;
    }

    // ----- Cover (per quiz) ------------------------------------------------

    @Transactional
    public void uploadCover(UUID quizId, UserDetails userDetails, MultipartFile file) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new QuizNotFoundException(quizId));
        QuizValidator.requireOwner(quiz, user);
        String path = storage.store(QuizImageStorageService.Kind.COVER, quizId, file);
        quiz.setCoverImagePath(path);
        quizRepository.save(quiz);
    }

    @Transactional
    public void deleteCover(UUID quizId, UserDetails userDetails) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new QuizNotFoundException(quizId));
        QuizValidator.requireOwner(quiz, user);
        storage.delete(QuizImageStorageService.Kind.COVER, quizId);
        quiz.setCoverImagePath(null);
        quizRepository.save(quiz);
    }

    public byte[] readCover(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new QuizNotFoundException(quizId));
        if (quiz.getCoverImagePath() == null) {
            throw new QuizNotFoundException(quizId);
        }
        return storage.read(QuizImageStorageService.Kind.COVER, quizId);
    }

    // ----- Question --------------------------------------------------------

    @Transactional
    public void uploadQuestionImage(UUID questionId, UserDetails userDetails, MultipartFile file) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Question q = questionRepository.findById(questionId).orElseThrow(() -> new QuestionNotFoundException(questionId));
        QuizValidator.requireOwner(q.getQuiz(), user);
        String path = storage.store(QuizImageStorageService.Kind.QUESTION, questionId, file);
        q.setImagePath(path);
        questionRepository.save(q);
    }

    @Transactional
    public void deleteQuestionImage(UUID questionId, UserDetails userDetails) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Question q = questionRepository.findById(questionId).orElseThrow(() -> new QuestionNotFoundException(questionId));
        QuizValidator.requireOwner(q.getQuiz(), user);
        storage.delete(QuizImageStorageService.Kind.QUESTION, questionId);
        q.setImagePath(null);
        questionRepository.save(q);
    }

    public byte[] readQuestionImage(UUID questionId) {
        Question q = questionRepository.findById(questionId).orElseThrow(() -> new QuestionNotFoundException(questionId));
        if (q.getImagePath() == null) {
            throw new QuestionNotFoundException(questionId);
        }
        return storage.read(QuizImageStorageService.Kind.QUESTION, questionId);
    }

    // ----- Option ----------------------------------------------------------

    @Transactional
    public void uploadOptionImage(UUID optionId, UserDetails userDetails, MultipartFile file) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Option opt = optionRepository.findById(optionId).orElseThrow(() -> new OptionNotFoundException(optionId));
        QuizValidator.requireOwner(opt.getQuestion().getQuiz(), user);
        String path = storage.store(QuizImageStorageService.Kind.OPTION, optionId, file);
        opt.setImagePath(path);
        optionRepository.save(opt);
    }

    @Transactional
    public void deleteOptionImage(UUID optionId, UserDetails userDetails) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Option opt = optionRepository.findById(optionId).orElseThrow(() -> new OptionNotFoundException(optionId));
        QuizValidator.requireOwner(opt.getQuestion().getQuiz(), user);
        storage.delete(QuizImageStorageService.Kind.OPTION, optionId);
        opt.setImagePath(null);
        optionRepository.save(opt);
    }

    public byte[] readOptionImage(UUID optionId) {
        Option opt = optionRepository.findById(optionId).orElseThrow(() -> new OptionNotFoundException(optionId));
        if (opt.getImagePath() == null) {
            throw new OptionNotFoundException(optionId);
        }
        return storage.read(QuizImageStorageService.Kind.OPTION, optionId);
    }
}
