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

  // Create a quiz
  await page.getByRole('link', { name: /new quiz/i }).click()
  await page.waitForURL(/\/app\/quiz\/new/)
  await page.getByLabel('Quiz name').fill(quizName)
  await page.getByLabel(/duration/i).fill('5')
  await page.getByRole('button', { name: 'Create' }).click()
  await page.waitForURL(/\/app\/quiz\/[^/]+$/, { timeout: 10_000 })

  // Add a question
  await page.getByLabel('Question text').fill('What is 2+2?')
  const optInputs = page.locator('input[placeholder="Option text"]')
  await optInputs.nth(0).fill('3')
  await optInputs.nth(1).fill('4')
  await page.locator('input[type="checkbox"]').nth(1).check()
  await page.getByRole('button', { name: 'Add question' }).click()
  await expect(page.getByText('What is 2+2?')).toBeVisible({ timeout: 10_000 })

  // Back to browse
  await page.getByRole('link', { name: /browse/i }).first().click()
  await page.waitForURL(/\/app$/)

  // Start the attempt on the quiz this user just created.
  const card = page.locator('article, .card, [class*=card]').filter({ has: page.getByRole('heading', { name: quizName }) }).first()
  await expect(card).toBeVisible()
  await card.getByRole('button', { name: /start attempt/i }).click()
  await page.waitForURL(/\/app\/attempt\/[^/]+$/, { timeout: 10_000 })

  // Pick the right option
  await page.getByRole('button', { name: '4', exact: true }).click()
  await page.waitForTimeout(800)

  // Finish
  await page.getByRole('button', { name: /finish attempt/i }).click()
  await page.waitForURL(/\/app\/attempt\/[^/]+\/result$/, { timeout: 15_000 })

  // Should show score and quiz name (not JSON)
  await expect(page.getByText(new RegExp(quizName))).toBeVisible()
  const body = (await page.locator('body').innerText()).trim()
  expect(body).not.toMatch(/^\{/)
})
