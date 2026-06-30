import { test, expect, type Page } from '@playwright/test'

// iPhone-ish — covers the narrowest non-folded phones in real use.
const MOBILE = { width: 375, height: 812 }

async function loginAsSampler(page: Page) {
  await page.setViewportSize(MOBILE)
  await page.goto('/login')
  await page.locator('input[autocomplete="username"]').fill('sampler')
  await page.locator('input[autocomplete="current-password"]').fill('Passw0rd1')
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/app/, { timeout: 15_000 })
}

test('AppShell topbar fits without nav text wrapping at 375px', async ({ page }) => {
  await loginAsSampler(page)
  await page.waitForLoadState('networkidle')
  // The topbar itself should be a single row not taller than ~64px. If
  // "Browse" or "New quiz" wraps, the row will be substantially taller.
  const topbarHeight = await page.locator('.topbar__inner').evaluate(
    (el) => (el as HTMLElement).getBoundingClientRect().height,
  )
  expect(topbarHeight).toBeLessThan(72)
  // The signed-in display name is hidden on mobile; only the avatar remains.
  await expect(page.locator('.me__name')).toBeHidden()
})

test('Profile stats stack to a single column on mobile', async ({ page }) => {
  await loginAsSampler(page)
  await page.goto('/app/profile')
  await page.waitForLoadState('networkidle')
  const stats = page.locator('.stat')
  // Three stat cards, all same column → all share the same x.
  const xs = await stats.evaluateAll((els) =>
    (els as HTMLElement[]).map((e) => Math.round(e.getBoundingClientRect().left)),
  )
  expect(xs.length).toBeGreaterThanOrEqual(3)
  expect(new Set(xs).size).toBe(1)
})

test('AttemptView header stacks: Finish button is full-width on mobile', async ({ page }) => {
  await loginAsSampler(page)
  // Use the Mixed-types quiz so we don't need to author anything.
  const list = await (await page.request.get('http://localhost:5173/api/users/sampler/quizzes?page=0')).json()
  const mixed =
    list._embedded.quizzes.find((q: { name: string }) => q.name === 'Mixed types') ||
    list._embedded.quizzes[0]
  const attempt = await (
    await page.request.post('http://localhost:5173/api/attempt', {
      data: { quizId: mixed.id },
      headers: { 'Content-Type': 'application/json' },
    })
  ).json()
  await page.goto(`/app/attempt/${attempt.id}`)
  await page.waitForLoadState('networkidle')

  const finishBtn = page.getByRole('button', { name: /finish attempt/i }).first()
  const btnW = await finishBtn.evaluate((b) => (b as HTMLElement).getBoundingClientRect().width)
  // Mobile rule expands the button to fill the available row.
  expect(btnW).toBeGreaterThan(300)
})

test('Edit-profile modal fits within the viewport on mobile', async ({ page }) => {
  await loginAsSampler(page)
  await page.goto('/app/profile')
  await page.getByRole('button', { name: 'Edit profile' }).click()
  await expect(page.getByRole('dialog', { name: 'Edit profile' })).toBeVisible()
  const dialogRect = await page.locator('.dialog').evaluate(
    (el) => (el as HTMLElement).getBoundingClientRect(),
  )
  // The dialog horizontally fits in 375px with the modal-container padding.
  expect(dialogRect.width).toBeLessThanOrEqual(MOBILE.width)
  // The dialog is vertically reachable (top is on-screen).
  expect(dialogRect.top).toBeGreaterThanOrEqual(0)
})

test('Browse search results render single-column on mobile', async ({ page }) => {
  await loginAsSampler(page)
  await page.locator('input[type="search"]').fill('sampler')
  await page.waitForTimeout(500)
  // Quiz result cards live in a grid that should collapse to 1 col here.
  const cards = page.locator('.grid > *').first()
  await expect(cards).toBeVisible()
  const cardW = await cards.evaluate((el) => (el as HTMLElement).getBoundingClientRect().width)
  // Cards should span effectively the full content column (~327px after 24px
  // horizontal padding on a 375px viewport).
  expect(cardW).toBeGreaterThan(280)
})

test('mobile hamburger replaces the nav tabs and opens a menu', async ({ page }) => {
  await loginAsSampler(page)
  // Desktop-only nav links are hidden; the inline Sign-out button is gone too.
  const browseTab = page.locator('.topbar__inner .nav a', { hasText: 'Browse' })
  await expect(browseTab).toBeHidden()
  await expect(page.locator('.signout-btn')).toBeHidden()
  // The hamburger is visible.
  const toggle = page.getByRole('button', { name: 'Open navigation menu' })
  await expect(toggle).toBeVisible()

  await toggle.click()
  const menu = page.getByRole('menu', { name: 'Primary navigation' })
  await expect(menu).toBeVisible()
  // All four items present.
  await expect(menu.getByRole('menuitem', { name: 'Browse' })).toBeVisible()
  await expect(menu.getByRole('menuitem', { name: 'New quiz' })).toBeVisible()
  await expect(menu.getByRole('menuitem', { name: 'Your profile' })).toBeVisible()
  await expect(menu.getByRole('menuitem', { name: 'Sign out' })).toBeVisible()
})

test('mobile menu closes on Escape and after navigating', async ({ page }) => {
  await loginAsSampler(page)
  const toggle = page.getByRole('button', { name: 'Open navigation menu' })
  await toggle.click()
  const menu = page.getByRole('menu', { name: 'Primary navigation' })
  await expect(menu).toBeVisible()

  await page.keyboard.press('Escape')
  await expect(menu).toBeHidden()

  // Reopen and navigate via the menu — it should auto-close.
  await toggle.click()
  await menu.getByRole('menuitem', { name: 'New quiz' }).click()
  await page.waitForURL(/\/app\/quiz\/new$/, { timeout: 5_000 })
  await expect(menu).toBeHidden()
})

test('mobile menu FAB sits in the bottom-right thumb zone', async ({ page }) => {
  await loginAsSampler(page)
  const fab = page.getByRole('button', { name: /open navigation menu/i })
  const box = await fab.boundingBox()
  expect(box, 'FAB must have a bounding box').not.toBeNull()
  // Right edge close to the viewport's right edge.
  expect(MOBILE.width - (box!.x + box!.width)).toBeLessThan(40)
  // Bottom edge close to the viewport's bottom — i.e. reachable by a
  // thumb on a phone in the dominant hand. Anything in the top 60% of
  // the viewport is not thumb-reachable.
  expect(box!.y).toBeGreaterThan(MOBILE.height * 0.6)
})
