import { test, expect, type Page } from '@playwright/test'

async function registerAndLogin(page: Page): Promise<string> {
  const username = `q${Date.now().toString(36)}${Math.floor(Math.random() * 100)}`.slice(0, 16)
  const password = 'Passw0rd1'
  await page.goto('/register')
  await page.locator('input[autocomplete="username"]').fill(username)
  await page.locator('input[autocomplete="new-password"]').fill(password)
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/app/, { timeout: 15_000 })
  return username
}

async function createQuiz(page: Page, name: string): Promise<string> {
  const res = await page.request.post('http://localhost:5173/api/quiz', {
    data: { quizName: name, quizDuration: 'PT5M' },
    headers: { 'Content-Type': 'application/json' },
  })
  return (await res.json()).id as string
}

async function addQuestion(
  page: Page,
  quizId: string,
  type: 'SINGLE_CHOICE' | 'MULTI_CHOICE',
  text: string,
  options: { text: string; correct: boolean }[],
) {
  const res = await page.request.post('http://localhost:5173/api/question', {
    data: { quizId, text, type, options },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(res.ok(), `add question failed: ${res.status()} ${await res.text().catch(() => '')}`).toBeTruthy()
}

async function attemptOf(page: Page, quizId: string) {
  const res = await page.request.post('http://localhost:5173/api/attempt', {
    data: { quizId },
    headers: { 'Content-Type': 'application/json' },
  })
  return res.json()
}

async function commit(
  page: Page,
  attemptId: string,
  actions: { questionId: string; optionId: string; selected: boolean }[],
) {
  const res = await page.request.patch('http://localhost:5173/api/attempt/commit', {
    data: { attemptId, actions },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(res.ok(), `commit failed: ${res.status()}`).toBeTruthy()
}

async function finish(page: Page, attemptId: string) {
  const res = await page.request.patch('http://localhost:5173/api/attempt/finish', {
    data: { attemptId },
    headers: { 'Content-Type': 'application/json' },
  })
  return res.json()
}

test('single-choice: picking the right option scores 1/1', async ({ page }) => {
  await registerAndLogin(page)
  const quizId = await createQuiz(page, `SC right ${Date.now()}`)
  await addQuestion(page, quizId, 'SINGLE_CHOICE', 'Pick A', [
    { text: 'A', correct: true },
    { text: 'B', correct: false },
    { text: 'C', correct: false },
  ])
  const attempt = await attemptOf(page, quizId)
  const q = attempt.questions[0]
  const aId = q.options.find((o: { text: string }) => o.text === 'A').id
  await commit(page, attempt.id, [{ questionId: q.id, optionId: aId, selected: true }])
  const result = await finish(page, attempt.id)
  expect(result.score).toBe(1)
  expect(result.maximumScore).toBe(1)
})

test('single-choice: picking a wrong option scores 0/1', async ({ page }) => {
  await registerAndLogin(page)
  const quizId = await createQuiz(page, `SC wrong ${Date.now()}`)
  await addQuestion(page, quizId, 'SINGLE_CHOICE', 'Pick A', [
    { text: 'A', correct: true },
    { text: 'B', correct: false },
    { text: 'C', correct: false },
  ])
  const attempt = await attemptOf(page, quizId)
  const q = attempt.questions[0]
  const bId = q.options.find((o: { text: string }) => o.text === 'B').id
  await commit(page, attempt.id, [{ questionId: q.id, optionId: bId, selected: true }])
  const result = await finish(page, attempt.id)
  expect(result.score).toBe(0)
  expect(result.maximumScore).toBe(1)
})

test('single-choice: selecting a second option auto-clears the first', async ({ page }) => {
  await registerAndLogin(page)
  const quizId = await createQuiz(page, `SC auto-clear ${Date.now()}`)
  await addQuestion(page, quizId, 'SINGLE_CHOICE', 'Pick A', [
    { text: 'A', correct: true },
    { text: 'B', correct: false },
    { text: 'C', correct: false },
  ])
  const attempt = await attemptOf(page, quizId)
  const q = attempt.questions[0]
  const aId = q.options.find((o: { text: string }) => o.text === 'A').id
  const bId = q.options.find((o: { text: string }) => o.text === 'B').id
  // Pick A, then B. Backend should auto-clear A.
  await commit(page, attempt.id, [{ questionId: q.id, optionId: aId, selected: true }])
  await commit(page, attempt.id, [{ questionId: q.id, optionId: bId, selected: true }])

  const inProgress = await (await page.request.get(`http://localhost:5173/api/attempt/in-progress?page=0`)).json()
  const a = inProgress._embedded.attempts.find((x: { id: string }) => x.id === attempt.id)
  const refreshed = a.questions[0]
  const picked = refreshed.options.filter((o: { selected: boolean }) => o.selected)
  expect(picked).toHaveLength(1)
  expect(picked[0].text).toBe('B')
})

test('multi-choice: per-option scoring unchanged', async ({ page }) => {
  await registerAndLogin(page)
  const quizId = await createQuiz(page, `MC ${Date.now()}`)
  // 4 options, 1 correct. Pick the correct one only → 4 / 4 (the one correctly
  // picked + the three correctly skipped).
  await addQuestion(page, quizId, 'MULTI_CHOICE', 'Which is W?', [
    { text: 'W', correct: true },
    { text: 'X', correct: false },
    { text: 'Y', correct: false },
    { text: 'Z', correct: false },
  ])
  const attempt = await attemptOf(page, quizId)
  const q = attempt.questions[0]
  const wId = q.options.find((o: { text: string }) => o.text === 'W').id
  await commit(page, attempt.id, [{ questionId: q.id, optionId: wId, selected: true }])
  const result = await finish(page, attempt.id)
  expect(result.score).toBe(4)
  expect(result.maximumScore).toBe(4)
})

test('authoring rejects single-choice with two correct options', async ({ page }) => {
  await registerAndLogin(page)
  const quizId = await createQuiz(page, `SC bad ${Date.now()}`)
  const res = await page.request.post('http://localhost:5173/api/question', {
    data: {
      quizId,
      text: 'Bad shape',
      type: 'SINGLE_CHOICE',
      options: [
        { text: 'A', correct: true },
        { text: 'B', correct: true },
      ],
    },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(res.status()).toBe(400)
  const body = await res.json()
  expect(body.errors?.[0]?.code).toBe('INVALID_QUESTION_SHAPE')
})

test('authoring UI: switching to single-choice keeps only one correct option', async ({ page }) => {
  await registerAndLogin(page)
  const quizId = await createQuiz(page, `UI ${Date.now()}`)
  await page.goto(`/app/quiz/${quizId}`)
  await page.waitForLoadState('networkidle')

  // Question text + two options with both correct (multi-select default).
  await page.getByLabel('Question text').fill('UI test?')
  const opts = page.locator('input[placeholder="Option text"]')
  await opts.nth(0).fill('alpha')
  await opts.nth(1).fill('beta')
  await page.locator('input[type="checkbox"]').nth(0).check()
  await page.locator('input[type="checkbox"]').nth(1).check()

  // Now flip to single-choice. The watcher should leave at most one "correct".
  await page.getByRole('button', { name: /single choice/i }).click()
  await page.waitForTimeout(100)
  const radios = page.locator('input[type="radio"]')
  const checkedCount = await radios.evaluateAll((els) =>
    (els as HTMLInputElement[]).filter((e) => e.checked).length,
  )
  expect(checkedCount).toBe(1)
})
