<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import Button from './Button.vue'

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
          <span v-if="auth.isAuthenticated()" class="label-sm muted">Signed in</span>
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
  gap: var(--space-md);
  flex: 1;
}
.nav a {
  color: var(--on-surface-variant);
  padding: var(--space-xs) 0;
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
.muted {
  color: var(--on-surface-variant);
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
}
</style>
