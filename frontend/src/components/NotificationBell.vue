<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'
import {
  useUnreadCount,
  useListNotifications,
  useMarkNotificationRead,
  useMarkAllNotificationsRead,
  getUnreadCountQueryKey,
  getListNotificationsQueryKey,
} from '@/api/notification-controller/notification-controller'
import type { NotificationDto } from '@/api/openAPIDefinition.schemas'

defineOptions({ name: 'NotificationBell' })

const router = useRouter()
const qc = useQueryClient()

const open = ref(false)
const root = ref<HTMLElement | null>(null)

// Unread count drives the badge. Poll every 30s so it picks up server-side
// emissions (e.g. someone rated your quiz while you sit on the dashboard).
const unread = useUnreadCount({
  query: { refetchInterval: 30_000 },
})
const unreadCount = computed(() => unread.data.value?.count ?? 0)

// Only fetch the list when the dropdown is open — saves a request on every
// route change. Page 0 is fine; the dropdown shows the latest five.
const list = useListNotifications(
  computed(() => ({ page: 0 })),
  { query: { enabled: open } },
)
const notifications = computed<NotificationDto[]>(
  () => list.data.value?._embedded?.notifications ?? [],
)
const top = computed(() => notifications.value.slice(0, 5))

const markRead = useMarkNotificationRead()
const markAllRead = useMarkAllNotificationsRead()

function toggle() {
  open.value = !open.value
}
function close() {
  open.value = false
}

async function activate(n: NotificationDto) {
  if (!n.id) return
  if (!n.read) {
    try {
      await markRead.mutateAsync({ id: n.id })
      qc.invalidateQueries({ queryKey: getUnreadCountQueryKey() })
      qc.invalidateQueries({ queryKey: getListNotificationsQueryKey() })
    } catch { /* the navigation still happens even if mark-read fails */ }
  }
  close()
  router.push(targetOf(n))
}

async function clearAll() {
  try {
    await markAllRead.mutateAsync()
    qc.invalidateQueries({ queryKey: getUnreadCountQueryKey() })
    qc.invalidateQueries({ queryKey: getListNotificationsQueryKey() })
  } catch { /* swallow */ }
}

function targetOf(n: NotificationDto): string {
  const p = (n.payload ?? {}) as Record<string, unknown>
  switch (n.type) {
    case 'COMMENT_RECEIVED':
      return '/app/profile'
    case 'QUIZ_RATED':
    case 'QUIZ_ATTEMPTED': {
      const id = typeof p.quizId === 'string' ? p.quizId : null
      return id ? `/app/quiz/${id}` : '/app/profile'
    }
    case 'RANK_DROPPED':
      return '/app/leaderboards'
    default:
      return '/app/notifications'
  }
}

function summary(n: NotificationDto): string {
  const p = (n.payload ?? {}) as Record<string, unknown>
  const actor = (p.actorDisplayName as string) || (p.actorUsername as string) || 'Someone'
  switch (n.type) {
    case 'COMMENT_RECEIVED':
      return `${actor} commented on your profile.`
    case 'QUIZ_RATED': {
      const score = typeof p.score === 'number' ? ` (${p.score}/10)` : ''
      const quiz = (p.quizName as string) || 'your quiz'
      return `${actor} rated ${quiz}${score}.`
    }
    case 'QUIZ_ATTEMPTED': {
      const quiz = (p.quizName as string) || 'your quiz'
      const score = typeof p.score === 'number' && typeof p.maxScore === 'number'
        ? ` — ${p.score}/${p.maxScore}` : ''
      return `${actor} finished ${quiz}${score}.`
    }
    case 'RANK_DROPPED': {
      const from = typeof p.from === 'number' ? `#${p.from}` : '?'
      const to = typeof p.to === 'number' ? `#${p.to}` : '?'
      const board = p.board === 'AUTHORS' ? 'authors' : 'players'
      return `You dropped from ${from} to ${to} on the ${board} board.`
    }
    default:
      return 'Notification'
  }
}

function fmtRelative(iso?: string): string {
  if (!iso) return ''
  const then = new Date(iso).getTime()
  if (Number.isNaN(then)) return ''
  const diffSec = Math.round((Date.now() - then) / 1000)
  if (Math.abs(diffSec) < 45) return 'just now'
  const diffMin = Math.round(diffSec / 60)
  if (Math.abs(diffMin) < 60) return `${diffMin} min ago`
  const diffHr = Math.round(diffSec / 3600)
  if (Math.abs(diffHr) < 24) return `${diffHr} h ago`
  const diffDay = Math.round(diffSec / 86400)
  if (Math.abs(diffDay) < 30) return `${diffDay} d ago`
  return new Date(iso).toLocaleDateString()
}

