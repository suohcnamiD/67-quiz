import { test, expect, type Page } from '@playwright/test'

async function registerAndLogin(page: Page): Promise<string> {
  const username = `n${Date.now().toString(36)}${Math.floor(Math.random() * 100)}`.slice(0, 16)
  const password = 'Passw0rd1'
  await page.goto('/register')
  await page.locator('input[autocomplete="username"]').fill(username)
  await page.locator('input[autocomplete="new-password"]').fill(password)
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/app/, { timeout: 15_000 })
  return username
}

test('unknown /app route shows the 404 page with a back link', async ({ page }) => {
  await registerAndLogin(page)
  await page.goto('/app/totally-not-a-real-page')
  await page.waitForLoadState('networkidle')

  await expect(page.getByRole('heading', { name: 'Page not found' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Back to browse' })).toBeVisible()
})

test('top-level garbage URL also lands on the 404 page (after auth)', async ({ page }) => {
  await registerAndLogin(page)
  await page.goto('/something-completely-random')
  await page.waitForLoadState('networkidle')

  await expect(page.getByRole('heading', { name: 'Page not found' })).toBeVisible()
})
