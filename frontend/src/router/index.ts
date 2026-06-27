import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'landing',
      component: () => import('@/views/LandingView.vue'),
      meta: { landing: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { anonymous: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { anonymous: true },
    },
    {
      path: '/app',
      component: () => import('@/components/AppShell.vue'),
      children: [
        { path: '', name: 'browse', component: () => import('@/views/BrowseView.vue') },
        { path: 'quiz/new', name: 'quiz-new', component: () => import('@/views/QuizCreateView.vue') },
        { path: 'quiz/:quizId', name: 'quiz-author', component: () => import('@/views/QuizAuthorView.vue') },
        { path: 'attempt/:attemptId', name: 'attempt', component: () => import('@/views/AttemptView.vue') },
        { path: 'attempt/:attemptId/result', name: 'attempt-result', component: () => import('@/views/AttemptResultView.vue') },
        { path: 'profile', name: 'profile', component: () => import('@/views/ProfileView.vue') },
        { path: 'users/:username', name: 'user-profile', component: () => import('@/views/UserProfileView.vue') },
        // Anything else under /app is a real 404 — show the page inside the shell.
        { path: ':pathMatch(.*)*', name: 'app-not-found', component: () => import('@/views/NotFoundView.vue') },
      ],
    },
    // Top-level catchall: route everything else to /app so the auth guard +
    // shell-scoped 404 take over. (e.g. /something-random redirects to
    // /app/something-random which then renders NotFoundView.)
    { path: '/:pathMatch(.*)*', redirect: (to) => `/app${to.path}` },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (auth.status === 'unknown') await auth.refresh()

  const isLanding = to.matched.some((r) => r.meta.landing)
  if (isLanding) {
    return auth.isAuthenticated() ? { name: 'browse' } : true
  }
  const wantsAnon = to.matched.some((r) => r.meta.anonymous)
  if (wantsAnon) {
    return auth.isAuthenticated() ? { name: 'browse' } : true
  }
  return auth.isAuthenticated() ? true : { name: 'login', query: { next: to.fullPath } }
})

export default router
