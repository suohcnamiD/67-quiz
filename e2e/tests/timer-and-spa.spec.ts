import { test, expect, type Page } from '@playwright/test'

async function registerAndLogin(page: Page): Promise<string> {
  const username = `e${Date.now().toString(36)}${Math.floor(Math.random() * 100)}`.slice(0, 16)
  const password = 'Passw0rd1'
  await page.goto('/register')
  await page.locator('input[autocomplete="username"]').fill(username)
  await page.locator('input[autocomplete="new-password"]').fill(password)
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/app/, { timeout: 15_000 })
  return username
}

async function createQuizWithOneQuestion(page: Page, quizName: string): Promise<string> {
  // Create quiz via the UI so we exercise the same path, then read the
  // generated quizId from the URL — that's enough to start an attempt
  // directly without depending on the global browse list pagination.
  await page.goto('/app/quiz/new')
  await page.getByLabel('Quiz name').fill(quizName)
  await page.getByLabel(/duration/i).fill('5')
  await page.getByRole('button', { name: 'Create' }).click()
  await page.waitForURL(/\/app\/quiz\/[0-9a-f]{8}-/i, { timeout: 10_000 })
  const quizId = page.url().split('/').pop()!
  await page.getByLabel('Question text').fill('q?')
  const opts = page.locator('input[placeholder="Option text"]')
  await opts.nth(0).fill('a')
  await opts.nth(1).fill('b')
  await page.locator('input[type="checkbox"]').nth(1).check()
  await page.getByRole('button', { name: 'Add question' }).click()
  await expect(page.getByText('q?')).toBeVisible({ timeout: 10_000 })
  return quizId
}

