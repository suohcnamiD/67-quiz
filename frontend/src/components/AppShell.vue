<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import Button from './Button.vue'
import Avatar from './Avatar.vue'

const auth = useAuthStore()
const router = useRouter()

const logout = () => {
  // No backend logout endpoint exposed; clear locally and bounce to /login.
  auth.clear()
  router.push('/login')
}
</script>

<template>
  <div class="shell">
    <header class="topbar">
      <div class="topbar__inner">
        <RouterLink to="/app" class="brand">67quiz</RouterLink>
        <nav class="nav label-md">
          <RouterLink to="/app" exact-active-class="router-link-active">Browse</RouterLink>
          <RouterLink to="/app/quiz/new" exact-active-class="router-link-active">New quiz</RouterLink>
        </nav>
        <div class="actions">
          <RouterLink
            v-if="auth.isAuthenticated()"
            to="/app/profile"
            class="me"
            :title="`Signed in as ${auth.displayName ?? auth.username}`"
          >
            <Avatar
              :username="auth.username"
              :display-name="auth.displayName"
              :version="auth.avatarVersion"
              :initials-only="!auth.hasAvatar"
              :size="32"
            />
            <span class="me__name label-md">{{ auth.displayName ?? auth.username }}</span>
          </RouterLink>
          <Button variant="ghost" @click="logout">Sign out</Button>
        </div>
      </div>
    </header>
    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.topbar {
  background: var(--surface);
  border-bottom: 1px solid var(--outline-variant);
  position: sticky;
  top: 0;
  z-index: 10;
}
.topbar__inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-md) var(--margin-desktop);
  display: flex;
  align-items: center;
  gap: var(--space-lg);
}
.brand {
  color: var(--on-surface);
  font-family: var(--font-display);
  font-weight: 800;
  font-style: italic;
  font-size: 22px;
  line-height: 1;
  letter-spacing: -0.03em;
}
.brand:hover {
  text-decoration: none;
}
.nav {
  display: flex;
  gap: var(--space-lg);
  flex: 1;
  min-width: 0;
}
.nav a {
  color: var(--on-surface-variant);
  padding: var(--space-xs) 0;
  white-space: nowrap;
}
.nav a.router-link-active {
  color: var(--on-surface);
  border-bottom: 1px solid var(--primary-container);
}
.actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}
.me {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  color: var(--on-surface);
  background: var(--surface-container-high);
  text-decoration: none;
  transition: background-color 120ms ease;
}
.me:hover {
  background: var(--surface-container);
  text-decoration: none;
}
.me__name {
  font-weight: 600;
  max-width: 12rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.content {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-xl) var(--margin-desktop);
  width: 100%;
  flex: 1;
}
@media (max-width: 768px) {
  .topbar__inner,
  .content {
    padding-left: var(--margin-mobile);
    padding-right: var(--margin-mobile);
  }
  .topbar__inner {
    gap: var(--space-md);
  }
  .nav {
    gap: var(--space-sm);
  }
  .me__name {
    display: none;
  }
}
@media (max-width: 480px) {
  /* On really narrow screens the brand + nav + avatar + sign-out won't all
   * fit; drop the brand link's visible weight so the right-side actions get
   * room. The brand is still the AppShell home link. */
  .brand {
    font-size: 16px;
  }
  .me {
    padding: 2px;
  }
  /* Tighter topbar gaps and a smaller Sign-out so all four pieces fit
   * without the nav text wrapping onto two lines. */
  .topbar__inner {
    gap: var(--space-sm);
  }
  .nav {
    /* Big enough that "Browse" and "New quiz" don't visually merge but small
     * enough that the four topbar pieces still fit a 375px row. */
    gap: 14px;
    font-size: 13px;
  }
  .actions :deep(.btn) {
    padding: 6px 10px;
    font-size: 12px;
  }
}
</style>