function onDocPointerDown(e: PointerEvent) {
  if (!open.value) return
  const target = e.target as Node | null
  if (target && !root.value?.contains(target)) close()
}
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && open.value) {
    close()
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
  <div ref="root" class="bell" :class="{ 'bell--open': open }">
    <button
      type="button"
      class="bell__button"
      :aria-label="unreadCount > 0 ? `Notifications (${unreadCount} unread)` : 'Notifications'"
      :aria-expanded="open"
      aria-haspopup="menu"
      @click="toggle"
    >
      <svg
        class="bell__icon"
        aria-hidden="true"
        viewBox="0 0 24 24"
        width="20"
        height="20"
        fill="none"
        stroke="currentColor"
        stroke-width="1.75"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M6 8a6 6 0 0 1 12 0c0 7 3 8 3 8H3s3-1 3-8" />
        <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
      </svg>
      <span v-if="unreadCount > 0" class="bell__badge label-sm">
        {{ unreadCount > 99 ? '99+' : unreadCount }}
      </span>
    </button>

    <Transition name="menu">
      <div v-if="open" class="bell__menu" role="menu" aria-label="Recent notifications">
        <header class="bell__head">
          <span class="bell__title body-md">Notifications</span>
          <button
            v-if="unreadCount > 0"
            type="button"
            class="bell__mark-all body-md"
            @click="clearAll"
          >Mark all read</button>
        </header>

        <p v-if="list.isLoading.value" class="bell__empty body-md">Loading…</p>
        <p v-else-if="!top.length" class="bell__empty body-md">
          Nothing yet. Quiz attempts, ratings, comments and rank changes will show up here.
        </p>

        <ul v-else class="bell__list">
          <li v-for="n in top" :key="n.id">
            <button
              type="button"
              :class="['bell__item', { 'bell__item--unread': !n.read }]"
              role="menuitem"
              @click="activate(n)"
            >
              <span class="bell__item-summary body-md">{{ summary(n) }}</span>
              <span class="bell__item-time body-md muted">{{ fmtRelative(n.createdAt) }}</span>
            </button>
          </li>
        </ul>

        <footer class="bell__foot">
          <RouterLink to="/app/notifications" class="bell__see-all body-md" @click="close">
            See all
          </RouterLink>
        </footer>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.bell {
  position: relative;
}
.bell__button {
  appearance: none;
  background: transparent;
  border: 0;
  color: var(--on-surface-variant);
  cursor: pointer;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: background-color 120ms ease, color 120ms ease;
}
.bell__button:hover {
  background: var(--surface-container-high);
  color: var(--on-surface);
}
.bell__icon {
  display: block;
}
.bell__badge {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--error-container);
  color: var(--on-error-container);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  text-transform: none;
  letter-spacing: 0;
}

.bell__menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  width: min(360px, calc(100vw - 2 * var(--space-md)));
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.45);
  z-index: 50;
  display: flex;
  flex-direction: column;
  max-height: min(70vh, 480px);
  overflow: hidden;
}
.bell__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-sm) var(--space-md);
  border-bottom: 1px solid var(--outline-variant);
}
.bell__title {
  font-weight: 700;
}
.bell__mark-all {
  appearance: none;
  background: transparent;
  border: 0;
  color: var(--on-surface-variant);
  font: inherit;
  cursor: pointer;
  text-decoration: underline;
}
.bell__mark-all:hover {
  color: var(--on-surface);
}

.bell__empty {
  margin: 0;
  padding: var(--space-md);
  color: var(--on-surface-variant);
}

.bell__list {
  list-style: none;
  padding: 0;
  margin: 0;
  overflow-y: auto;
  flex: 1;
}
.bell__item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  text-align: left;
  appearance: none;
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--outline-variant);
  padding: var(--space-sm) var(--space-md);
  cursor: pointer;
  color: var(--on-surface);
  font: inherit;
  transition: background-color 120ms ease;
}
.bell__item:last-child { border-bottom: 0; }
.bell__item:hover {
  background: var(--surface-container-high);
}
.bell__item--unread {
  background: color-mix(in srgb, var(--primary-container) 12%, transparent);
}
.bell__item--unread:hover {
  background: color-mix(in srgb, var(--primary-container) 22%, transparent);
}
.bell__item-summary { font-weight: 500; }
.bell__item-time { font-size: 0.8125rem; }
.muted { color: var(--on-surface-variant); }

.bell__foot {
  padding: var(--space-sm) var(--space-md);
  border-top: 1px solid var(--outline-variant);
  display: flex;
  justify-content: center;
}
.bell__see-all {
  color: var(--on-surface);
  text-decoration: none;
  font-weight: 600;
}
.bell__see-all:hover {
  text-decoration: underline;
}

.menu-enter-from, .menu-leave-to { opacity: 0; transform: translateY(-4px); }
.menu-enter-active, .menu-leave-active { transition: opacity 160ms ease, transform 160ms ease; }

@media (max-width: 640px) {
  .bell__menu {
    right: 0;
    left: auto;
    width: min(360px, calc(100vw - 2 * var(--space-md)));
  }
}
</style>
