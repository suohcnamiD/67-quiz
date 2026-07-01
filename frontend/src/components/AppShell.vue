<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'
import { confirmDialog } from '@/lib/confirmDialog'
import Button from './Button.vue'
import Avatar from './Avatar.vue'
import BrandMark from './BrandMark.vue'
import NotificationBell from './NotificationBell.vue'

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

async function logout() {
  closeMenu()
  const ok = await confirmDialog.open({
    title: 'Sign out?',
    body: "You'll need to sign in again to come back.",
    confirmLabel: 'Sign out',
    danger: true,
  })
  if (!ok) return
  // auth.logout invalidates the server session first so a refresh can't
  // silently re-authenticate via the still-valid cookie, then clears local
  // state.
  await auth.logout()
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
    return
  }
  // Quick-focus the BrowseView search input from anywhere in /app via
  // `/` or ⌘K / Ctrl+K. Skip when typing in another input so the slash
  // isn't intercepted mid-word.
  const tag = (e.target as HTMLElement | null)?.tagName
  const inField =
    tag === 'INPUT' || tag === 'TEXTAREA' || (e.target as HTMLElement | null)?.isContentEditable
  const cmdK = (e.key === 'k' || e.key === 'K') && (e.metaKey || e.ctrlKey)
  const slash = e.key === '/' && !inField
  if (cmdK || slash) {
    const input = document.querySelector<HTMLInputElement>('input[type="search"]')
    if (input) {
      e.preventDefault()
      input.focus()
      input.select()
    }
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
        <RouterLink to="/app" class="brand"><BrandMark size="sm" /></RouterLink>
        <nav class="nav label-md" aria-label="Primary">
          <RouterLink to="/app" exact-active-class="router-link-active">Browse</RouterLink>
          <RouterLink to="/app/leaderboards" exact-active-class="router-link-active">Leaderboards</RouterLink>
          <RouterLink to="/app/quiz/new" exact-active-class="router-link-active">New quiz</RouterLink>
        </nav>
        <div class="actions">
          <NotificationBell v-if="auth.isAuthenticated()" />
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
        </div>
      </div>
    </header>
    <main class="content">
      <RouterView />
    </main>

    <!-- Thumb-reachable navigation. Lives outside the topbar so it can
         anchor to the viewport's bottom-right. Hidden on desktop. -->
    <div class="thumb-nav" :class="{ 'thumb-nav--open': menuOpen }">
      <Transition name="menu">
        <div
          v-if="menuOpen"
          id="thumb-menu"
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
            to="/app/leaderboards"
            class="menu__item"
            role="menuitem"
            exact-active-class="menu__item--active"
            @click="closeMenu"
          >Leaderboards</RouterLink>
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
          <RouterLink
            v-if="auth.isAuthenticated()"
            to="/app/notifications"
            class="menu__item"
            role="menuitem"
            exact-active-class="menu__item--active"
            @click="closeMenu"
          >Notifications</RouterLink>
          <hr class="menu__sep" />
          <button
            type="button"
            class="menu__item menu__item--danger"
            role="menuitem"
            @click="logout"
          >Sign out</button>
        </div>
      </Transition>
      <button
        ref="toggleRef"
        type="button"
        class="fab"
        :aria-label="menuOpen ? 'Close navigation menu' : 'Open navigation menu'"
        aria-haspopup="menu"
        :aria-expanded="menuOpen"
        aria-controls="thumb-menu"
        @click="toggleMenu"
      >
        <span class="fab__bars" :class="{ 'fab__bars--open': menuOpen }" aria-hidden="true">
          <span class="fab__bar" />
          <span class="fab__bar" />
          <span class="fab__bar" />
        </span>
      </button>
    </div>
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

/* Thumb-reachable nav — hidden on desktop. */
.thumb-nav {
  display: none;
  position: fixed;
  bottom: var(--space-lg);
  right: var(--space-lg);
  z-index: 40;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-sm);
  /* The container itself doesn't intercept taps when closed; only the FAB and
   * the menu (when open) do. Keeps middle-of-screen clicks clean. */
  pointer-events: none;
}
.thumb-nav > * {
  pointer-events: auto;
}

.fab {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: 0;
  background: var(--primary-container);
  color: var(--on-primary-container);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.4);
  transition: transform 160ms cubic-bezier(0.16, 1, 0.3, 1), background-color 120ms ease;
}
.fab:hover {
  background: #c92424;
}
.fab:active {
  transform: scale(0.96);
}

/* Animated hamburger → X glyph. */
.fab__bars {
  position: relative;
  width: 22px;
  height: 16px;
}
.fab__bar {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background: currentColor;
  border-radius: 1px;
  transition: transform 220ms cubic-bezier(0.16, 1, 0.3, 1), opacity 160ms ease;
}
.fab__bar:nth-child(1) { top: 0; }
.fab__bar:nth-child(2) { top: 7px; }
.fab__bar:nth-child(3) { top: 14px; }
.fab__bars--open .fab__bar:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}
.fab__bars--open .fab__bar:nth-child(2) {
  opacity: 0;
}
.fab__bars--open .fab__bar:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

/* The menu sheet, expanding upward from above the FAB. */
.menu {
  min-width: 14rem;
  max-width: calc(100vw - 2 * var(--space-lg));
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.45);
  padding: var(--space-xs);
  display: flex;
  flex-direction: column;
  transform-origin: 100% 100%;
}
.menu__item {
  appearance: none;
  display: block;
  padding: 12px 16px;
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

/* Slide+fade from the bottom-right corner (anchored above the FAB). */
.menu-enter-active,
.menu-leave-active {
  transition:
    opacity 140ms ease,
    transform 200ms cubic-bezier(0.16, 1, 0.3, 1);
}
.menu-enter-from,
.menu-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.96);
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
  .fab__bar {
    transition: none;
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

/* Activate the thumb nav at <=640 px and pull the tab/sign-out controls
 * out of the topbar — they now live inside the FAB menu. Leave space at
 * the bottom of the page so content doesn't sit under the FAB. */
@media (max-width: 640px) {
  .nav,
  .signout-btn {
    display: none;
  }
  .thumb-nav {
    display: flex;
  }
  .content {
    padding-bottom: calc(56px + var(--space-xl) + var(--space-lg));
  }
}

@media (max-width: 480px) {
  .brand {
    font-size: 18px;
  }
  .me {
    padding: 2px;
  }
  /* Safe-area aware on phones with home indicators. */
  .thumb-nav {
    bottom: calc(var(--space-md) + env(safe-area-inset-bottom, 0px));
    right: var(--space-md);
  }
}
</style>
