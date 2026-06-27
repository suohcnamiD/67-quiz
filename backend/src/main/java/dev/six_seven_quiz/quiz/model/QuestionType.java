package dev.six_seven_quiz.quiz.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "How a question is presented and scored.")
public enum QuestionType {
    /** Exactly one correct option. Scored binary: 1 point if the user's pick matches the correct option, else 0. */
    SINGLE_CHOICE,
    /** Zero to N correct options. +1 per correctly classified option. Max score = option count. */
    MULTI_CHOICE,
}
