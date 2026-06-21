import { test, expect, type Page } from '@playwright/test'

async function registerAndLogin(page: Page): Promise<string> {
  const username = `s${Date.now().toString(36)}${Math.floor(Math.random() * 100)}`.slice(0, 16)
  const password = 'Passw0rd1'
  await page.goto('/register')
  await page.locator('input[autocomplete="username"]').fill(username)
  await page.locator('input[autocomplete="new-password"]').fill(password)
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/app/, { timeout: 15_000 })
  return username
}

test('search finds matching quizzes and people', async ({ page }) => {
  await registerAndLogin(page)
  await page.goto('/app')
  await page.waitForLoadState('networkidle')

  await page.locator('input[type="search"]').fill('sam')
  // Debounce is 250ms; give the search round-trip room.
  await page.waitForTimeout(500)

  // Quizzes group shows Sampler 10 (seeded).
  await expect(page.getByRole('heading', { name: /^Quizzes/ })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Sampler 10' })).toBeVisible()
  // People group shows sampler.
  await expect(page.getByRole('heading', { name: /^People/ })).toBeVisible()
  await expect(page.getByText('@sampler')).toBeVisible()
})

test('search below 2 chars returns nothing visible', async ({ page }) => {
  await registerAndLogin(page)
  await page.goto('/app')
  await page.waitForLoadState('networkidle')

  await page.locator('input[type="search"]').fill('a')
  await page.waitForTimeout(500)
  // The Browse/Continue sections are still visible; the search groups never render.
  await expect(page.getByRole('heading', { name: 'Browse quizzes' })).toBeVisible()
  expect(await page.getByRole('heading', { name: /^Quizzes\b/ }).count()).toBe(0)
})

test('clicking a user search result navigates to their profile', async ({ page }) => {
  await registerAndLogin(page)
  await page.goto('/app')
  await page.waitForLoadState('networkidle')

  await page.locator('input[type="search"]').fill('sampler')
  await page.waitForTimeout(500)

  await page.getByText('@sampler').click()
  await page.waitForURL(/\/app\/users\/sampler$/, { timeout: 10_000 })
  await expect(page.getByRole('heading', { name: 'sampler', exact: true })).toBeVisible()
})

test('clear button empties the search', async ({ page }) => {
  await registerAndLogin(page)
  await page.goto('/app')
  await page.waitForLoadState('networkidle')

  const input = page.locator('input[type="search"]')
  await input.fill('sampler')
  await page.waitForTimeout(500)
  await page.getByRole('button', { name: /clear search/i }).click()
  await expect(input).toHaveValue('')
  // Browse sections return.
  await expect(page.getByRole('heading', { name: 'Browse quizzes' })).toBeVisible()
})
