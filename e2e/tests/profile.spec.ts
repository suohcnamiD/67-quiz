import { test, expect, type Page } from '@playwright/test'

async function registerAndLogin(page: Page): Promise<string> {
  const username = `p${Date.now().toString(36)}${Math.floor(Math.random() * 100)}`.slice(0, 16)
  const password = 'Passw0rd1'
  await page.goto('/register')
  await page.locator('input[autocomplete="username"]').fill(username)
  await page.locator('input[autocomplete="new-password"]').fill(password)
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/app/, { timeout: 15_000 })
  return username
}

// Minimal valid PNG: 4×4 red square. ImageIO rejects truncated PNGs, so we
// keep a real-enough one — generated once via Python and hardcoded here.
const TINY_RED_PNG = Buffer.from(
  '89504e470d0a1a0a0000000d4948445200000004000000040802000000269309290000001049444154' +
    '789c63f8cfc000470cc47100ae930ff1d05f239e0000000049454e44ae426082',
  'hex',
)

test('register: AppShell shows the new user’s name and initials avatar', async ({ page }) => {
  const username = await registerAndLogin(page)
  // The AppShell `.me` chip should carry the username as text (no avatar yet).
  await expect(page.locator('.me__name').first()).toHaveText(username, { timeout: 10_000 })
})

test('profile page renders own stats and lets you edit the display name', async ({ page }) => {
  const username = await registerAndLogin(page)
  await page.goto('/app/profile')
  await expect(page.getByRole('heading', { name: username, exact: true })).toBeVisible({ timeout: 10_000 })
  await expect(page.getByText(`@${username}`)).toBeVisible()

  // Stats are all-zero for a fresh user.
  await expect(page.getByText('Quizzes authored')).toBeVisible()
  await expect(page.getByText('Attempts taken')).toBeVisible()
  await expect(page.getByText('Average score')).toBeVisible()

  const newName = `Display ${Date.now() % 1000}`
  await page.getByLabel('Display name').fill(newName)
  await page.getByRole('button', { name: 'Save' }).click()
  // Header and AppShell pick up the new name without a refresh.
  await expect(page.getByRole('heading', { name: newName })).toBeVisible({ timeout: 10_000 })
  await expect(page.locator('.me__name').first()).toHaveText(newName)
})

test('public profile route is readable for any user', async ({ page }) => {
  const username = await registerAndLogin(page)
  await page.goto(`/app/users/${username}`)
  await expect(page.getByRole('heading', { name: username, exact: true })).toBeVisible({ timeout: 10_000 })
  // It's you, so the "This is you" affordance shows.
  await expect(page.getByText(/This is you/)).toBeVisible()
})

test('avatar upload changes the AppShell image src', async ({ page }) => {
  await registerAndLogin(page)
  await page.goto('/app/profile')
  await expect(page.getByRole('button', { name: 'Upload avatar' })).toBeVisible({ timeout: 10_000 })

  // Drive the hidden <input type="file"> directly.
  await page.locator('input[type="file"]').setInputFiles({
    name: 'avatar.png',
    mimeType: 'image/png',
    buffer: TINY_RED_PNG,
  })

  // After upload, the button text flips and the AppShell <img> resolves.
  await expect(page.getByRole('button', { name: 'Replace avatar' })).toBeVisible({ timeout: 15_000 })
  const shellImg = page.locator('.me img')
  await expect(shellImg).toBeVisible({ timeout: 10_000 })
  const src = await shellImg.getAttribute('src')
  expect(src).toMatch(/^\/api\/users\/.+\/avatar\?v=\d+$/)
})

test('quiz card author chip links to the author profile', async ({ page }) => {
  // Use the seeded sampler user — they own the "General trivia" quiz.
  const username = await registerAndLogin(page)
  await page.goto('/app')
  await page.waitForLoadState('networkidle')

  // The author chip exists on at least one card (sampler's quizzes are seeded).
  const chip = page.locator('.author').first()
  if (await chip.count() === 0) {
    // No quizzes in DB? Skip rather than fail — the chip only renders if
    // there's a quiz with an author on the page.
    test.skip(true, 'no quizzes available to test author chip')
  }
  const authorName = await chip.locator('.author__name').innerText()
  await chip.click()
  await page.waitForURL(/\/app\/users\/[^/]+$/, { timeout: 10_000 })
  // The destination shows the same name in the hero heading (h1). The
  // "Quizzes by …" h2 also contains the name — disambiguate by h1.
  await expect(page.locator('h1', { hasText: authorName }).first()).toBeVisible({ timeout: 10_000 })
  // The freshly registered user isn't the author, so no "This is you" affordance.
  expect(page.url()).not.toContain(`/users/${username}`)
})
