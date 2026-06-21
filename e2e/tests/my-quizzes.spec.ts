import { test, expect, type Page } from '@playwright/test'

async function registerAndLogin(page: Page): Promise<string> {
  const username = `m${Date.now().toString(36)}${Math.floor(Math.random() * 100)}`.slice(0, 16)
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

test('own profile lists authored quizzes', async ({ page }) => {
  await registerAndLogin(page)
  const unique = Date.now()
  await createQuiz(page, `Mine A ${unique}`)
  await createQuiz(page, `Mine B ${unique}`)
  await page.goto('/app/profile')
  await page.waitForLoadState('networkidle')

  await expect(page.getByRole('heading', { name: 'Your quizzes' })).toBeVisible()
  await expect(page.getByRole('heading', { name: `Mine A ${unique}` })).toBeVisible()
  await expect(page.getByRole('heading', { name: `Mine B ${unique}` })).toBeVisible()
})

test('public profile lists authored quizzes (read-only — no Delete)', async ({ page }) => {
  // Sampler has seeded quizzes. Register a fresh user and look at sampler's profile.
  await registerAndLogin(page)
  await page.goto('/app/users/sampler')
  await page.waitForLoadState('networkidle')

  await expect(page.getByRole('heading', { name: /Quizzes by/i })).toBeVisible()
  // At least one of the seeded quizzes is visible.
  await expect(page.getByRole('heading', { name: 'Sampler 10' })).toBeVisible({ timeout: 10_000 })
  // Crucially, no Delete button on someone else's quizzes.
  expect(await page.getByRole('button', { name: 'Delete' }).count()).toBe(0)
})

test('own-profile shows Delete on each authored card', async ({ page }) => {
  await registerAndLogin(page)
  const unique = Date.now()
  await createQuiz(page, `Owned ${unique}`)
  await page.goto('/app/profile')
  await page.waitForLoadState('networkidle')

  // The card for the new quiz includes a Delete button.
  await expect(page.getByRole('heading', { name: `Owned ${unique}` })).toBeVisible()
  expect(await page.getByRole('button', { name: 'Delete' }).count()).toBeGreaterThan(0)
})
