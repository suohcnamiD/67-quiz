import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAuthenticationState } from '@/api/authentication-state-controller/authentication-state-controller'
import { firstErrorCode } from '@/lib/axios'

type AuthStatus = 'unknown' | 'authenticated' | 'anonymous'

export const useAuthStore = defineStore('auth', () => {
  const status = ref<AuthStatus>('unknown')
  const roles = ref<string[]>([])

  const isAuthenticated = () => status.value === 'authenticated'

  async function refresh() {
    try {
      const me = await getAuthenticationState()
      roles.value = me.roles ?? []
      status.value = 'authenticated'
    } catch (e) {
      const code = firstErrorCode(e)
      // 401 = anonymous; treat any auth failure as anonymous, surface other errors via console.
      if (code) console.warn('auth refresh failed:', code)
      roles.value = []
      status.value = 'anonymous'
    }
  }

  function markAuthenticated(newRoles: string[]) {
    roles.value = newRoles
    status.value = 'authenticated'
  }

  function clear() {
    roles.value = []
    status.value = 'anonymous'
  }

  return { status, roles, isAuthenticated, refresh, markAuthenticated, clear }
})
