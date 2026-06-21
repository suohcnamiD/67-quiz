#!/usr/bin/env python3
"""Seed demo quizzes against a local backend.

Usage:
    python3 scripts/seed-quiz.py [--username NAME] [--password PASS]

Creates (or signs in as) a user, then creates two fresh quizzes:

  - "Sampler 10": 10 questions with 4 options each, evenly distributed
    over 0..4 correct options. Use this to exercise the result screen.
  - "General trivia": 10 real general-knowledge questions with a
    semi-even mix of 1..4 correct options (no zero-correct questions
    here because that would be cruel for actual trivia).
"""
import argparse
import json
import sys
import urllib.error
import urllib.request
from http.cookiejar import CookieJar

BASE = "http://localhost:8080"

# Sampler: deterministic 0..4 correct, two questions per bucket.
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
        opts = [
            {"text": f"Option {chr(ord('A') + i)}", "correct": i < n_correct}
            for i in range(4)
        ]
        SAMPLER_QUESTIONS.append((f"{label} (#{variant})", opts))


def opt(text, correct=False):
    return {"text": text, "correct": correct}


# General trivia: 10 real questions, semi-even distribution of correct counts.
# Counts: 2× 1-correct, 3× 2-correct, 3× 3-correct, 2× 4-correct.
TRIVIA_QUESTIONS = [
    # ----- 1 correct -----
    ("What is the capital of Australia?", [
        opt("Sydney"),
        opt("Melbourne"),
        opt("Canberra", correct=True),
        opt("Perth"),
    ]),
    ("Which planet has the most moons (as of 2024)?", [
        opt("Jupiter"),
        opt("Saturn", correct=True),
        opt("Uranus"),
        opt("Neptune"),
    ]),

    # ----- 2 correct -----
    ("Which of these are primary colours in additive (RGB) mixing?", [
        opt("Red", correct=True),
        opt("Yellow"),
        opt("Green", correct=True),
        opt("Magenta"),
    ]),
    ("Which of these languages are written right-to-left?", [
        opt("Arabic", correct=True),
        opt("Mandarin"),
        opt("Hebrew", correct=True),
        opt("Japanese"),
    ]),
    ("Which countries border France on land?", [
        opt("Spain", correct=True),
        opt("Portugal"),
        opt("Belgium", correct=True),
        opt("Netherlands"),
    ]),

    # ----- 3 correct -----
    ("Which of these are noble gases?", [
        opt("Argon", correct=True),
        opt("Nitrogen"),
        opt("Neon", correct=True),
        opt("Krypton", correct=True),
    ]),
    ("Which of these are programming languages designed in the 1990s?", [
        opt("Python", correct=True),
        opt("C"),
        opt("Java", correct=True),
        opt("JavaScript", correct=True),
    ]),
    ("Which composers are associated with the Classical period (c. 1750–1820)?", [
        opt("Wolfgang Amadeus Mozart", correct=True),
        opt("Johann Sebastian Bach"),
        opt("Joseph Haydn", correct=True),
        opt("Ludwig van Beethoven", correct=True),
    ]),

    # ----- 4 correct -----
    ("Which of these are member states of the European Union?", [
        opt("Germany", correct=True),
        opt("France", correct=True),
        opt("Italy", correct=True),
        opt("Spain", correct=True),
    ]),
    ("Which of these are oceans on Earth?", [
        opt("Pacific", correct=True),
        opt("Atlantic", correct=True),
        opt("Indian", correct=True),
        opt("Arctic", correct=True),
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

    for i, (text, options) in enumerate(questions, start=1):
        n_correct = sum(1 for o in options if o["correct"])
        code, q = req(opener, "POST", "/api/question", {
            "quizId": quiz_id, "text": text, "options": options,
        })
        if code != 200:
            print(f"  Question {i} failed: {code} {q}", file=sys.stderr)
            sys.exit(1)
        print(f"  Q{i:2d} ({n_correct}/{len(options)} correct): {text}")

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
    print(f"Sampler quiz:  http://localhost:5173/app/quiz/{sampler_id}")
    print(f"Trivia quiz:   http://localhost:5173/app/quiz/{trivia_id}")


if __name__ == "__main__":
    main()
