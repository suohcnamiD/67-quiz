import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/app' },
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
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/app' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (auth.status === 'unknown') await auth.refresh()

  const wantsAnon = to.matched.some((r) => r.meta.anonymous)
  if (wantsAnon) {
    return auth.isAuthenticated() ? { name: 'browse' } : true
  }
  return auth.isAuthenticated() ? true : { name: 'login', query: { next: to.fullPath } }
})

export default router
