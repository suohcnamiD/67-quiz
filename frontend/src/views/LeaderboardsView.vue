<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  useTopPlayers,
  useTopAuthors,
} from '@/api/leaderboard-controller/leaderboard-controller'
import Avatar from '@/components/Avatar.vue'
import Button from '@/components/Button.vue'
import type { LeaderboardEntryDto } from '@/api/openAPIDefinition.schemas'

type Tab = 'players' | 'authors'

const router = useRouter()
const tab = ref<Tab>('players')
const playersPage = ref(0)
const authorsPage = ref(0)

const players = useTopPlayers(computed(() => ({ page: playersPage.value })))
const authors = useTopAuthors(computed(() => ({ page: authorsPage.value })))

const current = computed(() => (tab.value === 'players' ? players : authors))
const currentPage = computed({
  get: () => (tab.value === 'players' ? playersPage.value : authorsPage.value),
  set: (v: number) => {
    if (tab.value === 'players') playersPage.value = v
    else authorsPage.value = v
  },
})

const entries = computed<LeaderboardEntryDto[]>(() => current.value.data.value?.entries ?? [])
const totalPages = computed(() => current.value.data.value?.totalPages ?? 1)
const totalElements = computed(() => current.value.data.value?.totalElements ?? 0)
const you = computed<LeaderboardEntryDto | null>(() => current.value.data.value?.you ?? null)
const isLoading = computed(() => current.value.isLoading.value)

const primaryLabel = computed(() => (tab.value === 'players' ? 'Avg score' : 'Avg rating'))
const secondaryLabel = computed(() => (tab.value === 'players' ? 'attempts' : 'ratings'))
const qualifierHint = computed(() =>
  tab.value === 'players'
    ? 'Qualifying: at least 3 finished attempts.'
    : 'Qualifying: at least 5 ratings across your quizzes.',
)

function fmtPrimary(v: number | undefined): string {
  if (v == null) return '—'
  if (tab.value === 'players') return `${Math.round(v)}%`
  // Rating board: one decimal, trim trailing .0
  return v.toFixed(1).replace(/\.0$/, '')
}

function openProfile(username?: string) {
  if (username) router.push({ name: 'user-profile', params: { username } })
}
</script>

