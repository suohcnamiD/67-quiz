<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'
import {
  useListNotifications,
  useMarkNotificationRead,
  useMarkAllNotificationsRead,
  getUnreadCountQueryKey,
  getListNotificationsQueryKey,
} from '@/api/notification-controller/notification-controller'
import type { NotificationDto } from '@/api/openAPIDefinition.schemas'
import Button from '@/components/Button.vue'

const router = useRouter()
const qc = useQueryClient()

const page = ref(0)
const list = useListNotifications(computed(() => ({ page: page.value })))
const items = computed<NotificationDto[]>(
  () => list.data.value?._embedded?.notifications ?? [],
)
const totalPages = computed(() => list.data.value?.page?.totalPages ?? 1)
const totalElements = computed(() => list.data.value?.page?.totalElements ?? 0)

const markRead = useMarkNotificationRead()
const markAllRead = useMarkAllNotificationsRead()

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

async function activate(n: NotificationDto) {
  if (!n.id) return
  if (!n.read) {
    try {
      await markRead.mutateAsync({ id: n.id })
      qc.invalidateQueries({ queryKey: getUnreadCountQueryKey() })
      qc.invalidateQueries({ queryKey: getListNotificationsQueryKey() })
    } catch { /* navigation still happens */ }
  }
  router.push(targetOf(n))
}

async function clearAll() {
  try {
    await markAllRead.mutateAsync()
    qc.invalidateQueries({ queryKey: getUnreadCountQueryKey() })
    qc.invalidateQueries({ queryKey: getListNotificationsQueryKey() })
  } catch { /* swallow */ }
}
</script>

<template>
  <section class="head">
    <div>
      <h1 class="headline-lg">Notifications</h1>
      <p v-if="totalElements > 0" class="subtitle body-md muted">
        {{ totalElements }} total
      </p>
    </div>
    <Button v-if="totalElements > 0" variant="ghost" @click="clearAll">Mark all read</Button>
  </section>

  <p v-if="list.isLoading.value" class="empty body-md">Loading…</p>
  <p v-else-if="!items.length" class="empty body-md">
    Nothing here yet. Comments on your profile, ratings + attempts on your
    quizzes, and changes to your leaderboard rank will land here.
  </p>

  <ul v-else class="list">
    <li v-for="n in items" :key="n.id">
      <button
        type="button"
        :class="['item', { 'item--unread': !n.read }]"
        @click="activate(n)"
      >
        <span class="item__summary body-md">{{ summary(n) }}</span>
        <span class="item__time body-md muted">{{ fmtRelative(n.createdAt) }}</span>
      </button>
    </li>
  </ul>

  <div v-if="totalPages > 1" class="pager">
    <Button variant="ghost" :disabled="page === 0" @click="page = Math.max(0, page - 1)">Previous</Button>
    <span class="body-md muted">Page {{ page + 1 }} / {{ totalPages }}</span>
    <Button variant="ghost" :disabled="page + 1 >= totalPages" @click="page = page + 1">Next</Button>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}
.subtitle {
  margin: var(--space-xs) 0 0;
}
.muted { color: var(--on-surface-variant); }
.empty { color: var(--on-surface-variant); }
.list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  appearance: none;
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  text-align: left;
  font: inherit;
  color: var(--on-surface);
  cursor: pointer;
  transition: background-color 120ms ease, border-color 120ms ease;
}
.item:hover { background: var(--surface-container-high); }
.item--unread {
  background: color-mix(in srgb, var(--primary-container) 14%, var(--surface-container));
  border-color: color-mix(in srgb, var(--primary-container) 40%, var(--outline-variant));
}
.item__summary { font-weight: 500; }
.item__time { font-size: 0.8125rem; }
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  margin-top: var(--space-lg);
}
</style>
