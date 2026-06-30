import { test, expect, type APIRequestContext } from '@playwright/test'

const BASE = 'http://localhost:5173'
const PASSWORD = 'Passw0rd1'

async function register(req: APIRequestContext): Promise<string> {
  const username = `o${Date.now().toString(36)}${Math.floor(Math.random() * 10000)}`.slice(0, 16)
  const res = await req.post(`${BASE}/api/authentication/register`, {
    data: { username, password: PASSWORD },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(res.ok()).toBeTruthy()
  return username
}

async function createQuizWithQuestion(req: APIRequestContext, name: string): Promise<string> {
  const quizRes = await req.post(`${BASE}/api/quiz`, {
    data: { quizName: name, quizDuration: 'PT5M' },
    headers: { 'Content-Type': 'application/json' },
  })
  const quiz = await quizRes.json()
  await req.post(`${BASE}/api/question`, {
    data: {
      quizId: quiz.id,
      text: 'pick the truthy one',
      type: 'SINGLE_CHOICE',
      options: [
        { text: 'no', correct: false },
        { text: 'yes', correct: true },
      ],
    },
    headers: { 'Content-Type': 'application/json' },
  })
  return quiz.id
}

// quiz_attempts.quiz_id is ON DELETE SET NULL, so when an author deletes
// their quiz every attempt row of it survives with a null quiz reference.
// Both the in-progress and finished pages, and finishing the orphaned
// attempt itself, need to handle that gracefully — they used to 500.

test('listing finished attempts of a deleted quiz returns a tombstone, not 500', async ({ browser, request }) => {
  await register(request)
  const quizId = await createQuizWithQuestion(request, `tombstone finish ${Date.now()}`)

  const attemptee = await browser.newContext()
  await register(attemptee.request)
  const startRes = await attemptee.request.post(`${BASE}/api/attempt`, {
    data: { quizId }, headers: { 'Content-Type': 'application/json' },
  })
  const attempt = await startRes.json()
  await attemptee.request.patch(`${BASE}/api/attempt/finish`, {
    data: { attemptId: attempt.id }, headers: { 'Content-Type': 'application/json' },
  })

  // Author deletes the quiz. Attempt row survives with quiz_id = NULL.
  await request.delete(`${BASE}/api/quiz/${quizId}`)

  // Attempter loads their finished page — must return 200 with a tombstone.
  const page = await attemptee.request.get(`${BASE}/api/attempt/finished?page=0`)
  expect(page.ok()).toBeTruthy()
  const body = await page.json()
  const found = (body._embedded?.attempts ?? []).find((a: { id?: string }) => a.id === attempt.id)
  expect(found).toBeTruthy()
  expect(found.quiz?.name).toBe('Deleted quiz')
  expect(found.quiz?.id).toBeNull()
  await attemptee.close()
})

test('finishing an in-progress attempt whose quiz was deleted does not 500', async ({ browser, request }) => {
  await register(request)
  const quizId = await createQuizWithQuestion(request, `tombstone inflight ${Date.now()}`)

  const attemptee = await browser.newContext()
  await register(attemptee.request)
  // Start but don't finish.
  const startRes = await attemptee.request.post(`${BASE}/api/attempt`, {
    data: { quizId }, headers: { 'Content-Type': 'application/json' },
  })
  const attempt = await startRes.json()

  // Author deletes the quiz mid-attempt.
  await request.delete(`${BASE}/api/quiz/${quizId}`)

  // Attempter tries to finish — should succeed (no notification, but no 500).
  const finRes = await attemptee.request.patch(`${BASE}/api/attempt/finish`, {
    data: { attemptId: attempt.id }, headers: { 'Content-Type': 'application/json' },
  })
  expect(finRes.ok()).toBeTruthy()
  const finBody = await finRes.json()
  expect(finBody.quiz?.name).toBe('Deleted quiz')
  await attemptee.close()
})

test('in-progress page with an orphaned attempt returns 200', async ({ browser, request }) => {
  await register(request)
  const quizId = await createQuizWithQuestion(request, `tombstone inprogress ${Date.now()}`)

  const attemptee = await browser.newContext()
  await register(attemptee.request)
  const startRes = await attemptee.request.post(`${BASE}/api/attempt`, {
    data: { quizId }, headers: { 'Content-Type': 'application/json' },
  })
  const attempt = await startRes.json()

  await request.delete(`${BASE}/api/quiz/${quizId}`)

  const page = await attemptee.request.get(`${BASE}/api/attempt/in-progress?page=0`)
  expect(page.ok()).toBeTruthy()
  const body = await page.json()
  const found = (body._embedded?.attempts ?? []).find((a: { id?: string }) => a.id === attempt.id)
  expect(found).toBeTruthy()
  expect(found.quiz?.name).toBe('Deleted quiz')
  await attemptee.close()
})
