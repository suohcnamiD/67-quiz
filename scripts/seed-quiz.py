#!/usr/bin/env python3
"""Seed demo quizzes against a local backend.

Usage:
    python3 scripts/seed-quiz.py [--username NAME] [--password PASS]

Creates (or signs in as) a user, then creates three fresh quizzes:

  - "Sampler 10": multi-choice questions evenly distributed over 0..4 correct
    options. Exercises the multi-choice scoring rule (+1 per option).
  - "General trivia": multi-choice general-knowledge questions with a
    semi-even mix of 1..4 correct.
  - "Mixed types": five single-choice + five multi-choice questions for
    exercising the new question-type UI end to end.
"""
import argparse
import json
import sys
import urllib.error
import urllib.request
from http.cookiejar import CookieJar

BASE = "http://localhost:8080"


def opt(text, correct=False):
    return {"text": text, "correct": correct}


# Sampler: multi-choice, deterministic 0..4 correct, two questions per bucket.
SAMPLER_QUESTIONS = []
for n_correct in range(5):
    for variant in (1, 2):
        label = {
            0: "Pick none — every answer here is a distractor",
            1: "Exactly one option is correct",
            2: "Two options are correct",
            3: "Three options are correct, one is a distractor",
            4: "All four options are correct",
        }[n_correct]
        SAMPLER_QUESTIONS.append((
            f"{label} (#{variant})",
            "MULTI_CHOICE",
            [opt(f"Option {chr(ord('A') + i)}", correct=i < n_correct) for i in range(4)],
        ))


# General trivia: real questions, multi-choice, semi-even 1..4 correct distribution.
TRIVIA_QUESTIONS = [
    ("What is the capital of Australia?", "MULTI_CHOICE", [
        opt("Sydney"), opt("Melbourne"), opt("Canberra", correct=True), opt("Perth"),
    ]),
    ("Which planet has the most moons (as of 2024)?", "MULTI_CHOICE", [
        opt("Jupiter"), opt("Saturn", correct=True), opt("Uranus"), opt("Neptune"),
    ]),
    ("Which of these are primary colours in additive (RGB) mixing?", "MULTI_CHOICE", [
        opt("Red", correct=True), opt("Yellow"), opt("Green", correct=True), opt("Magenta"),
    ]),
    ("Which of these languages are written right-to-left?", "MULTI_CHOICE", [
        opt("Arabic", correct=True), opt("Mandarin"), opt("Hebrew", correct=True), opt("Japanese"),
    ]),
    ("Which countries border France on land?", "MULTI_CHOICE", [
        opt("Spain", correct=True), opt("Portugal"), opt("Belgium", correct=True), opt("Netherlands"),
    ]),
    ("Which of these are noble gases?", "MULTI_CHOICE", [
        opt("Argon", correct=True), opt("Nitrogen"), opt("Neon", correct=True), opt("Krypton", correct=True),
    ]),
    ("Which of these are programming languages designed in the 1990s?", "MULTI_CHOICE", [
        opt("Python", correct=True), opt("C"), opt("Java", correct=True), opt("JavaScript", correct=True),
    ]),
    ("Which composers are associated with the Classical period (c. 1750–1820)?", "MULTI_CHOICE", [
        opt("Wolfgang Amadeus Mozart", correct=True),
        opt("Johann Sebastian Bach"),
        opt("Joseph Haydn", correct=True),
        opt("Ludwig van Beethoven", correct=True),
    ]),
    ("Which of these are member states of the European Union?", "MULTI_CHOICE", [
        opt("Germany", correct=True), opt("France", correct=True), opt("Italy", correct=True), opt("Spain", correct=True),
    ]),
    ("Which of these are oceans on Earth?", "MULTI_CHOICE", [
        opt("Pacific", correct=True), opt("Atlantic", correct=True), opt("Indian", correct=True), opt("Arctic", correct=True),
    ]),
]


