import { test, expect } from '@playwright/test'
import * as fs from 'fs'

const BASE = 'http://localhost:5173'
const PASSWORD = 'Passw0rd1'

// 11-byte minimum RIFF WebP header (gibberish, won't decode) — we want a real
// WebP, so look for the cached probe file at /tmp/probe.webp. The dev created
// it with `cwebp` and the repo doesn't ship binary fixtures.
function findWebpFixture(): string | null {
  const candidates = ['/tmp/probe.webp']
  for (const c of candidates) if (fs.existsSync(c)) return c
  return null
}

test('avatar upload accepts a real WebP file', async ({ page }) => {
  const webp = findWebpFixture()
  test.skip(!webp, 'No WebP fixture at /tmp/probe.webp — generate one with `cwebp some.jpg -o /tmp/probe.webp`')

  const username = `w${Date.now().toString(36)}${Math.floor(Math.random() * 1000)}`.slice(0, 16)
  await page.goto('/register')
  await page.locator('input[autocomplete="username"]').fill(username)
  await page.locator('input[autocomplete="new-password"]').fill(PASSWORD)
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/app/, { timeout: 15_000 })

  // Upload via the API to avoid clicking through the avatar modal; the API is
  // what the modal calls and the bug we're guarding against is server-side.
  const res = await page.request.put(`${BASE}/api/users/me/avatar`, {
    multipart: { file: { name: 'avatar.webp', mimeType: 'image/webp', buffer: fs.readFileSync(webp!) } },
  })
  expect(res.ok()).toBeTruthy()
  const profile = await res.json()
  expect(profile.hasAvatar).toBe(true)

  // The avatar endpoint should now return an image (JPEG, since BE
  // re-encodes regardless of input type).
  const fetch = await page.request.get(`${BASE}/api/users/${username}/avatar`)
  expect(fetch.ok()).toBeTruthy()
  expect(fetch.headers()['content-type']).toContain('image/jpeg')
  expect((await fetch.body()).length).toBeGreaterThan(0)
})

test('avatar upload rejects bytes that look like an image by extension but aren\'t', async ({ page }) => {
  const username = `w${Date.now().toString(36)}${Math.floor(Math.random() * 1000)}`.slice(0, 16)
  await page.goto('/register')
  await page.locator('input[autocomplete="username"]').fill(username)
  await page.locator('input[autocomplete="new-password"]').fill(PASSWORD)
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/app/, { timeout: 15_000 })

  // 64 bytes of garbage masquerading as a WebP. The BE's whitelist will pass
  // (content-type header), but ImageIO.read returns null → INVALID_IMAGE.
  const garbage = Buffer.alloc(64, 0x42)
  const res = await page.request.put(`${BASE}/api/users/me/avatar`, {
    multipart: { file: { name: 'fake.webp', mimeType: 'image/webp', buffer: garbage } },
  })
  expect(res.status()).toBe(400)
  const body = await res.json()
  expect(body.errors?.[0]?.code).toBe('INVALID_IMAGE')
})
