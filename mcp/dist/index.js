import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import axios from 'axios';
import { z } from 'zod';
const BASE_URL = process.env.QUIZ_BASE_URL ?? 'http://localhost:8080';
// Axios instance that stores the session cookie across requests
const http = axios.create({
    baseURL: BASE_URL,
    withCredentials: true,
});
let sessionCookie = null;
http.interceptors.request.use((config) => {
    if (sessionCookie) {
        config.headers['Cookie'] = sessionCookie;
    }
    return config;
});
http.interceptors.response.use((response) => {
    const setCookie = response.headers['set-cookie'];
    if (setCookie) {
        sessionCookie = setCookie.map((c) => c.split(';')[0]).join('; ');
    }
    return response;
});
const server = new McpServer({
    name: '67-quiz',
    version: '1.0.0',
});
// ─── login ───────────────────────────────────────────────────────────────────
server.tool('login', 'Log in to 67-quiz with username and password. Must be called before any other tool.', {
    username: z.string().describe('Your 67-quiz username'),
    password: z.string().describe('Your 67-quiz password'),
}, async ({ username, password }) => {
    try {
        await http.post('/authentication/login', { username, password });
        return { content: [{ type: 'text', text: `Logged in as ${username}` }] };
    }
    catch (e) {
        return { content: [{ type: 'text', text: `Login failed: ${e.response?.data?.errors?.[0]?.code ?? e.message}` }], isError: true };
    }
});
// ─── list_quizzes ─────────────────────────────────────────────────────────────
server.tool('list_quizzes', 'List all available quizzes.', { page: z.number().int().min(0).default(0).describe('Page number (0-based)') }, async ({ page }) => {
    try {
        const res = await http.get(`/quiz?page=${page}`);
        const quizzes = res.data._embedded?.quizzes ?? [];
        if (quizzes.length === 0)
            return { content: [{ type: 'text', text: 'No quizzes found.' }] };
        const lines = quizzes.map((q) => `- ${q.name} (id: ${q.id}, questions: ${q.questionCount ?? 0})`);
        return { content: [{ type: 'text', text: lines.join('\n') }] };
    }
    catch (e) {
        return { content: [{ type: 'text', text: `Error: ${e.message}` }], isError: true };
    }
});
// ─── create_quiz ──────────────────────────────────────────────────────────────
server.tool('create_quiz', 'Create a new quiz. Returns the quiz ID.', {
    name: z.string().min(1).describe('Quiz name'),
    duration_minutes: z.number().int().min(1).default(5).describe('Time limit in minutes'),
}, async ({ name, duration_minutes }) => {
    try {
        const res = await http.post('/quiz', {
            quizName: name,
            quizDuration: `PT${duration_minutes}M`,
        });
        return { content: [{ type: 'text', text: `Created quiz "${name}" with id: ${res.data.id}` }] };
    }
    catch (e) {
        return { content: [{ type: 'text', text: `Error: ${e.response?.data?.errors?.[0]?.code ?? e.message}` }], isError: true };
    }
});
// ─── add_question ─────────────────────────────────────────────────────────────
server.tool('add_question', 'Add a question to an existing quiz.', {
    quiz_id: z.string().uuid().describe('The quiz ID to add the question to'),
    text: z.string().min(1).describe('Question text'),
    type: z.enum(['SINGLE_CHOICE', 'MULTI_CHOICE']).describe('SINGLE_CHOICE = one correct answer, MULTI_CHOICE = multiple correct answers'),
    options: z.array(z.object({
        text: z.string().min(1).describe('Option text'),
        correct: z.boolean().describe('Whether this option is correct'),
    })).min(2).describe('Answer options (at least 2)'),
}, async ({ quiz_id, text, type, options }) => {
    try {
        await http.post('/question', { quizId: quiz_id, text, type, options });
        return { content: [{ type: 'text', text: `Added question "${text}" to quiz ${quiz_id}` }] };
    }
    catch (e) {
        return { content: [{ type: 'text', text: `Error: ${e.response?.data?.errors?.[0]?.code ?? e.message}` }], isError: true };
    }
});
// ─── delete_quiz ──────────────────────────────────────────────────────────────
server.tool('delete_quiz', 'Delete a quiz you authored.', { quiz_id: z.string().uuid().describe('The quiz ID to delete') }, async ({ quiz_id }) => {
    try {
        await http.delete(`/quiz/${quiz_id}`);
        return { content: [{ type: 'text', text: `Deleted quiz ${quiz_id}` }] };
    }
    catch (e) {
        return { content: [{ type: 'text', text: `Error: ${e.response?.data?.errors?.[0]?.code ?? e.message}` }], isError: true };
    }
});
// ─── get_quiz ─────────────────────────────────────────────────────────────────
server.tool('get_quiz', 'Get full details of a quiz you authored, including all questions and options.', { quiz_id: z.string().uuid().describe('The quiz ID') }, async ({ quiz_id }) => {
    try {
        const res = await http.get(`/quiz/authoring/${quiz_id}`);
        const quiz = res.data;
        const lines = [`Quiz: ${quiz.name} (${quiz.duration})`, ''];
        for (const [i, q] of (quiz.questions ?? []).entries()) {
            lines.push(`Q${i + 1} [${q.type}]: ${q.text}`);
            for (const o of q.options ?? []) {
                lines.push(`  ${o.correct ? '✓' : '✗'} ${o.text}`);
            }
        }
        return { content: [{ type: 'text', text: lines.join('\n') }] };
    }
    catch (e) {
        return { content: [{ type: 'text', text: `Error: ${e.response?.data?.errors?.[0]?.code ?? e.message}` }], isError: true };
    }
});
// ─── start ────────────────────────────────────────────────────────────────────
const transport = new StdioServerTransport();
await server.connect(transport);
