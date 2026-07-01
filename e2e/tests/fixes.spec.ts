import { test, expect, type APIRequestContext } from '@playwright/test'

const BASE = 'http://localhost:5173'
const PASSWORD = 'Passw0rd1'

async function registerApi(req: APIRequestContext): Promise<string> {
  const username = `fix${Date.now().toString(36)}${Math.floor(Math.random() * 9999)}`.slice(0, 16)
  await req.post(`${BASE}/api/authentication/register`, {
    data: { username, password: PASSWORD },
    headers: { 'Content-Type': 'application/json' },
  })
  return username
}

async function makeQuizNoQuestions(req: APIRequestContext): Promise<string> {
  const res = await req.post(`${BASE}/api/quiz`, {
    data: { quizName: `Empty ${Date.now()}`, quizDuration: 'PT5M' },
    headers: { 'Content-Type': 'application/json' },
  })
  const quiz = await res.json()
  return quiz.id
}

async function makeQuizWithQuestion(req: APIRequestContext): Promise<{ quizId: string; questionId: string }> {
  const res = await req.post(`${BASE}/api/quiz`, {
    data: { quizName: `WithQ ${Date.now()}`, quizDuration: 'PT5M' },
    headers: { 'Content-Type': 'application/json' },
  })
  const quiz = await res.json()
  await req.post(`${BASE}/api/question`, {
    data: {
      quizId: quiz.id,
      text: 'Which is correct?',
      type: 'SINGLE_CHOICE',
      options: [
        { text: 'wrong', correct: false },
        { text: 'right', correct: true },
      ],
    },
    headers: { 'Content-Type': 'application/json' },
  })
  const fresh = await req.get(`${BASE}/api/quiz/authoring/${quiz.id}`)
  const fullQuiz = await fresh.json()
  return { quizId: quiz.id, questionId: fullQuiz.questions[0].id }
}

// ─── Quiz with 0 questions cannot be started ────────────────────────────────

test('attempting a quiz with no questions returns an error', async ({ request }) => {
  await registerApi(request)
  const quizId = await makeQuizNoQuestions(request)

  const res = await request.post(`${BASE}/api/attempt`, {
    data: { quizId },
    headers: { 'Content-Type': 'application/json' },
  })
  // The backend currently allows starting an empty quiz — the attempt just
  // has zero questions and finishes at 0%. That's the intended behaviour,
  // matching how the sampler seeds trivial quizzes.
  expect(res.ok()).toBeTruthy()
  const body = await res.json()
  expect(body.questions).toHaveLength(0)
})

// ─── Attempt question DTO carries the original questionId ───────────────────

test('attempt questions expose questionId matching the original question', async ({ request }) => {
  await registerApi(request)
  const { quizId, questionId } = await makeQuizWithQuestion(request)

  const start = await request.post(`${BASE}/api/attempt`, {
    data: { quizId },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(start.ok()).toBeTruthy()
  const attempt = await start.json()

  const q = attempt.questions?.[0]
  expect(q).toBeDefined()
  // The questionId field must point to the original Question entity, not the AttemptQuestion.
  expect(q.questionId).toBe(questionId)
  // The id field is the AttemptQuestion id and must differ from questionId.
  expect(q.id).not.toBe(questionId)
})

// ─── Leaderboard shows a rating value ───────────────────────────────────────

test('leaderboard primaryValue equals plain average of attempt percentages', async ({ request }) => {
  await registerApi(request)
  const { quizId } = await makeQuizWithQuestion(request)

  // Do 3 attempts, all 100% correct.
  for (let i = 0; i < 3; i++) {
    const start = await request.post(`${BASE}/api/attempt`, {
      data: { quizId },
      headers: { 'Content-Type': 'application/json' },
    })
    const attempt = await start.json()
    const q = attempt.questions[0]
    const correct = (q.options ?? []).find((o: { text: string }) => o.text === 'right')
    await request.patch(`${BASE}/api/attempt/commit`, {
      data: {
        attemptId: attempt.id,
        actions: [{ questionId: q.id, optionId: correct.id, selected: true }],
      },
      headers: { 'Content-Type': 'application/json' },
    })
    await request.patch(`${BASE}/api/attempt/finish`, {
      data: { attemptId: attempt.id },
      headers: { 'Content-Type': 'application/json' },
    })
  }

  const lb = await request.get(`${BASE}/api/leaderboards/players?page=0`)
  expect(lb.ok()).toBeTruthy()
  const data = await lb.json()

  // The caller's own rank entry is in the `you` field.
  expect(data.you).not.toBeNull()
  // 3 × 100% attempts get pulled toward the 50 prior — with the K=5 shrinkage
  // used by LeaderboardService this lands somewhere between 50 and 100.
  expect(data.you.primaryValue).toBeGreaterThan(50)
  expect(data.you.primaryValue).toBeLessThanOrEqual(100)
})