<template>
  <section class="head">
    <div>
      <h1 class="headline-lg">Leaderboards</h1>
      <p class="subtitle body-md">{{ qualifierHint }}</p>
    </div>
    <div class="tabs" role="tablist" aria-label="Leaderboard">
      <button
        type="button"
        role="tab"
        :aria-selected="tab === 'players'"
        :class="['tab', { 'tab--on': tab === 'players' }]"
        @click="tab = 'players'"
      >Top players</button>
      <button
        type="button"
        role="tab"
        :aria-selected="tab === 'authors'"
        :class="['tab', { 'tab--on': tab === 'authors' }]"
        @click="tab = 'authors'"
      >Top authors</button>
    </div>
  </section>

  <p v-if="you" class="you label-md" role="status">
    <span class="you__rank">#{{ you.rank }}</span>
    <span>You are ranked {{ you.rank }} of {{ totalElements }} —</span>
    <strong>{{ fmtPrimary(you.primaryValue) }}</strong>
    <span class="muted">({{ you.secondaryValue }} {{ secondaryLabel }})</span>
  </p>

  <p v-if="isLoading" class="empty body-md">Loading…</p>
  <p v-else-if="!entries.length" class="empty body-md">
    Nobody has crossed the qualifying threshold yet. Be the first.
  </p>

  <ol v-else class="board">
    <li
      v-for="entry in entries"
      :key="`${entry.rank}-${entry.user?.username}`"
      :class="['row', { 'row--you': you && entry.rank === you.rank }]"
    >
      <span class="row__rank" aria-label="Rank">#{{ entry.rank }}</span>
      <button
        type="button"
        class="row__user"
        :title="entry.user?.username ? `View ${entry.user.displayName ?? entry.user.username}'s profile` : ''"
        :disabled="!entry.user?.username"
        @click="openProfile(entry.user?.username)"
      >
        <Avatar
          v-if="entry.user"
          :username="entry.user.username"
          :display-name="entry.user.displayName"
          :initials-only="!entry.user.hasAvatar"
          :size="36"
        />
        <span class="row__name">{{ entry.user?.displayName ?? entry.user?.username ?? '—' }}</span>
      </button>
      <span class="row__primary" :title="primaryLabel">
        <strong>{{ fmtPrimary(entry.primaryValue) }}</strong>
        <span class="label-sm muted">{{ primaryLabel }}</span>
      </span>
      <span class="row__secondary label-sm muted">
        {{ entry.secondaryValue }} {{ secondaryLabel }}
      </span>
    </li>
  </ol>

  <div v-if="totalPages > 1" class="pager">
    <Button variant="ghost" :disabled="currentPage === 0" @click="currentPage = Math.max(0, currentPage - 1)">Previous</Button>
    <span class="label-sm muted">Page {{ currentPage + 1 }} / {{ totalPages }}</span>
    <Button variant="ghost" :disabled="currentPage + 1 >= totalPages" @click="currentPage = currentPage + 1">Next</Button>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
  flex-wrap: wrap;
}
.subtitle {
  color: var(--on-surface-variant);
  margin: var(--space-xs) 0 0;
}
.tabs {
  display: inline-flex;
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
  padding: 4px;
  gap: 2px;
}
.tab {
  appearance: none;
  background: transparent;
  border: 0;
  color: var(--on-surface-variant);
  font: inherit;
  font-weight: 600;
  padding: 8px 16px;
  border-radius: var(--radius);
  cursor: pointer;
  min-height: 36px;
}
.tab:hover { color: var(--on-surface); }
.tab--on {
  background: var(--surface-container-high);
  color: var(--on-surface);
}

.you {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
  padding: var(--space-sm) var(--space-md);
  background: var(--primary-container);
  color: var(--on-primary-container);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-lg);
  position: sticky;
  top: var(--space-md);
  z-index: 1;
}
.you__rank {
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  font-size: 1.1rem;
}

.empty {
  color: var(--on-surface-variant);
}

.board {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.row {
  display: grid;
  grid-template-columns: 56px 1fr auto auto;
  align-items: center;
  gap: var(--space-md);
  padding: 10px var(--space-md);
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
}
.row--you {
  border-color: var(--primary-container);
  background: color-mix(in srgb, var(--primary-container) 16%, var(--surface-container));
}
.row__rank {
  font-variant-numeric: tabular-nums;
  font-weight: 800;
  color: var(--on-surface);
  font-size: 1.05rem;
}
.row__user {
  appearance: none;
  background: transparent;
  border: 0;
  color: inherit;
  font: inherit;
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  text-align: left;
  cursor: pointer;
  min-width: 0;
}
.row__user:disabled { cursor: default; }
.row__user:hover:not(:disabled) .row__name { text-decoration: underline; }
.row__name {
  font-weight: 600;
  color: var(--on-surface);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row__primary {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  font-variant-numeric: tabular-nums;
  color: var(--on-surface);
}
.row__primary strong {
  font-size: 1.15rem;
  font-weight: 800;
}
.row__secondary {
  min-width: max-content;
}
.muted {
  color: var(--on-surface-variant);
}
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  margin-top: var(--space-lg);
}

@media (max-width: 640px) {
  .head {
    flex-direction: column;
    align-items: stretch;
  }
  .tabs {
    display: grid;
    grid-template-columns: 1fr 1fr;
    width: 100%;
  }
  .tab { text-align: center; }
  .row {
    grid-template-columns: 44px 1fr auto;
    grid-template-areas:
      "rank user primary"
      "rank user secondary";
    row-gap: 2px;
  }
  .row__rank { grid-area: rank; }
  .row__user { grid-area: user; }
  .row__primary { grid-area: primary; }
  .row__secondary { grid-area: secondary; justify-self: end; }
  .you {
    position: static;
  }
}
</style>
