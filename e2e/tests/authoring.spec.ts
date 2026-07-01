import { test, expect, type Page } from '@playwright/test'

async function registerAndLogin(page: Page): Promise<string> {
  const username = `a${Date.now().toString(36)}${Math.floor(Math.random() * 100)}`.slice(0, 16)
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

async function addQuestion(
  page: Page,
  quizId: string,
  text: string,
  type: 'SINGLE_CHOICE' | 'MULTI_CHOICE' = 'MULTI_CHOICE',
  options: { text: string; correct: boolean }[] = [
    { text: 'A', correct: true },
    { text: 'B', correct: false },
  ],
): Promise<string> {
  const res = await page.request.post('http://localhost:5173/api/question', {
    data: { quizId, text, type, options },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(res.ok()).toBeTruthy()
  const list = await res.json()
  // The endpoint returns the full question list; the just-added one is last.
  return list[list.length - 1].id as string
}

test('Remove button deletes the question from the list', async ({ page }) => {
  page.on('dialog', (d) => void d.accept())
  await registerAndLogin(page)
  const quizId = await createQuiz(page, `Del ${Date.now()}`)
  await addQuestion(page, quizId, 'What is 2 + 2?')
  await addQuestion(page, quizId, 'Capital of France?')

  await page.goto(`/app/quiz/${quizId}/edit`)
  await page.waitForLoadState('networkidle')
  await expect(page.getByText('What is 2 + 2?')).toBeVisible()
  await expect(page.getByText('Capital of France?')).toBeVisible()

  // Wait for the DELETE response so we know the server processed the removal
  // before checking the DOM (the cache invalidation otherwise races us).
  const removed = page.waitForResponse(
    (res) => res.url().includes('/api/question/') && res.request().method() === 'DELETE',
    { timeout: 10_000 },
  )
  await page
    .locator('li', { hasText: 'What is 2 + 2?' })
    .getByRole('button', { name: 'Remove' })
    .click()
  // ConfirmDialog appears for the destructive action.
  await page.getByRole('dialog').getByRole('button', { name: 'Remove' }).click()
  const deleteResponse = await removed
  expect(deleteResponse.status()).toBe(200)
  await expect(page.getByText('What is 2 + 2?')).toBeHidden({ timeout: 10_000 })
  await expect(page.getByText('Capital of France?')).toBeVisible()
})

test('Edit updates the question text inline', async ({ page }) => {
  await registerAndLogin(page)
  const quizId = await createQuiz(page, `Edit ${Date.now()}`)
  await addQuestion(page, quizId, 'Original text')

  await page.goto(`/app/quiz/${quizId}/edit`)
  await page.waitForLoadState('networkidle')
  await page
    .locator('li', { hasText: 'Original text' })
    .getByRole('button', { name: 'Edit' })
    .click()

  // Edit form now in-place inside the only question card. Update the text.
  const card = page.locator('.qlist > li').first()
  const input = card.getByRole('textbox', { name: 'Question text' })
  await expect(input).toHaveValue('Original text')
  await input.fill('Edited text')
  await card.getByRole('button', { name: 'Save changes' }).click()

  // Edit form collapses; the card now reads the new text.
  await expect(page.getByText('Edited text')).toBeVisible({ timeout: 10_000 })
  await expect(page.getByText('Original text')).toBeHidden()

  // Persists across a refresh.
  await page.reload()
  await page.waitForLoadState('networkidle')
  await expect(page.getByText('Edited text')).toBeVisible()
})

test('Editing a single-choice with two correct rejects', async ({ page }) => {
  await registerAndLogin(page)
  const quizId = await createQuiz(page, `EditBad ${Date.now()}`)
  const qId = await addQuestion(page, quizId, 'Initial', 'MULTI_CHOICE', [
    { text: 'A', correct: true },
    { text: 'B', correct: false },
  ])

  // The server rejection is the actual safety net — the UI form trims down
  // to one correct on switch, but we want to be sure a misbehaving client
  // can't bypass that. PATCH directly with a bad shape.
  const bad = await page.request.patch(`http://localhost:5173/api/question/${qId}`, {
    data: {
      text: 'bad',
      type: 'SINGLE_CHOICE',
      options: [
        { text: 'A', correct: true },
        { text: 'B', correct: true },
      ],
    },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(bad.status()).toBe(400)
  const body = await bad.json()
  expect(body.errors?.[0]?.code).toBe('INVALID_QUESTION_SHAPE')
})