# Mixed types: 5 single-choice + 5 multi-choice. Single-choice questions
# score binary; multi-choice score per-option. Demonstrates both UI modes.
MIXED_QUESTIONS = [
    # ----- Single-choice (one correct, scored 0 or 1 per question) -----
    ("What's the largest planet in the solar system?", "SINGLE_CHOICE", [
        opt("Earth"), opt("Saturn"), opt("Jupiter", correct=True), opt("Mars"),
    ]),
    ("Which year did the Berlin Wall fall?", "SINGLE_CHOICE", [
        opt("1987"), opt("1989", correct=True), opt("1991"), opt("1993"),
    ]),
    ("Who wrote the play 'Hamlet'?", "SINGLE_CHOICE", [
        opt("Charles Dickens"), opt("Mark Twain"),
        opt("William Shakespeare", correct=True), opt("Oscar Wilde"),
    ]),
    ("What is the chemical symbol for gold?", "SINGLE_CHOICE", [
        opt("Go"), opt("Gd"), opt("Au", correct=True), opt("Ag"),
    ]),
    ("Which artist painted the Mona Lisa?", "SINGLE_CHOICE", [
        opt("Michelangelo"), opt("Raphael"),
        opt("Leonardo da Vinci", correct=True), opt("Donatello"),
    ]),

    # ----- Multi-choice (0..N correct, scored per option) -----
    ("Which of these are Scandinavian countries?", "MULTI_CHOICE", [
        opt("Norway", correct=True), opt("Sweden", correct=True),
        opt("Finland"), opt("Denmark", correct=True),
    ]),
    ("Which of these are primary colours in subtractive (CMY) mixing?", "MULTI_CHOICE", [
        opt("Cyan", correct=True), opt("Red"),
        opt("Magenta", correct=True), opt("Yellow", correct=True),
    ]),
    ("Which of these were members of The Beatles?", "MULTI_CHOICE", [
        opt("John Lennon", correct=True), opt("Mick Jagger"),
        opt("Paul McCartney", correct=True), opt("Ringo Starr", correct=True),
    ]),
    ("Which of these are mammals?", "MULTI_CHOICE", [
        opt("Dolphin", correct=True), opt("Shark"),
        opt("Bat", correct=True), opt("Eagle"),
    ]),
    ("Which of these are valid HTTP methods?", "MULTI_CHOICE", [
        opt("GET", correct=True), opt("FETCH"),
        opt("DELETE", correct=True), opt("PATCH", correct=True),
    ]),
]


def build_opener():
    jar = CookieJar()
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar)), jar


def req(opener, method, path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(
        f"{BASE}{path}",
        data=data,
        method=method,
        headers={"Content-Type": "application/json"} if body is not None else {},
    )
    try:
        with opener.open(request) as r:
            raw = r.read().decode() or "{}"
            return r.status, json.loads(raw) if raw.strip() else {}
    except urllib.error.HTTPError as e:
        raw = e.read().decode() or ""
        try:
            return e.code, json.loads(raw)
        except json.JSONDecodeError:
            return e.code, {"raw": raw}


def create_quiz(opener, name, duration, questions):
    code, quiz = req(opener, "POST", "/api/quiz", {
        "quizName": name,
        "quizDuration": duration,
    })
    if code != 200:
        print(f"Quiz create failed for {name}: {code} {quiz}", file=sys.stderr)
        sys.exit(1)
    quiz_id = quiz["id"]
    print(f"Created quiz '{name}' ({quiz_id})")

    for i, (text, qtype, options) in enumerate(questions, start=1):
        n_correct = sum(1 for o in options if o["correct"])
        code, q = req(opener, "POST", "/api/question", {
            "quizId": quiz_id, "text": text, "type": qtype, "options": options,
        })
        if code != 200:
            print(f"  Question {i} failed: {code} {q}", file=sys.stderr)
            sys.exit(1)
        tag = "SC" if qtype == "SINGLE_CHOICE" else "MC"
        print(f"  Q{i:2d} [{tag}] ({n_correct}/{len(options)} correct): {text}")

    return quiz_id


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--username", default="sampler")
    ap.add_argument("--password", default="Passw0rd1")
    args = ap.parse_args()

    opener, _ = build_opener()

    # Try register; if the user already exists, sign in instead.
    code, _ = req(opener, "POST", "/api/authentication/register", {
        "username": args.username, "password": args.password,
    })
    if code == 200:
        print(f"Registered new user {args.username}")
    else:
        code, _ = req(opener, "POST", "/api/authentication/login", {
            "username": args.username, "password": args.password,
        })
        if code != 200:
            print(f"Could not sign in: status {code}", file=sys.stderr)
            sys.exit(1)
        print(f"Signed in as existing user {args.username}")

    sampler_id = create_quiz(opener, "Sampler 10", "PT15M", SAMPLER_QUESTIONS)
    print()
    trivia_id = create_quiz(opener, "General trivia", "PT10M", TRIVIA_QUESTIONS)
    print()
    mixed_id = create_quiz(opener, "Mixed types", "PT10M", MIXED_QUESTIONS)

    print()
    print(f"Sampler quiz:  http://localhost:5173/app/quiz/{sampler_id}")
    print(f"Trivia quiz:   http://localhost:5173/app/quiz/{trivia_id}")
    print(f"Mixed quiz:    http://localhost:5173/app/quiz/{mixed_id}")


if __name__ == "__main__":
    main()
