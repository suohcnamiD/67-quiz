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

test('full happy path: create quiz → add question → start attempt → finish → see result', async ({ page }) => {
  page.on('dialog', async (d) => { await d.accept() })
  await registerAndLogin(page)
  const quizName = `E2E ${Date.now()}`

  // Create a quiz (UI)
  await page.getByRole('link', { name: /new quiz/i }).click()
  await page.waitForURL(/\/app\/quiz\/new/)
  await page.getByLabel('Quiz name').fill(quizName)
  await page.getByLabel(/duration/i).fill('5')
  await page.getByRole('button', { name: 'Create' }).click()
  await page.waitForURL(/\/app\/quiz\/[0-9a-f]{8}-/i, { timeout: 10_000 })
  const quizId = page.url().split('/').pop()!

  // Add a question (UI)
  await page.getByLabel('Question text').fill('What is 2+2?')
  const optInputs = page.locator('input[placeholder="Option text"]')
  await optInputs.nth(0).fill('3')
  await optInputs.nth(1).fill('4')
  await page.locator('input[type="checkbox"]').nth(1).check()
  await page.getByRole('button', { name: 'Add question' }).click()
  await expect(page.getByText('What is 2+2?')).toBeVisible({ timeout: 10_000 })

  // Start the attempt via the API — the global browse list is too crowded
  // from prior test runs to reliably locate a single new quiz's card.
  const startRes = await page.request.post('http://localhost:5173/api/attempt', {
    data: { quizId },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(startRes.ok()).toBeTruthy()
  const attempt = await startRes.json()
  await page.goto(`/app/attempt/${attempt.id}`)
  await page.waitForURL(/\/app\/attempt\/[^/]+$/)

  // Pick the right option (UI)
  await page.getByRole('button', { name: '4', exact: true }).click()
  await page.waitForTimeout(500)

  // Finish (UI)
  await page.getByRole('button', { name: /finish attempt/i }).click()
  await page.waitForURL(/\/app\/attempt\/[^/]+\/result$/, { timeout: 15_000 })

  // Dismiss the celebration popup so we can assert on the underlying page.
  const popup = page.getByRole('dialog')
  if (await popup.count()) await popup.locator('..').click({ force: true }).catch(() => {})

  // Should show score (not JSON)
  await expect(page.getByRole('heading', { name: /score/i }).or(page.getByText('Score').first())).toBeVisible()
  const body = (await page.locator('body').innerText()).trim()
  expect(body).not.toMatch(/^\{/)
})
