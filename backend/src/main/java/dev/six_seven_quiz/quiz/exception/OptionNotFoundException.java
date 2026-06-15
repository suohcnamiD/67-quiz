package dev.six_seven_quiz.quiz.exception;

import java.util.UUID;

public class OptionNotFoundException extends RuntimeException {
    public UUID getOptionId() {
        return optionId;
    }

    private final UUID optionId;

    public OptionNotFoundException(UUID optionId) {
        super(String.format("Option with id %s not found", optionId));
        this.optionId = optionId;
    }
}
