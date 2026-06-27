import { test, expect } from '@playwright/test'

test('anonymous visit to / renders the landing page', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '67quiz' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Get started' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Sign in' })).toBeVisible()
})

test('Get started routes to /register', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('link', { name: 'Get started' }).click()
  await page.waitForURL(/\/register$/, { timeout: 5_000 })
})
