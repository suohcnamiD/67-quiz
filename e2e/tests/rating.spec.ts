import { test, expect, type Page } from '@playwright/test'

const BASE = 'http://localhost:5173'
const PASSWORD = 'Passw0rd1'

async function register(page: Page): Promise<string> {
  const username = `r${Date.now().toString(36)}${Math.floor(Math.random() * 100)}`.slice(0, 16)
  await page.goto('/register')
  await page.locator('input[autocomplete="username"]').fill(username)
  await page.locator('input[autocomplete="new-password"]').fill(PASSWORD)
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/app/, { timeout: 15_000 })
  return username
}

// Build a one-question quiz via the API (the authoring flow is exercised in
// happy-path.spec.ts already — here we just need a quiz to attempt + rate).
async function makeQuiz(page: Page, name: string): Promise<string> {
  const quizRes = await page.request.post(`${BASE}/api/quiz`, {
    data: { quizName: name, quizDuration: 'PT5M' },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(quizRes.ok()).toBeTruthy()
  const quiz = await quizRes.json()
  const addRes = await page.request.post(`${BASE}/api/question`, {
    data: {
      quizId: quiz.id,
      text: 'Pick the truthy one',
      type: 'SINGLE_CHOICE',
      options: [
        { text: 'false', correct: false },
        { text: 'true', correct: true },
      ],
    },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(addRes.ok()).toBeTruthy()
  return quiz.id
}

async function attemptAndFinish(page: Page, quizId: string): Promise<string> {
  const startRes = await page.request.post(`${BASE}/api/attempt`, {
    data: { quizId },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(startRes.ok()).toBeTruthy()
  const attempt = await startRes.json()
  await page.goto(`/app/attempt/${attempt.id}`)
  await page.waitForURL(/\/app\/attempt\/[^/]+$/)
  await page.getByRole('button', { name: 'true', exact: true }).click()
  await page.waitForTimeout(300)
  await page.getByRole('button', { name: /finish attempt/i }).first().click()
  await page.getByRole('dialog').getByRole('button', { name: 'Finish' }).click()
  await page.waitForURL(/\/app\/attempt\/[^/]+\/result/, { timeout: 15_000 })
  // Dismiss the celebration if it's open so it doesn't intercept clicks.
  const popup = page.getByRole('dialog')
  if (await popup.count()) await popup.locator('..').click({ force: true }).catch(() => {})
  return attempt.id
}

test('result page shows the rating widget and persists a submitted rating', async ({ page }) => {
  page.on('dialog', async (d) => { await d.accept() })
  await register(page)
  const quizId = await makeQuiz(page, `Rating ${Date.now()}`)
  await attemptAndFinish(page, quizId)

  // Widget is visible with the empty-state title.
  const rateCard = page.locator('section.rate, .rate').first()
  await expect(page.getByRole('heading', { name: 'Rate this quiz' })).toBeVisible()

  // Submit a 7 with a short comment.
  const stars = page.locator('.rate__star')
  await expect(stars).toHaveCount(10)
  await stars.nth(6).click()
  await expect(page.getByText('You picked 7 / 10.')).toBeVisible()
  await page.locator('textarea.rate__comment').fill('Solid little quiz.')
  await page.getByRole('button', { name: /submit rating/i }).click()
  await expect(page.getByText('Saved.')).toBeVisible({ timeout: 5_000 })

  // Reload the result page — widget should now title "Your rating" and the
  // value should be pre-filled (the dismiss flag means the prompt would be
  // hidden anyway, but the saved rating overrides that).
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Your rating' })).toBeVisible({ timeout: 10_000 })
  await expect(page.getByText('You picked 7 / 10.')).toBeVisible()
  await expect(page.locator('textarea.rate__comment')).toHaveValue('Solid little quiz.')

  // Update to 9 and confirm the button copy switches to "Update rating".
  await stars.nth(8).click()
  await page.getByRole('button', { name: /update rating/i }).click()
  await expect(page.getByText('Saved.')).toBeVisible({ timeout: 5_000 })
})

test('quiz card shows the rating average and count after submission', async ({ page }) => {
  page.on('dialog', async (d) => { await d.accept() })
  await register(page)
  const quizName = `Rated ${Date.now()}`
  const quizId = await makeQuiz(page, quizName)
  await attemptAndFinish(page, quizId)

  // Submit a rating of 8.
  await page.locator('.rate__star').nth(7).click()
  await page.getByRole('button', { name: /submit rating/i }).click()
  await expect(page.getByText('Saved.')).toBeVisible({ timeout: 5_000 })

  // Browse → find this quiz's card by its name — it should show ★ 8 (1).
  await page.goto('/app')
  await page.getByPlaceholder(/search quizzes/i).fill(quizName)
  // Wait for debounced search results to render.
  const card = page.locator('article, [class*="card"]').filter({ hasText: quizName }).first()
  await expect(card).toBeVisible({ timeout: 10_000 })
  await expect(card).toContainText('★')
  await expect(card).toContainText('8')
  await expect(card).toContainText('(1)')
})

test('non-eligible user cannot submit a rating without finishing', async ({ page }) => {
  // User A creates the quiz; user B (no attempt) tries to rate via the API.
  await register(page)
  const quizId = await makeQuiz(page, `Eligibility ${Date.now()}`)
  await page.request.post(`${BASE}/api/authentication/logout`).catch(() => {})
  // Register a fresh user (no finished attempt of the quiz).
  await register(page)
  const res = await page.request.put(`${BASE}/api/quiz/${quizId}/ratings/me`, {
    data: { score: 5, comment: 'too soon' },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(res.status()).toBe(403)
  const body = await res.json()
  expect(body.errors?.[0]?.code).toBe('RATING_NOT_ELIGIBLE')
})

test('rating with an out-of-range score is rejected', async ({ page }) => {
  page.on('dialog', async (d) => { await d.accept() })
  await register(page)
  const quizId = await makeQuiz(page, `Range ${Date.now()}`)
  await attemptAndFinish(page, quizId)

  // Use the API to bypass the slider clamp — the backend's @Min/@Max
  // bean-validation kicks in before the service's range check, returning 400.
  const res = await page.request.put(`${BASE}/api/quiz/${quizId}/ratings/me`, {
    data: { score: 99, comment: '' },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(res.status()).toBe(400)
})
