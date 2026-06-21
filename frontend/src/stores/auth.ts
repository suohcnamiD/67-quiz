import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getOwnProfile } from '@/api/user-profile-controller/user-profile-controller'
import { firstErrorCode } from '@/lib/axios'

type AuthStatus = 'unknown' | 'authenticated' | 'anonymous'

export const useAuthStore = defineStore('auth', () => {
  const status = ref<AuthStatus>('unknown')
  const roles = ref<string[]>([])
  const username = ref<string | null>(null)
  const displayName = ref<string | null>(null)
  const hasAvatar = ref<boolean>(false)
  // Bumped every time the user uploads/deletes an avatar so the AppShell <img>
  // refetches instead of showing the cached version.
  const avatarVersion = ref<number>(0)

  const isAuthenticated = () => status.value === 'authenticated'
  const avatarUrl = computed(() =>
    username.value && hasAvatar.value
      ? `/api/users/${username.value}/avatar?v=${avatarVersion.value}`
      : null,
  )

  async function refresh() {
    try {
      const me = await getOwnProfile()
      roles.value = [] // /me on profile doesn't carry roles, but the route guard only needs status
      username.value = me.username ?? null
      displayName.value = me.displayName ?? me.username ?? null
      hasAvatar.value = !!me.hasAvatar
      status.value = 'authenticated'
    } catch (e) {
      const code = firstErrorCode(e)
      if (code) console.warn('auth refresh failed:', code)
      clearLocal()
    }
  }

  function markAuthenticated(_newRoles: string[]) {
    // After register/login the auth controller doesn't return identity. Mark
    // authenticated synchronously so the router guard lets the navigation
    // through, and refresh identity in the background so the AppShell renders
    // the user as soon as the call resolves.
    status.value = 'authenticated'
    void refresh()
  }

  function clear() {
    clearLocal()
  }

  function applyProfileSnapshot(snapshot: {
    username?: string | null
    displayName?: string | null
    hasAvatar?: boolean | null
  }) {
    if (snapshot.username !== undefined) username.value = snapshot.username ?? null
    if (snapshot.displayName !== undefined)
      displayName.value = snapshot.displayName ?? username.value
    if (snapshot.hasAvatar !== undefined) hasAvatar.value = !!snapshot.hasAvatar
  }

  function bumpAvatarVersion() {
    avatarVersion.value++
  }

  function clearLocal() {
    roles.value = []
    username.value = null
    displayName.value = null
    hasAvatar.value = false
    status.value = 'anonymous'
  }

  return {
    status,
    roles,
    username,
    displayName,
    hasAvatar,
    avatarVersion,
    avatarUrl,
    isAuthenticated,
    refresh,
    markAuthenticated,
    clear,
    applyProfileSnapshot,
    bumpAvatarVersion,
  }
})
