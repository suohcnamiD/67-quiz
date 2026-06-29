import { test, expect, type APIRequestContext } from '@playwright/test'
import * as fs from 'fs'

const BASE = 'http://localhost:5173'
const PASSWORD = 'Passw0rd1'

function findWebpFixture(): string | null {
  const candidates = ['/tmp/probe.webp']
  for (const c of candidates) if (fs.existsSync(c)) return c
  return null
}

async function registerApi(req: APIRequestContext): Promise<string> {
  const username = `i${Date.now().toString(36)}${Math.floor(Math.random() * 10000)}`.slice(0, 16)
  await req.post(`${BASE}/api/authentication/register`, {
    data: { username, password: PASSWORD },
    headers: { 'Content-Type': 'application/json' },
  })
  return username
}

async function makeQuizWithQuestion(req: APIRequestContext, name: string): Promise<{ quizId: string; questionId: string; optionId: string }> {
  const quizRes = await req.post(`${BASE}/api/quiz`, {
    data: { quizName: name, quizDuration: 'PT5M' },
    headers: { 'Content-Type': 'application/json' },
  })
  const quiz = await quizRes.json()
  await req.post(`${BASE}/api/question`, {
    data: {
      quizId: quiz.id,
      text: 'With image?',
      type: 'SINGLE_CHOICE',
      options: [
        { text: 'no', correct: false },
        { text: 'yes', correct: true },
      ],
    },
    headers: { 'Content-Type': 'application/json' },
  })
  // Reload to get the question + option IDs.
  const fresh = await req.get(`${BASE}/api/quiz/authoring/${quiz.id}`)
  const fullQuiz = await fresh.json()
  const question = fullQuiz.questions[0]
  const option = question.options[0]
  return { quizId: quiz.id, questionId: question.id, optionId: option.id }
}

test('cover image upload roundtrips through PUT + GET', async ({ request }) => {
  const webp = findWebpFixture()
  test.skip(!webp, 'No WebP fixture at /tmp/probe.webp')
  await registerApi(request)
  const { quizId } = await makeQuizWithQuestion(request, `Cover ${Date.now()}`)

  // Upload
  const up = await request.put(`${BASE}/api/quiz/${quizId}/cover`, {
    multipart: { file: { name: 'cover.webp', mimeType: 'image/webp', buffer: fs.readFileSync(webp!) } },
  })
  expect(up.ok()).toBeTruthy()

  // Fetch — public endpoint, returns JPEG
  const get = await request.get(`${BASE}/api/quiz/${quizId}/cover`)
  expect(get.ok()).toBeTruthy()
  expect(get.headers()['content-type']).toContain('image/jpeg')
  expect((await get.body()).length).toBeGreaterThan(0)

  // The quiz authoring DTO should now report hasCover = true.
  const authored = await request.get(`${BASE}/api/quiz/authoring/${quizId}`)
  const data = await authored.json()
  expect(data.hasCover).toBe(true)
})

test('question + option image uploads roundtrip and propagate hasImage', async ({ request }) => {
  const webp = findWebpFixture()
  test.skip(!webp, 'No WebP fixture at /tmp/probe.webp')
  await registerApi(request)
  const { quizId, questionId, optionId } = await makeQuizWithQuestion(request, `Q+O ${Date.now()}`)

  // Question image
  const qUp = await request.put(`${BASE}/api/question/${questionId}/image`, {
    multipart: { file: { name: 'q.webp', mimeType: 'image/webp', buffer: fs.readFileSync(webp!) } },
  })
  expect(qUp.ok()).toBeTruthy()
  const qGet = await request.get(`${BASE}/api/question/${questionId}/image`)
  expect(qGet.headers()['content-type']).toContain('image/jpeg')

  // Option image
  const oUp = await request.put(`${BASE}/api/option/${optionId}/image`, {
    multipart: { file: { name: 'o.webp', mimeType: 'image/webp', buffer: fs.readFileSync(webp!) } },
  })
  expect(oUp.ok()).toBeTruthy()
  const oGet = await request.get(`${BASE}/api/option/${optionId}/image`)
  expect(oGet.headers()['content-type']).toContain('image/jpeg')

  // The hasImage flags should now show on the authoring DTO.
  const authored = await request.get(`${BASE}/api/quiz/authoring/${quizId}`)
  const data = await authored.json()
  expect(data.questions[0].hasImage).toBe(true)
  expect(data.questions[0].options[0].hasImage).toBe(true)
})

test('non-author cannot upload a cover image', async ({ request, browser }) => {
  const webp = findWebpFixture()
  test.skip(!webp, 'No WebP fixture at /tmp/probe.webp')
  await registerApi(request)
  const { quizId } = await makeQuizWithQuestion(request, `Owner-only ${Date.now()}`)

  // A different user attempts to upload.
  const otherCtx = await browser.newContext()
  await registerApi(otherCtx.request)
  const res = await otherCtx.request.put(`${BASE}/api/quiz/${quizId}/cover`, {
    multipart: { file: { name: 'cover.webp', mimeType: 'image/webp', buffer: fs.readFileSync(webp!) } },
  })
  expect(res.status()).toBe(403)
  await otherCtx.close()
})

test('garbage bytes with image content-type are rejected as INVALID_IMAGE', async ({ request }) => {
  await registerApi(request)
  const { quizId } = await makeQuizWithQuestion(request, `Garbage ${Date.now()}`)

  const res = await request.put(`${BASE}/api/quiz/${quizId}/cover`, {
    multipart: { file: { name: 'fake.webp', mimeType: 'image/webp', buffer: Buffer.alloc(64, 0x42) } },
  })
  expect(res.status()).toBe(400)
  const body = await res.json()
  expect(body.errors?.[0]?.code).toBe('INVALID_IMAGE')
})

test('deleting a cover clears hasCover and 404s the GET', async ({ request }) => {
  const webp = findWebpFixture()
  test.skip(!webp, 'No WebP fixture at /tmp/probe.webp')
  await registerApi(request)
  const { quizId } = await makeQuizWithQuestion(request, `Delete ${Date.now()}`)

  await request.put(`${BASE}/api/quiz/${quizId}/cover`, {
    multipart: { file: { name: 'cover.webp', mimeType: 'image/webp', buffer: fs.readFileSync(webp!) } },
  })
  // Confirm it's there.
  expect((await request.get(`${BASE}/api/quiz/${quizId}/cover`)).ok()).toBeTruthy()

  // Delete.
  const del = await request.delete(`${BASE}/api/quiz/${quizId}/cover`)
  expect(del.ok()).toBeTruthy()

  // hasCover is false, GET 404s.
  const authored = await request.get(`${BASE}/api/quiz/authoring/${quizId}`)
  const data = await authored.json()
  expect(data.hasCover).toBe(false)

  const get = await request.get(`${BASE}/api/quiz/${quizId}/cover`)
  expect(get.status()).toBe(404)
})
