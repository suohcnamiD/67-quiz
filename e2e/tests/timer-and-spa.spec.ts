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

test('attempt timer shows ~5 minutes immediately after start (not 00:00)', async ({ page }) => {
  page.on('dialog', async (d) => { await d.accept() })
  await registerAndLogin(page)
  const quizName = `TZ ${Date.now()}`

  await page.getByRole('link', { name: /new quiz/i }).click()
  await page.waitForURL(/\/app\/quiz\/new/)
  await page.getByLabel('Quiz name').fill(quizName)
  await page.getByLabel(/duration/i).fill('5')
  await page.getByRole('button', { name: 'Create' }).click()
  await page.waitForURL(/\/app\/quiz\/[^/]+$/, { timeout: 10_000 })

  await page.getByLabel('Question text').fill('q?')
  const opts = page.locator('input[placeholder="Option text"]')
  await opts.nth(0).fill('a')
  await opts.nth(1).fill('b')
  await page.locator('input[type="checkbox"]').nth(1).check()
  await page.getByRole('button', { name: 'Add question' }).click()
  await expect(page.getByText('q?')).toBeVisible({ timeout: 10_000 })

  await page.getByRole('link', { name: /browse/i }).first().click()
  await page.waitForURL(/\/app$/)

  const card = page.locator('article, .card, [class*=card]').filter({ has: page.getByRole('heading', { name: quizName }) }).first()
  await expect(card).toBeVisible()
  await card.getByRole('button', { name: /start attempt/i }).click()
  await page.waitForURL(/\/app\/attempt\/[^/]+$/, { timeout: 10_000 })

  // Within 1.5 seconds, the timer should read 04:5x (5-minute quiz).
  // Bug repro: shows 00:00 because the backend stamp is interpreted as UTC.
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
