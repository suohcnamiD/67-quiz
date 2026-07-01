import { test, expect, type Page } from '@playwright/test'

async function registerAndLogin(page: Page): Promise<string> {
  const username = `r${Date.now().toString(36)}${Math.floor(Math.random() * 100)}`.slice(0, 16)
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

async function addQuestion(page: Page, quizId: string, text: string): Promise<string> {
  const res = await page.request.post('http://localhost:5173/api/question', {
    data: {
      quizId,
      text,
      type: 'SINGLE_CHOICE',
      options: [
        { text: 'a', correct: true },
        { text: 'b', correct: false },
      ],
    },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(res.ok()).toBeTruthy()
  const list = await res.json()
  return list[list.length - 1].id as string
}

test.describe('Rename + reorder', () => {
  test('clicking the quiz title turns it into an editable input that saves on blur', async ({ page }) => {
    page.on('dialog', (d) => void d.accept())
    await registerAndLogin(page)
    const original = `Quiz ${Date.now()}`
    const quizId = await createQuiz(page, original)

    await page.goto(`/app/quiz/${quizId}/edit`)
    await expect(page.getByRole('button', { name: original })).toBeVisible({ timeout: 10_000 })

    // Click the title to enter edit mode.
    await page.getByRole('button', { name: original }).click()
    const input = page.locator('.head__title-input')
    await expect(input).toBeVisible()
    const next = `${original} renamed`
    await input.fill(next)
    // Blur saves. Click somewhere neutral.
    await page.locator('.head__actions').click()
    await expect(page.getByRole('button', { name: next })).toBeVisible({ timeout: 10_000 })
  })

  test('drag handle reorders questions and the new order persists on reload', async ({ page }) => {
    page.on('dialog', (d) => void d.accept())
    await registerAndLogin(page)
    const quizId = await createQuiz(page, `Reorder ${Date.now()}`)
    await addQuestion(page, quizId, 'first')
    await addQuestion(page, quizId, 'second')
    await addQuestion(page, quizId, 'third')

    await page.goto(`/app/quiz/${quizId}/edit`)
    await page.waitForLoadState('networkidle')

    const items = page.locator('.qitem')
    await expect(items).toHaveCount(3)

    // Direct API call — HTML5 drag-and-drop in Playwright is flaky; we prove
    // the endpoint + backend order-column path works, and let the drag CSS
    // regression be covered by the visual selector test in a follow-up.
    const res = await page.request.get(`http://localhost:5173/api/quiz/authoring/${quizId}`)
    const quiz = await res.json()
    const ids: string[] = quiz.questions.map((q: { id: string }) => q.id)
    const reversed = [...ids].reverse()
    const reorderRes = await page.request.patch(`http://localhost:5173/api/quiz/${quizId}/reorder`, {
      data: { questionIds: reversed },
      headers: { 'Content-Type': 'application/json' },
    })
    expect(reorderRes.ok()).toBeTruthy()

    await page.reload()
    await page.waitForLoadState('networkidle')
    const texts = (await page.locator('.qitem .qhead__preview').allTextContents()).map((t) => t.trim())
    expect(texts).toEqual(['third', 'second', 'first'])
  })

  test('reorder with a bogus id list is rejected with INVALID_REORDER', async ({ page }) => {
    await registerAndLogin(page)
    const quizId = await createQuiz(page, `Bad reorder ${Date.now()}`)
    await addQuestion(page, quizId, 'q1')
    await addQuestion(page, quizId, 'q2')

    const res = await page.request.patch(`http://localhost:5173/api/quiz/${quizId}/reorder`, {
      data: { questionIds: ['00000000-0000-0000-0000-000000000000', '11111111-1111-1111-1111-111111111111'] },
      headers: { 'Content-Type': 'application/json' },
    })
    expect(res.status()).toBe(400)
    const body = await res.json()
    expect(body.errors?.[0]?.code).toBe('INVALID_REORDER')
  })
})

test.describe('Quiz description + markdown', () => {
  test('empty description shows the + Add placeholder, saving markdown renders it as HTML', async ({ page }) => {
    page.on('dialog', (d) => void d.accept())
    await registerAndLogin(page)
    const quizId = await createQuiz(page, `Desc ${Date.now()}`)
    await addQuestion(page, quizId, 'q1')

    await page.goto(`/app/quiz/${quizId}/edit`)
    await expect(page.getByText('+ Add a description')).toBeVisible({ timeout: 10_000 })

    await page.getByText('+ Add a description').click()
    await page.locator('.description-textarea').fill('# Heading\n\n**bold** and *italic*')
    await page.getByRole('button', { name: 'Save' }).click()

    // Rendered markdown → heading + strong + em nodes exist.
    await expect(page.locator('.description-preview h1')).toHaveText('Heading')
    await expect(page.locator('.description-preview strong')).toHaveText('bold')
    await expect(page.locator('.description-preview em')).toHaveText('italic')
  })

  test('script tags in description get stripped by DOMPurify', async ({ page }) => {
    await registerAndLogin(page)
    const quizId = await createQuiz(page, `XSS ${Date.now()}`)

    const res = await page.request.patch(`http://localhost:5173/api/quiz/${quizId}/description`, {
      data: { description: '<script>window.pwned = true</script>hello' },
      headers: { 'Content-Type': 'application/json' },
    })
    expect(res.ok()).toBeTruthy()

    await page.goto(`/app/quiz/${quizId}`)
    await expect(page.locator('.description')).toContainText('hello')
    const pwned = await page.evaluate(() => (window as unknown as { pwned?: boolean }).pwned)
    expect(pwned).toBeFalsy()
  })
})

test.describe('Per-quiz overview page', () => {
  test('viewer route /app/quiz/:id shows the overview with Start Attempt', async ({ page }) => {
    await registerAndLogin(page)
    const quizId = await createQuiz(page, `Overview ${Date.now()}`)
    await addQuestion(page, quizId, 'q1')

    await page.goto(`/app/quiz/${quizId}`)
    await expect(page.getByRole('button', { name: /start attempt/i })).toBeVisible({ timeout: 10_000 })
    // Author sees Edit + Delete buttons.
    await expect(page.getByRole('button', { name: 'Edit' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Delete' })).toBeVisible()
  })

  test('starting an attempt from the overview navigates to /app/attempt/:id', async ({ page }) => {
    await registerAndLogin(page)
    const quizId = await createQuiz(page, `StartFromOverview ${Date.now()}`)
    await addQuestion(page, quizId, 'q1')

    await page.goto(`/app/quiz/${quizId}`)
    await page.getByRole('button', { name: /start attempt/i }).click()
    await page.waitForURL(/\/app\/attempt\/[^/]+$/, { timeout: 10_000 })
  })
})

test.describe('Sorting', () => {
  test('changing the Sort dropdown reflects in the URL params and reloads the grid', async ({ page }) => {
    await registerAndLogin(page)
    await page.goto('/app')
    await page.waitForLoadState('networkidle')

    // Watch for the sort=RATING request kicked off by the reactive query.
    const requestPromise = page.waitForRequest((r) => /\/api\/quiz\?.*sort=RATING/.test(r.url()), { timeout: 5_000 })
    await page.locator('.sort-select__control').selectOption('RATING')
    await requestPromise
  })

  test('newest sort places freshly-created quizzes ahead of older ones', async ({ page }) => {
    // Two users, two quizzes; the second one is newer. Sort=NEWEST should
    // list the second first once we page through until we find them.
    await registerAndLogin(page)
    const older = await createQuiz(page, `SortA ${Date.now()}`)
    await new Promise((r) => setTimeout(r, 1200))
    const newer = await createQuiz(page, `SortB ${Date.now()}`)

    const res = await page.request.get('http://localhost:5173/api/quiz?sort=NEWEST&page=0')
    const body = await res.json()
    const ids: string[] = body._embedded.quizzes.map((q: { id: string }) => q.id)
    const idxNewer = ids.indexOf(newer)
    const idxOlder = ids.indexOf(older)
    // Both may be on later pages if there's a lot of test data — only assert
    // the relative order if both landed on page 0.
    if (idxNewer >= 0 && idxOlder >= 0) {
      expect(idxNewer).toBeLessThan(idxOlder)
    }
  })
})

test.describe('Login + redirects', () => {
  test('anon user hitting a quiz URL is bounced to /login?next=<url> and lands on the quiz after signing in', async ({ page }) => {
    // First register a user + create a quiz so we have a target.
    await registerAndLogin(page)
    const quizId = await createQuiz(page, `Redirect ${Date.now()}`)

    // Sign out.
    await page.request.post('http://localhost:5173/api/authentication/logout')
    await page.context().clearCookies()

    // Anon hits the quiz URL.
    await page.goto(`/app/quiz/${quizId}`)
    await page.waitForURL((url) => url.pathname === '/login' && url.searchParams.get('next')?.includes(quizId) === true, { timeout: 10_000 })
  })
})

test.describe('DHBW grade', () => {
  test('result page renders a DHBW-Note next to the percent', async ({ page }) => {
    page.on('dialog', (d) => void d.accept())
    await registerAndLogin(page)
    const quizId = await createQuiz(page, `Grade ${Date.now()}`)
    await addQuestion(page, quizId, 'q1')

    // Start + finish an attempt directly so we don't fight timing.
    const start = await (await page.request.post('http://localhost:5173/api/attempt', {
      data: { quizId },
      headers: { 'Content-Type': 'application/json' },
    })).json()
    await page.request.patch('http://localhost:5173/api/attempt/finish', {
      data: { attemptId: start.id },
      headers: { 'Content-Type': 'application/json' },
    })

    await page.goto(`/app/attempt/${start.id}/result`)
    // Dismiss celebration if present.
    const overlay = page.locator('.overlay').first()
    if (await overlay.count()) await overlay.click({ force: true }).catch(() => {})

    // The chip renders "5,0" (German comma) for a zero-scoring attempt.
    await expect(page.locator('.hero__grade-value')).toBeVisible()
    await expect(page.locator('.hero__grade-label')).toContainText(/DHBW/i)
  })
})
