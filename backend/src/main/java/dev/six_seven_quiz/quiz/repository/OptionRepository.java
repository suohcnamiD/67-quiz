package dev.six_seven_quiz.quiz.repository;

import dev.six_seven_quiz.quiz.model.Option;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OptionRepository extends JpaRepository<Option, UUID> {
}