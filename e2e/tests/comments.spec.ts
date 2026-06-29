import { test, expect, type Page, type APIRequestContext } from '@playwright/test'

const BASE = 'http://localhost:5173'
const PASSWORD = 'Passw0rd1'

async function registerUI(page: Page): Promise<string> {
  const username = `c${Date.now().toString(36)}${Math.floor(Math.random() * 1000)}`.slice(0, 16)
  await page.goto('/register')
  await page.locator('input[autocomplete="username"]').fill(username)
  await page.locator('input[autocomplete="new-password"]').fill(PASSWORD)
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/app/, { timeout: 15_000 })
  return username
}

async function logout(page: Page): Promise<void> {
  page.on('dialog', async (d) => { await d.accept() })
  await page.getByRole('button', { name: /sign out/i }).first().click().catch(() => {})
  // Confirm dialog
  const confirm = page.getByRole('dialog').getByRole('button', { name: /sign out/i })
  if (await confirm.count()) await confirm.click()
  await page.waitForURL(/\/login/, { timeout: 10_000 }).catch(() => {})
}

async function registerApi(req: APIRequestContext): Promise<string> {
  const username = `c${Date.now().toString(36)}${Math.floor(Math.random() * 1000)}`.slice(0, 16)
  await req.post(`${BASE}/api/authentication/register`, {
    data: { username, password: PASSWORD },
    headers: { 'Content-Type': 'application/json' },
  })
  return username
}

test('signed-in user can post a comment on someone else\'s profile and delete their own', async ({ page, browser }) => {
  // Create a target user first via an isolated context
  const ctx = await browser.newContext()
  const targetUsername = await registerApi(ctx.request)
  await ctx.close()

  // Sign in as a second user and visit the target's profile
  await registerUI(page)
  await page.goto(`/app/users/${targetUsername}`)
  await expect(page.getByRole('heading', { name: 'Comments' })).toBeVisible()

  const body = `Hello ${Date.now()}`
  await page.getByPlaceholder(/leave a comment/i).fill(body)
  await page.getByRole('button', { name: 'Post' }).click()
  await expect(page.getByText(body)).toBeVisible({ timeout: 5_000 })

  // Delete (we authored it → can delete)
  const item = page.locator('.item', { hasText: body })
  await expect(item).toBeVisible()
  page.on('dialog', async (d) => { await d.accept() })
  await item.getByRole('button', { name: /delete comment/i }).click()
  // ConfirmDialog
  await page.getByRole('dialog').getByRole('button', { name: 'Delete' }).click()
  await expect(page.getByText(body)).toHaveCount(0)
})

test('empty comment is rejected with inline error', async ({ page, browser }) => {
  const ctx = await browser.newContext()
  const targetUsername = await registerApi(ctx.request)
  await ctx.close()

  await registerUI(page)
  await page.goto(`/app/users/${targetUsername}`)
  await page.getByRole('button', { name: 'Post' }).click()
  await expect(page.getByText(/write something before posting/i)).toBeVisible()
})

test('profile owner can delete any comment on their own profile', async ({ page, browser }) => {
  // Owner registers, then a separate guest posts a comment via the API.
  const ownerUsername = await registerUI(page)

  // Guest registers + posts (cookies kept in a separate context).
  const ctx = await browser.newContext()
  const guestUsername = await registerApi(ctx.request)
  const body = `Guest says ${Date.now()}`
  const post = await ctx.request.post(`${BASE}/api/users/${ownerUsername}/comments`, {
    data: { body },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(post.ok()).toBeTruthy()
  await ctx.close()

  // Owner views own profile → sees the comment and can delete it.
  await page.goto('/app/profile')
  await expect(page.getByText(body)).toBeVisible({ timeout: 5_000 })
  const item = page.locator('.item', { hasText: body })
  page.on('dialog', async (d) => { await d.accept() })
  await item.getByRole('button', { name: /delete comment/i }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Delete' }).click()
  await expect(page.getByText(body)).toHaveCount(0)
  // Suppress unused-variable warning for guestUsername (used only as a label).
  expect(guestUsername).toBeTruthy()
})

test('non-owner non-author cannot see the delete button', async ({ page, browser }) => {
  // A leaves a comment on B's profile, then C visits B's profile and sees no delete affordance.
  const targetCtx = await browser.newContext()
  const targetUsername = await registerApi(targetCtx.request)
  await targetCtx.close()

  const authorCtx = await browser.newContext()
  await registerApi(authorCtx.request)
  const body = `Witness ${Date.now()}`
  const r = await authorCtx.request.post(`${BASE}/api/users/${targetUsername}/comments`, {
    data: { body },
    headers: { 'Content-Type': 'application/json' },
  })
  expect(r.ok()).toBeTruthy()
  await authorCtx.close()

  // Sign in as a third user (C) and visit B's profile.
  await registerUI(page)
  await page.goto(`/app/users/${targetUsername}`)
  const item = page.locator('.item', { hasText: body })
  await expect(item).toBeVisible({ timeout: 5_000 })
  await expect(item.getByRole('button', { name: /delete comment/i })).toHaveCount(0)
})