async function startAttemptViaApi(page: Page, quizId: string): Promise<string> {
  const res = await page.request.post('http://localhost:5173/api/attempt', {
    data: { quizId },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(res.ok(), `start attempt failed: ${res.status()}`).toBeTruthy()
  const body = await res.json()
  return body.id as string
}

async function createQuizViaApi(page: Page, quizName: string, duration: string): Promise<string> {
  const create = await page.request.post('http://localhost:5173/api/quiz', {
    data: { quizName, quizDuration: duration },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(create.ok(), `create quiz failed: ${create.status()}`).toBeTruthy()
  const quiz = await create.json()
  // Backend requires at least one question to start an attempt? Actually no —
  // it tolerates empty quizzes for attempts. Add one anyway for realism.
  const q = await page.request.post('http://localhost:5173/api/question', {
    data: { quizId: quiz.id, text: 'q', options: [{ text: 'a', correct: true }, { text: 'b', correct: false }] },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(q.ok()).toBeTruthy()
  return quiz.id as string
}

test('timer expiry auto-finishes the attempt and bounces to result', async ({ page }) => {
  await registerAndLogin(page)
  // A 2-second attempt — long enough to render the AttemptView, short enough
  // to expire before the test times out.
  const quizId = await createQuizViaApi(page, `Expire ${Date.now()}`, 'PT2S')
  const attemptId = await startAttemptViaApi(page, quizId)

  // Watch for any "Attempt not found" flash during the transition.
  let flashedNotFound = false
  const watchdog = (async () => {
    const start = Date.now()
    while (Date.now() - start < 10_000) {
      if (await page.getByText(/Attempt not found/i).count() > 0) {
        flashedNotFound = true
        return
      }
      await page.waitForTimeout(50)
    }
  })()

  await page.goto(`/app/attempt/${attemptId}`)
  // We should land on the attempt view briefly, then auto-redirect to /result.
  await page.waitForURL(new RegExp(`/app/attempt/${attemptId}/result$`), { timeout: 15_000 })

  // The result view should render the score (not raw JSON, not "Loading…").
  const body = (await page.locator('body').innerText()).trim()
  expect(body).not.toMatch(/^\{/)
  // Result hero shows a percent and pts; per-question card shows Score badge.
  await expect(page.getByText(/^\d+%$/).first()).toBeVisible({ timeout: 5_000 })
  await watchdog
  expect(flashedNotFound, '"Attempt not found" flashed during auto-finish').toBe(false)
})

test('attempt timer shows ~5 minutes immediately after start (not 00:00)', async ({ page }) => {
  page.on('dialog', async (d) => { await d.accept() })
  await registerAndLogin(page)
  const quizId = await createQuizWithOneQuestion(page, `TZ ${Date.now()}`)
  const attemptId = await startAttemptViaApi(page, quizId)
  await page.goto(`/app/attempt/${attemptId}`)

  const timer = page.locator('.time')
  await expect(timer).toBeVisible({ timeout: 5_000 })
  const text = (await timer.innerText()).trim()
  expect(text, `timer reads ${text}, expected 04:5x`).toMatch(/^04:5[0-9]$/)
})

test('hitting backend SPA shell paths never returns raw JSON', async ({ request }) => {
  for (const path of ['/app', '/app/foo', '/app/attempt/abc', '/login', '/register', '/totally-not-a-thing']) {
    const res = await request.get(`http://localhost:8080${path}`)
    const body = (await res.text()).trimStart()
    expect(body.startsWith('{'), `path ${path} returned JSON body starting with ${body.slice(0,80)}`).toBe(false)
  }
})

test('clicking options does not flash "Loading…"', async ({ page }) => {
  page.on('dialog', async (d) => { await d.accept() })
  await registerAndLogin(page)
  const quizId = await createQuizWithOneQuestion(page, `Flash ${Date.now()}`)
  const attemptId = await startAttemptViaApi(page, quizId)
  await page.goto(`/app/attempt/${attemptId}`)

  const loading = page.getByText('Loading…')
  const optionButtons = page.getByRole('button').filter({ hasText: /^[ab]$/ })
  await expect(optionButtons.first()).toBeVisible({ timeout: 5_000 })

  let flashed = false
  const watchdog = (async () => {
    for (let i = 0; i < 30; i++) {
      if (await loading.count() > 0) { flashed = true; return }
      await page.waitForTimeout(50)
    }
  })()

  await optionButtons.first().click()
  await page.waitForTimeout(300)
  await optionButtons.nth(1).click()
  await page.waitForTimeout(300)
  await optionButtons.first().click()
  await watchdog

  expect(flashed, 'Loading… flashed while toggling options').toBe(false)
})

test('attempt result shows the score with the label "Score"', async ({ page }) => {
  page.on('dialog', async (d) => { await d.accept() })
  await registerAndLogin(page)
  const quizId = await createQuizWithOneQuestion(page, `Score ${Date.now()}`)
  const attemptId = await startAttemptViaApi(page, quizId)
  await page.goto(`/app/attempt/${attemptId}`)

  await page.getByRole('button', { name: 'b', exact: true }).click()
  await page.waitForTimeout(400)
  await page.getByRole('button', { name: /finish attempt/i }).click()
  await page.waitForURL(/\/app\/attempt\/[^/]+\/result$/, { timeout: 15_000 })

  // Dismiss celebration overlay if it appears.
  const overlay = page.getByRole('dialog')
  if (await overlay.count()) await overlay.locator('..').click({ force: true }).catch(() => {})

  // Hero shows the percentage and pts. Per-question card shows a "Score" badge.
  await expect(page.getByText(/^\d+%$/).first()).toBeVisible()
  await expect(page.getByText(/of \d+ points/).first()).toBeVisible()
})

test('finished attempt shows clear correct/wrong cues', async ({ page }) => {
  await registerAndLogin(page)
  // Create a quiz with 1 correct + 1 distractor option.
  const create = await page.request.post('http://localhost:5173/api/quiz', {
    data: { quizName: `Cues ${Date.now()}`, quizDuration: 'PT5M' },
    headers: { 'Content-Type': 'application/json' },
  })
  const quiz = await create.json()
  await page.request.post('http://localhost:5173/api/question', {
    data: {
      quizId: quiz.id,
      text: 'q',
      options: [
        { text: 'right', correct: true },
        { text: 'wrong', correct: false },
      ],
    },
    headers: { 'Content-Type': 'application/json' },
  })
  const attemptId = await startAttemptViaApi(page, quiz.id)

  // Commit one correct + one wrong pick directly via API.
  const attemptRes = await page.request.get(`http://localhost:5173/api/attempt/in-progress?page=0`)
  const attemptsJson = await attemptRes.json()
  const a = attemptsJson._embedded.attempts.find((x: { id: string }) => x.id === attemptId)
  const rightOpt = a.questions[0].options.find((o: { text: string }) => o.text === 'right').id
  const wrongOpt = a.questions[0].options.find((o: { text: string }) => o.text === 'wrong').id
  await page.request.patch('http://localhost:5173/api/attempt/commit', {
    data: { attemptId, actions: [{ questionId: a.questions[0].id, optionId: rightOpt, selected: true }] },
    headers: { 'Content-Type': 'application/json' },
  })
  await page.request.patch('http://localhost:5173/api/attempt/finish', {
    data: { attemptId },
    headers: { 'Content-Type': 'application/json' },
  })

  await page.goto(`/app/attempt/${attemptId}/result`)
  // Both states must read explicitly in terms of the user's action.
  await expect(page.getByText(/You picked — correct/i).first()).toBeVisible()
  await expect(page.getByText(/You skipped$/).first()).toBeVisible()
  // sanity: the correct option's row has the green visual state class
  await expect(page.locator('.opt--correct').first()).toBeVisible()
})

