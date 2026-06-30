import { test, expect, type Page, type APIRequestContext } from '@playwright/test'

const BASE = 'http://localhost:5173'
const PASSWORD = 'Passw0rd1'

async function registerUI(page: Page): Promise<string> {
  const username = `n${Date.now().toString(36)}${Math.floor(Math.random() * 10000)}`.slice(0, 16)
  await page.goto('/register')
  await page.locator('input[autocomplete="username"]').fill(username)
  await page.locator('input[autocomplete="new-password"]').fill(PASSWORD)
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/app/, { timeout: 15_000 })
  return username
}

async function registerApi(req: APIRequestContext): Promise<string> {
  const username = `n${Date.now().toString(36)}${Math.floor(Math.random() * 10000)}`.slice(0, 16)
  await req.post(`${BASE}/api/authentication/register`, {
    data: { username, password: PASSWORD },
    headers: { 'Content-Type': 'application/json' },
  })
  return username
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

async function makeAndFinishAttempt(req: APIRequestContext, quizId: string, pickText = 'true'): Promise<void> {
  const startRes = await req.post(`${BASE}/api/attempt`, {
    data: { quizId }, headers: { 'Content-Type': 'application/json' },
  })
  const attempt = await startRes.json()
  const question = attempt.questions?.[0]
  const option = (question?.options ?? []).find((o: { text?: string }) => o.text === pickText)
  if (option?.id && question?.id) {
    await req.patch(`${BASE}/api/attempt/commit`, {
      data: { attemptId: attempt.id, actions: [{ questionId: question.id, optionId: option.id, selected: true }] },
      headers: { 'Content-Type': 'application/json' },
    })
  }
  await req.patch(`${BASE}/api/attempt/finish`, {
    data: { attemptId: attempt.id }, headers: { 'Content-Type': 'application/json' },
  })
}

test('comment on profile generates a COMMENT_RECEIVED notification with bell badge', async ({ page, browser }) => {
  const owner = await registerUI(page)

  // A separate context posts a comment on owner's profile via the API.
  const ctx = await browser.newContext()
  await registerApi(ctx.request)
  await ctx.request.post(`${BASE}/api/users/${owner}/comments`, {
    data: { body: `Hi from a stranger ${Date.now()}` },
    headers: { 'Content-Type': 'application/json' },
  })
  await ctx.close()

  // Reload to pick up the unread count.
  await page.reload()
  // Bell badge should show 1.
  const badge = page.locator('.bell__badge')
  await expect(badge).toBeVisible({ timeout: 5_000 })
  await expect(badge).toContainText('1')
})

test('rating my quiz generates a QUIZ_RATED notification visible in the dropdown', async ({ page, browser }) => {
  const owner = await registerUI(page)
  const quizId = await makeQuiz(page.request, `Notif rating ${Date.now()}`)

  // A separate user attempts (so they can rate), finishes, then rates.
  const ctx = await browser.newContext()
  await registerApi(ctx.request)
  await makeAndFinishAttempt(ctx.request, quizId)
  await ctx.request.put(`${BASE}/api/quiz/${quizId}/ratings/me`, {
    data: { score: 8, comment: '' },
    headers: { 'Content-Type': 'application/json' },
  })
  await ctx.close()

  await page.reload()
  // We should have at least one notification (QUIZ_RATED + QUIZ_ATTEMPTED actually).
  await expect(page.locator('.bell__badge')).toBeVisible({ timeout: 5_000 })

  // Open the dropdown and assert the rating notification is in the list.
  await page.locator('.bell__button').click()
  await expect(page.getByRole('menu', { name: /recent notifications/i })).toBeVisible()
  await expect(page.locator('.bell__item').filter({ hasText: /rated.*8\/10/i }).first()).toBeVisible({ timeout: 5_000 })
  expect(owner).toBeTruthy()
})

test('finishing my quiz generates a QUIZ_ATTEMPTED notification', async ({ page, browser }) => {
  const owner = await registerUI(page)
  const quizId = await makeQuiz(page.request, `Notif attempt ${Date.now()}`)

  const ctx = await browser.newContext()
  await registerApi(ctx.request)
  await makeAndFinishAttempt(ctx.request, quizId)
  await ctx.close()

  await page.reload()
  await page.locator('.bell__button').click()
  await expect(page.locator('.bell__item').filter({ hasText: /finished/i }).first()).toBeVisible({ timeout: 5_000 })
  expect(owner).toBeTruthy()
})

test('clicking a notification marks it read and clears the badge if it was the only unread', async ({ page, browser }) => {
  const owner = await registerUI(page)
  const ctx = await browser.newContext()
  await registerApi(ctx.request)
  await ctx.request.post(`${BASE}/api/users/${owner}/comments`, {
    data: { body: 'click me' },
    headers: { 'Content-Type': 'application/json' },
  })
  await ctx.close()

  await page.reload()
  await expect(page.locator('.bell__badge')).toBeVisible({ timeout: 5_000 })

  await page.locator('.bell__button').click()
  const item = page.locator('.bell__item').first()
  await expect(item).toBeVisible()
  await item.click()
  // After click we should be on the profile page and the badge should be gone.
  await page.waitForURL(/\/app\/profile/)
  await expect(page.locator('.bell__badge')).toHaveCount(0, { timeout: 5_000 })
})

test('mark-all-read clears every unread notification', async ({ page, browser }) => {
  const owner = await registerUI(page)
  const quizId = await makeQuiz(page.request, `Mark-all ${Date.now()}`)
  const ctx = await browser.newContext()
  await registerApi(ctx.request)
  // Generate two notifications: an attempt + a rating.
  await makeAndFinishAttempt(ctx.request, quizId)
  await ctx.request.put(`${BASE}/api/quiz/${quizId}/ratings/me`, {
    data: { score: 7, comment: '' }, headers: { 'Content-Type': 'application/json' },
  })
  await ctx.close()

  await page.reload()
  await expect(page.locator('.bell__badge')).toBeVisible({ timeout: 5_000 })

  // Open dropdown, hit "Mark all read".
  await page.locator('.bell__button').click()
  await page.getByRole('button', { name: /mark all read/i }).first().click()
  await expect(page.locator('.bell__badge')).toHaveCount(0, { timeout: 5_000 })
  expect(owner).toBeTruthy()
})

test('notifications history page lists every notification', async ({ page, browser }) => {
  const owner = await registerUI(page)
  const quizId = await makeQuiz(page.request, `History ${Date.now()}`)
  const ctx = await browser.newContext()
  await registerApi(ctx.request)
  await makeAndFinishAttempt(ctx.request, quizId)
  await ctx.request.put(`${BASE}/api/quiz/${quizId}/ratings/me`, {
    data: { score: 9, comment: '' }, headers: { 'Content-Type': 'application/json' },
  })
  await ctx.request.post(`${BASE}/api/users/${owner}/comments`, {
    data: { body: 'history comment' }, headers: { 'Content-Type': 'application/json' },
  })
  await ctx.close()

  await page.goto('/app/notifications')
  await expect(page.getByRole('heading', { name: 'Notifications' })).toBeVisible()
  // We should see all three.
  await expect(page.locator('.item').filter({ hasText: /finished/i }).first()).toBeVisible()
  await expect(page.locator('.item').filter({ hasText: /rated/i }).first()).toBeVisible()
  await expect(page.locator('.item').filter({ hasText: /commented/i }).first()).toBeVisible()
})

test('leaderboard rank drop fires a RANK_DROPPED notification', async ({ page, browser }) => {
  test.setTimeout(90_000)
  // The snapshot job runs every 2s in this profile (notifications.snapshot.
  // interval-ms=2000). To see a drop we need: (1) victim snapshot exists, (2)
  // a competitor outranks them, (3) the next snapshot runs and compares.

  // Victim — qualifies with 3 perfect attempts.
  const victim = await registerUI(page)
  const quizA = await makeQuiz(page.request, `Drop A ${Date.now()}`)
  for (let i = 0; i < 3; i++) await makeAndFinishAttempt(page.request, quizA)

  // Wait for the victim to be snapshotted at some rank.
  await page.waitForTimeout(4_000)

  // Stronger competitor — 10 perfect attempts shrink less under Bayes.
  const ctx = await browser.newContext()
  await registerApi(ctx.request)
  const quizB = await makeQuiz(ctx.request, `Drop B ${Date.now()}`)
  for (let i = 0; i < 10; i++) await makeAndFinishAttempt(ctx.request, quizB)
  await ctx.close()

  // Wait for the snapshot job to record the competitor and then run another
  // pass that detects the victim's drop. Up to 40s of polling.
  await page.goto('/app/notifications')
  await expect
    .poll(async () => {
      await page.reload()
      // Wait for the page to settle: either the empty state or the list.
      await page.locator('.empty, .list').first().waitFor({ state: 'visible', timeout: 5_000 })
      return await page.locator('.item').filter({ hasText: /dropped from/i }).count()
    }, { timeout: 40_000, intervals: [2_500, 2_500, 2_500] })
    .toBeGreaterThan(0)
  expect(victim).toBeTruthy()
})
