package dev.six_seven_quiz.quiz.exception;

public class InvalidRatingException extends RuntimeException {
    private final int minimum;
    private final int maximum;

    public InvalidRatingException(int minimum, int maximum) {
        super(String.format("Rating must be between %d and %d", minimum, maximum));
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public int getMinimum() {
        return minimum;
    }

    public int getMaximum() {
        return maximum;
    }
}
