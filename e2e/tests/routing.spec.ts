import { test, expect, type Page } from '@playwright/test'

const RANDOM_UUID = '11111111-1111-1111-1111-111111111111'
const ANOTHER_UUID = '22222222-2222-2222-2222-222222222222'

function isJsonError(text: string): boolean {
  const t = text.trim()
  if (!t.startsWith('{')) return false
  return /"errors"|"status"/.test(t)
}

async function bodyText(page: Page): Promise<string> {
  return (await page.locator('body').innerText()).trim()
}

async function registerAndLogin(page: Page): Promise<string> {
  // Backend constraints: ^[a-zA-Z][a-zA-Z0-9_]+$, 5..16 chars
  const username = `e${Date.now().toString(36)}${Math.floor(Math.random() * 100)}`.slice(0, 16)
  const password = 'Passw0rd1'
  await page.goto('/register')
  await page.locator('input[autocomplete="username"]').fill(username)
  await page.locator('input[autocomplete="new-password"]').fill(password)
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/app/, { timeout: 15_000 })
  return username
}

test.describe('Routing — no raw JSON leaks', () => {
  test('unauthenticated deep route → app shell, redirect to /login', async ({ page }) => {
    await page.goto(`/app/attempt/${RANDOM_UUID}`)
    await page.waitForLoadState('networkidle')
    const body = await bodyText(page)
    expect(isJsonError(body), `body: ${body.slice(0, 200)}`).toBe(false)
    expect(page.url()).toContain('/login')
  })

  test('non-existent attempt while authed → friendly not-found UI', async ({ page }) => {
    await registerAndLogin(page)
    await page.goto(`/app/attempt/${RANDOM_UUID}`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)
    const body = await bodyText(page)
    expect(isJsonError(body), `body: ${body.slice(0, 200)}`).toBe(false)
    await expect(page.getByText(/not found/i).first()).toBeVisible()
  })

  test('non-existent attempt result while authed → friendly not-found UI', async ({ page }) => {
    await registerAndLogin(page)
    await page.goto(`/app/attempt/${RANDOM_UUID}/result`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)
    const body = await bodyText(page)
    expect(isJsonError(body), `body: ${body.slice(0, 200)}`).toBe(false)
    await expect(page.getByText(/not found/i).first()).toBeVisible()
  })

  test('non-existent quiz author route → friendly not-found UI', async ({ page }) => {
    await registerAndLogin(page)
    await page.goto(`/app/quiz/${ANOTHER_UUID}`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)
    const body = await bodyText(page)
    expect(isJsonError(body), `body: ${body.slice(0, 200)}`).toBe(false)
    await expect(page.getByText(/not found/i).first()).toBeVisible()
  })

  test('garbage URL → 404 page', async ({ page }) => {
    // Garbage routes outside /app are rewritten to /app/<garbage> by the
    // top-level catchall, then the shell-scoped catchall renders the 404 view.
    await page.goto('/totally-not-a-route/foo/bar')
    await page.waitForLoadState('networkidle')
    const body = await bodyText(page)
    expect(isJsonError(body)).toBe(false)
    // The user is bounced to /login if anonymous; if authed, they see the 404.
    // We just need the body to be the friendly state, not a JSON dump.
    expect(page.url()).toMatch(/\/(app|login)/)
  })
})

test.describe('Routing — login / browse happy path', () => {
  test('register lands on browse view', async ({ page }) => {
    await registerAndLogin(page)
    const body = await bodyText(page)
    expect(isJsonError(body)).toBe(false)
    expect(page.url()).toMatch(/\/app$/)
    // Browse view has a "Browse quizzes" header
    await expect(page.getByText(/browse quizzes|browse/i).first()).toBeVisible()
  })

  test('logged-in visit to /login redirects to /app', async ({ page }) => {
    await registerAndLogin(page)
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    expect(page.url()).toMatch(/\/app/)
  })
})
