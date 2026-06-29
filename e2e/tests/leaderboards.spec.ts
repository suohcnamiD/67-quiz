import { test, expect, type Page, type APIRequestContext } from '@playwright/test'

const BASE = 'http://localhost:5173'
const PASSWORD = 'Passw0rd1'

async function registerUI(page: Page): Promise<string> {
  const username = `l${Date.now().toString(36)}${Math.floor(Math.random() * 1000)}`.slice(0, 16)
  await page.goto('/register')
  await page.locator('input[autocomplete="username"]').fill(username)
  await page.locator('input[autocomplete="new-password"]').fill(PASSWORD)
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/app/, { timeout: 15_000 })
  return username
}

async function makeAndFinishAttempt(req: APIRequestContext, quizId: string, pickText = 'true'): Promise<void> {
  const start = await req.post(`${BASE}/api/attempt`, {
    data: { quizId },
    headers: { 'Content-Type': 'application/json' },
  })
  const attempt = await start.json()
  // Pick the option whose text matches — order isn't stable across attempts.
  const question = attempt.questions?.[0]
  const option = (question?.options ?? []).find((o: { text?: string }) => o.text === pickText)
  if (option?.id && question?.id) {
    await req.patch(`${BASE}/api/attempt/commit`, {
      data: {
        attemptId: attempt.id,
        actions: [{ questionId: question.id, optionId: option.id, selected: true }],
      },
      headers: { 'Content-Type': 'application/json' },
    })
  }
  const fin = await req.patch(`${BASE}/api/attempt/finish`, {
    data: { attemptId: attempt.id },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(fin.ok()).toBeTruthy()
}

async function makeQuiz(req: APIRequestContext, name: string): Promise<string> {
  const res = await req.post(`${BASE}/api/quiz`, {
    data: { quizName: name, quizDuration: 'PT5M' },
    headers: { 'Content-Type': 'application/json' },
  })
  const quiz = await res.json()
  await req.post(`${BASE}/api/question`, {
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
  return quiz.id
}

test('leaderboards nav link is present and lands on the players tab', async ({ page }) => {
  await registerUI(page)
  await page.getByRole('link', { name: /leaderboards/i }).first().click()
  await page.waitForURL(/\/app\/leaderboards/)
  await expect(page.getByRole('heading', { name: 'Leaderboards' })).toBeVisible()
  await expect(page.getByRole('tab', { name: 'Top players' })).toHaveAttribute('aria-selected', 'true')
})

test('completing 3+ attempts surfaces the user on the players board with their rank', async ({ page }) => {
  page.on('dialog', async (d) => { await d.accept() })
  const username = await registerUI(page)
  // Create a quiz and run three finished attempts so the user qualifies (≥3).
  const quizId = await makeQuiz(page.request, `Lb players ${Date.now()}`)
  for (let i = 0; i < 3; i++) await makeAndFinishAttempt(page.request, quizId)
  await page.goto('/app/leaderboards')
  // "You are ranked X of N" pill should be visible.
  await expect(page.locator('.you')).toBeVisible({ timeout: 10_000 })
  await expect(page.locator('.you')).toContainText('You are ranked')
  await expect(page.locator('.you')).toContainText('100%')
  // The user's own row should be highlighted.
  await expect(page.locator('.row--you')).toBeVisible()
  await expect(page.locator('.row--you')).toContainText(username)
})

test('authors tab renders and shows the qualifier hint when the user has no ratings', async ({ page }) => {
  await registerUI(page)
  await page.goto('/app/leaderboards')
  await page.getByRole('tab', { name: 'Top authors' }).click()
  await expect(page.getByRole('tab', { name: 'Top authors' })).toHaveAttribute('aria-selected', 'true')
  await expect(page.getByText(/Qualifying: at least 5 ratings/)).toBeVisible()
})

test('a user with fewer than 3 attempts does not get a "you" rank on players', async ({ page }) => {
  page.on('dialog', async (d) => { await d.accept() })
  await registerUI(page)
  const quizId = await makeQuiz(page.request, `Lb sub ${Date.now()}`)
  // Only 2 finished attempts — below the 3-attempt threshold.
  await makeAndFinishAttempt(page.request, quizId)
  await makeAndFinishAttempt(page.request, quizId)
  await page.goto('/app/leaderboards')
  // Page renders, but no "you" pill — user is not in the qualifying set.
  await expect(page.getByRole('heading', { name: 'Leaderboards' })).toBeVisible()
  await expect(page.locator('.you')).toHaveCount(0)
})

test('players board orders entries by avg score descending', async ({ page, browser }) => {
  // Build three users with distinct averages: 100%, ~67%, 33%.
  // Each user takes 3 attempts of the same quiz, picking the right option
  // a different number of times.
  const owner = await registerUI(page)
  const quizId = await makeQuiz(page.request, `Order ${Date.now()}`)
  expect(owner).toBeTruthy()

  async function userWithAvg(picks: Array<'true' | 'false'>): Promise<string> {
    const ctx = await browser.newContext()
    const username = `o${Date.now().toString(36)}${Math.floor(Math.random() * 1000)}`.slice(0, 16)
    await ctx.request.post(`${BASE}/api/authentication/register`, {
      data: { username, password: PASSWORD },
      headers: { 'Content-Type': 'application/json' },
    })
    for (const pick of picks) {
      await makeAndFinishAttempt(ctx.request, quizId, pick)
    }
    await ctx.close()
    return username
  }

  // High: 3/3 correct → 100%
  const high = await userWithAvg(['true', 'true', 'true'])
  // Mid: 2/3 correct → 66.7%
  const mid = await userWithAvg(['true', 'true', 'false'])
  // Low: 1/3 correct → 33.3%
  const low = await userWithAvg(['true', 'false', 'false'])

  // Load the players board and capture rendered text in row order.
  await page.goto('/app/leaderboards')
  await expect(page.locator('.row').first()).toBeVisible({ timeout: 10_000 })

  // Find each of our three rows and read their .row__rank.
  async function rankOf(username: string): Promise<number> {
    const row = page.locator('.row', { hasText: username }).first()
    await expect(row).toBeVisible({ timeout: 10_000 })
    const rankText = await row.locator('.row__rank').innerText()
    return parseInt(rankText.replace('#', ''), 10)
  }

  const [hr, mr, lr] = await Promise.all([rankOf(high), rankOf(mid), rankOf(low)])
  // Strict ordering: high comes before mid which comes before low.
  expect(hr).toBeLessThan(mr)
  expect(mr).toBeLessThan(lr)
})
