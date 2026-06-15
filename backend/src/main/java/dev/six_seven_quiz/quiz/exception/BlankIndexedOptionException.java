package dev.six_seven_quiz.quiz.exception;

public class BlankIndexedOptionException extends RuntimeException {
    public int getOptionIndex() {
        return optionIndex;
    }

    private final int optionIndex;
    public BlankIndexedOptionException(int optionIndex) {
        super();
        this.optionIndex = optionIndex;
    }
}
