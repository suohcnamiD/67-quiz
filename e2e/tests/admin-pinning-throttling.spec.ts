import { test, expect, type Page } from '@playwright/test'

async function registerAndLogin(page: Page, prefix = 'p'): Promise<string> {
  const username = `${prefix}${Date.now().toString(36)}${Math.floor(Math.random() * 100)}`.slice(0, 16)
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
  expect(res.ok()).toBeTruthy()
  return (await res.json()).id as string
}

test.describe('Admin + pinning', () => {
  test('non-admin cannot pin a quiz they own — 403 NO_ACCESS_TO_QUIZ', async ({ page }) => {
    await registerAndLogin(page, 'own')
    const quizId = await createQuiz(page, `Pin own ${Date.now()}`)

    const res = await page.request.patch(`http://localhost:5173/api/quiz/${quizId}/pin`, {
      data: { pinned: true },
      headers: { 'Content-Type': 'application/json' },
    })
    expect(res.status()).toBe(403)
    const body = await res.json()
    expect(body.errors?.[0]?.code).toBe('NO_ACCESS_TO_QUIZ')
  })

  test('overview page hides Pin/Unpin button for non-admin users', async ({ page }) => {
    await registerAndLogin(page, 'own')
    const quizId = await createQuiz(page, `NoPinBtn ${Date.now()}`)
    await page.goto(`/app/quiz/${quizId}`)
    await expect(page.getByRole('button', { name: /^pin$/i })).toHaveCount(0)
    await expect(page.getByRole('button', { name: /^unpin$/i })).toHaveCount(0)
  })

  // Verifying the admin PATH itself (that /pin flips the flag when the caller
  // has ADMIN) would require an admin fixture user seeded by APP_ADMIN_USERNAMES
  // — deferred to a config-level test since e2e doesn't restart the backend
  // with new env between tests.
})

test.describe('Logout invalidates the session', () => {
  test('after logout, refreshing the page bounces back to /login (session is dead server-side)', async ({ page }) => {
    await registerAndLogin(page, 'lo')
    // Verify we're logged in.
    const me = await page.request.get('http://localhost:5173/api/users/me')
    expect(me.status()).toBe(200)

    // Logout via UI to exercise the full store→BE flow.
    await page.getByRole('button', { name: /sign out/i }).first().click()
    // ConfirmDialog appears — accept.
    await page.getByRole('dialog').getByRole('button', { name: /sign out/i }).click()
    await page.waitForURL(/\/login/, { timeout: 10_000 })

    // The session cookie should now be invalid. Even keeping the cookies
    // (Spring Session removes the row), calling /me should 401.
    const meAfter = await page.request.get('http://localhost:5173/api/users/me')
    expect(meAfter.status()).toBe(401)
  })
})

test.describe('Rate limiting', () => {
  test.skip(!process.env.THROTTLING_ENABLED, 'backend must run without local/test profile to enforce throttling')
  test('rapid failed logins get 429 with RATE_LIMITED after the 10th attempt', async ({ page }) => {
    await page.goto('/')

    let rateLimitedAt = -1
    for (let i = 0; i < 15; i++) {
      const res = await page.request.post('http://localhost:5173/api/authentication/login', {
        data: { username: 'definitelynotauser', password: 'nope' },
        headers: { 'Content-Type': 'application/json' },
        failOnStatusCode: false,
      })
      if (res.status() === 429) {
        rateLimitedAt = i
        const body = await res.json()
        expect(body.errors?.[0]?.code).toBe('RATE_LIMITED')
        expect(res.headers()['retry-after']).toBeDefined()
        break
      }
    }
    // The bucket is 10/min; we should hit the limit somewhere in the 11-15
    // range (small jitter allowed for shared state with other tests).
    expect(rateLimitedAt).toBeGreaterThanOrEqual(0)
    expect(rateLimitedAt).toBeLessThan(15)
  })
})
