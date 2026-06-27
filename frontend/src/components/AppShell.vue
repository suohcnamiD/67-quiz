<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'
import Button from './Button.vue'
import Avatar from './Avatar.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const menuOpen = ref(false)
const menuRef = ref<HTMLElement | null>(null)
const toggleRef = ref<HTMLButtonElement | null>(null)

function toggleMenu() {
  menuOpen.value = !menuOpen.value
}
function closeMenu() {
  menuOpen.value = false
}

function logout() {
  closeMenu()
  // No backend logout endpoint exposed; clear locally and bounce to /login.
  auth.clear()
  router.push('/login')
}

// Close on route change so navigating from inside the menu actually dismisses it.
watch(() => route.fullPath, closeMenu)

function onDocPointerDown(e: PointerEvent) {
  if (!menuOpen.value) return
  const target = e.target as Node | null
  if (
    target &&
    !menuRef.value?.contains(target) &&
    !toggleRef.value?.contains(target)
  ) {
    closeMenu()
  }
}
function onKeydown(e: KeyboardEvent) {
  if (menuOpen.value && e.key === 'Escape') {
    e.preventDefault()
    closeMenu()
    toggleRef.value?.focus()
  }
}

onMounted(() => {
  document.addEventListener('pointerdown', onDocPointerDown)
  window.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  document.removeEventListener('pointerdown', onDocPointerDown)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div class="shell">
    <header class="topbar">
      <div class="topbar__inner">
        <RouterLink to="/app" class="brand">67quiz</RouterLink>
        <nav class="nav label-md" aria-label="Primary">
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
          <Button class="signout-btn" variant="ghost" @click="logout">Sign out</Button>
          <button
            ref="toggleRef"
            type="button"
            class="menu-toggle"
            aria-label="Open navigation menu"
            aria-haspopup="menu"
            :aria-expanded="menuOpen"
            aria-controls="topbar-menu"
            @click="toggleMenu"
          >
            <span class="menu-toggle__bar" aria-hidden="true" />
            <span class="menu-toggle__bar" aria-hidden="true" />
            <span class="menu-toggle__bar" aria-hidden="true" />
          </button>
        </div>
      </div>
      <Transition name="menu">
        <div
          v-if="menuOpen"
          id="topbar-menu"
          ref="menuRef"
          class="menu"
          role="menu"
          aria-label="Primary navigation"
        >
          <RouterLink
            to="/app"
            class="menu__item"
            role="menuitem"
            exact-active-class="menu__item--active"
            @click="closeMenu"
          >Browse</RouterLink>
          <RouterLink
            to="/app/quiz/new"
            class="menu__item"
            role="menuitem"
            exact-active-class="menu__item--active"
            @click="closeMenu"
          >New quiz</RouterLink>
          <RouterLink
            v-if="auth.isAuthenticated()"
            to="/app/profile"
            class="menu__item"
            role="menuitem"
            exact-active-class="menu__item--active"
            @click="closeMenu"
          >Your profile</RouterLink>
          <hr class="menu__sep" />
          <button
            type="button"
            class="menu__item menu__item--danger"
            role="menuitem"
            @click="logout"
          >Sign out</button>
        </div>
      </Transition>
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

/* Hamburger button — hidden on desktop, shown on mobile. */
.menu-toggle {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 1px solid var(--outline-variant);
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  transition: border-color 120ms ease, background-color 120ms ease;
}
.menu-toggle:hover {
  border-color: var(--outline);
  background: var(--surface-container-high);
}
.menu-toggle__bar {
  display: block;
  width: 18px;
  height: 2px;
  margin: 0 auto;
  background: var(--on-surface);
  border-radius: 1px;
}

/* The sheet itself drops down from the topbar. */
.menu {
  position: absolute;
  top: 100%;
  right: var(--margin-desktop);
  margin-top: var(--space-sm);
  min-width: 14rem;
  max-width: calc(100vw - 2 * var(--margin-mobile));
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-overlay);
  padding: var(--space-xs);
  display: flex;
  flex-direction: column;
}
.menu__item {
  appearance: none;
  display: block;
  padding: 10px 14px;
  border: 0;
  border-radius: var(--radius);
  background: transparent;
  color: var(--on-surface);
  text-align: left;
  font: inherit;
  cursor: pointer;
  text-decoration: none;
}
.menu__item:hover,
.menu__item:focus-visible {
  background: var(--surface-container-high);
}
.menu__item--active {
  color: var(--on-secondary-container);
  font-weight: 600;
}
.menu__item--danger {
  color: var(--error);
}
.menu__sep {
  border: 0;
  border-top: 1px solid var(--outline-variant);
  margin: var(--space-xs) 0;
}

/* Mount/unmount transition for the menu. */
.menu-enter-active,
.menu-leave-active {
  transition: opacity 120ms ease, transform 160ms cubic-bezier(0.16, 1, 0.3, 1);
}
.menu-enter-from,
.menu-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
@media (prefers-reduced-motion: reduce) {
  .menu-enter-active,
  .menu-leave-active {
    transition: none;
  }
  .menu-enter-from,
  .menu-leave-to {
    transform: none;
  }
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
  .me__name {
    display: none;
  }
}

/* Replace tabs with the hamburger menu at <=640 px. The Sign-out button
 * goes into the menu too, so the topbar carries only brand + avatar +
 * menu trigger. */
@media (max-width: 640px) {
  .nav,
  .signout-btn {
    display: none;
  }
  .menu-toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
  .menu {
    right: var(--margin-mobile);
  }
}

@media (max-width: 480px) {
  .brand {
    font-size: 18px;
  }
  .me {
    padding: 2px;
  }
}
</style>
