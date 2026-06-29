import { isAxiosError } from 'axios'
import type { Router } from 'vue-router'
import { AXIOS_INSTANCE } from './axios'
import { errorMessage, firstErrorCode, httpStatus } from './errors'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

// Endpoint substrings whose errors are owned by the view (login form etc.) —
// the interceptor should not toast or redirect for these.
const SILENT_PATHS = ['/authentication/login', '/authentication/register']

// Endpoints whose 401 is expected and shouldn't kick the user to /login.
// `getOwnProfile` returns 401 on first load when anonymous; that's how the
// auth store determines session status.
const ANONYMOUS_PROBE_PATHS = ['/users/me', '/authentication/me']

function isSilent(url?: string): boolean {
  if (!url) return false
  return SILENT_PATHS.some((p) => url.includes(p))
}

function isAnonProbe(url?: string): boolean {
  if (!url) return false
  return ANONYMOUS_PROBE_PATHS.some((p) => url.includes(p))
}

export function installAxiosInterceptors(router: Router): void {
  AXIOS_INSTANCE.interceptors.response.use(
    (response) => response,
    (error: unknown) => {
      if (!isAxiosError(error)) return Promise.reject(error)

      const status = httpStatus(error)
      const url = error.config?.url
      const code = firstErrorCode(error)
      const silent = isSilent(url)
      const anonProbe = isAnonProbe(url)

      // Session expired or anonymous on a protected route → boot to login.
      if (status === 401 && !silent && !anonProbe) {
        const auth = useAuthStore()
        const toast = useToastStore()
        const wasAuthed = auth.isAuthenticated()
        auth.clear()
        const current = router.currentRoute.value
        const next = current.fullPath
        const onAnonRoute = current.matched.some(
          (r) => r.meta.anonymous || r.meta.landing,
        )
        if (!onAnonRoute) {
          if (wasAuthed) {
            toast.push({
              tone: 'warning',
              message: 'Your session has expired. Please sign in again.',
            })
          }
          void router.push({ name: 'login', query: { next } })
        }
        return Promise.reject(error)
      }

      // Network failure (no response received). Surface a generic message so
      // the user knows the request didn't reach the server.
      if (!error.response && !silent) {
        const toast = useToastStore()
        toast.push({
          tone: 'error',
          message: 'Network problem. Check your connection and try again.',
        })
        return Promise.reject(error)
      }

      // Server crash: always toast so the user sees something happened even
      // if the calling view forgot to display the error.
      if (status && status >= 500 && !silent) {
        const toast = useToastStore()
        toast.push({
          tone: 'error',
          message: errorMessage(error),
        })
        return Promise.reject(error)
      }

      // 413 Payload Too Large can fire from Spring's filter before the
      // controller — toast it globally so even uncaught spots show feedback.
      if (status === 413 && !silent) {
        const toast = useToastStore()
        toast.push({
          tone: 'error',
          message: errorMessage(error),
        })
        return Promise.reject(error)
      }

      // 429 Too Many Requests — only the interceptor knows it crossed.
      if (status === 429 && !silent) {
        const toast = useToastStore()
        toast.push({
          tone: 'warning',
          message: errorMessage(error),
        })
        return Promise.reject(error)
      }

      // 403 on a protected route from a known code → toast as well; otherwise
      // let the view handle it.
      if (status === 403 && code === 'FORBIDDEN' && !silent) {
        const toast = useToastStore()
        toast.push({
          tone: 'warning',
          message: errorMessage(error),
        })
      }

      return Promise.reject(error)
    },
  )
}
