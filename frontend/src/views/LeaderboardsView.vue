<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  useTopPlayers,
  useTopAuthors,
} from '@/api/leaderboard-controller/leaderboard-controller'
import Avatar from '@/components/Avatar.vue'
import Button from '@/components/Button.vue'
import Modal from '@/components/Modal.vue'
import type { LeaderboardEntryDto } from '@/api/openAPIDefinition.schemas'

defineOptions({ name: 'LeaderboardsView' })

type Tab = 'players' | 'authors'

const route = useRoute()
const router = useRouter()
// Initial tab reads from ?tab=authors so dashboard links open the right
// board directly instead of always defaulting to players.
const initialTab: Tab = route.query.tab === 'authors' ? 'authors' : 'players'
const tab = ref<Tab>(initialTab)
// Keep the URL in sync when the user flips tabs so refreshes/back-nav land
// on the same view.
watch(tab, (next) => {
  if (route.query.tab === next) return
  router.replace({ path: route.path, query: { ...route.query, tab: next } })
})
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

const primaryLabel = computed(() => (tab.value === 'players' ? 'Rating' : 'Avg rating'))
const secondaryLabel = computed(() => (tab.value === 'players' ? 'attempts' : 'ratings'))

// Players board: rating is a derived score on a 0–100 scale, not a percent —
// hence the title-cased label and no % suffix. Authors board: avg rating is
// on a 1–10 scale, one decimal.
function fmtPrimary(v: number | null | undefined): string {
  if (v == null) return '—'
  if (tab.value === 'players') return v.toFixed(1).replace(/\.0$/, '')
  return v.toFixed(1).replace(/\.0$/, '')
}
function fmtPercent(v: number | null | undefined): string {
  if (v == null) return '—'
  return `${Math.round(v)}%`
}

function openProfile(username?: string) {
  if (username) router.push({ name: 'user-profile', params: { username } })
}

// ----- Explainer modal --------------------------------------------------
const explainerOpen = ref(false)
const explainerEntry = ref<LeaderboardEntryDto | null>(null)

function openExplainer(entry: LeaderboardEntryDto) {
  explainerEntry.value = entry
  explainerOpen.value = true
}
function closeExplainer() {
  explainerOpen.value = false
}

const explainerLines = computed<string[]>(() => {
  const e = explainerEntry.value
  if (!e) return []
  const who = e.user?.displayName ?? e.user?.username ?? 'This player'
  if (tab.value === 'players') {
    const attempts = Number(e.secondaryValue ?? 0)
    const trueAvg = Number(e.tertiaryValue ?? 0)
    const rating = Number(e.primaryValue ?? 0)
    return [
      `${who} answered correctly ${fmtPercent(trueAvg)} of the time across ${attempts} finished ${attempts === 1 ? 'attempt' : 'attempts'}.`,
      `Their rating is ${rating.toFixed(1)} — a small-sample correction nudges new players toward 50% until they've taken enough attempts for their accuracy to speak for itself.`,
    ]
  }
  const ratings = Number(e.secondaryValue ?? 0)
  const avg = Number(e.primaryValue ?? 0)
  return [
    `${who} averages ${avg.toFixed(1)} / 10 across ${ratings} ${ratings === 1 ? 'rating' : 'ratings'} on their quizzes.`,
  ]
})
</script>

<template>
  <section class="head">
    <div>
      <h1 class="headline-lg">Leaderboards</h1>
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

  <div v-if="you" class="you" role="status">
    <span class="you__rank">#{{ you.rank }}</span>
    <div class="you__body">
      <p class="you__line body-md">
        You are ranked {{ you.rank }} of {{ totalElements }}.
      </p>
      <p class="you__metrics body-md">
        <strong>{{ fmtPrimary(you.primaryValue) }}</strong>
        <span class="you__metric-label muted">{{ primaryLabel }}</span>
        <template v-if="tab === 'players' && you.tertiaryValue != null">
          <span class="you__sep muted">·</span>
          <strong>{{ fmtPercent(you.tertiaryValue) }}</strong>
          <span class="you__metric-label muted">true average</span>
        </template>
        <span class="you__sep muted">·</span>
        <strong>{{ you.secondaryValue }}</strong>
        <span class="you__metric-label muted">{{ secondaryLabel }}</span>
      </p>
    </div>
    <button
      type="button"
      class="info-btn"
      aria-label="Explain how this rank was computed"
      @click="openExplainer(you)"
    >?</button>
  </div>

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
      <span class="row__primary">
        <strong>{{ fmtPrimary(entry.primaryValue) }}</strong>
        <span class="row__label muted">{{ primaryLabel }}</span>
      </span>
      <span v-if="tab === 'players' && entry.tertiaryValue != null" class="row__tertiary" :title="`Raw accuracy average across ${entry.secondaryValue} attempts`">
        <strong>{{ fmtPercent(entry.tertiaryValue) }}</strong>
        <span class="row__label muted">true avg</span>
      </span>
      <span class="row__secondary">
        <strong>{{ entry.secondaryValue }}</strong>
        <span class="row__label muted">{{ secondaryLabel }}</span>
      </span>
      <button
        type="button"
        class="info-btn"
        aria-label="Explain this rank"
        @click="openExplainer(entry)"
      >?</button>
    </li>
  </ol>

  <div v-if="totalPages > 1" class="pager">
    <Button variant="ghost" :disabled="currentPage === 0" @click="currentPage = Math.max(0, currentPage - 1)">Previous</Button>
    <span class="label-sm muted">Page {{ currentPage + 1 }} / {{ totalPages }}</span>
    <Button variant="ghost" :disabled="currentPage + 1 >= totalPages" @click="currentPage = currentPage + 1">Next</Button>
  </div>

  <Modal :open="explainerOpen" title="How this rank was computed" @close="closeExplainer">
    <div class="explainer">
      <p v-for="(line, i) in explainerLines" :key="i" :class="['explainer__line', { 'explainer__line--mono': line.includes('=') || line.includes('rating =') }]">
        {{ line }}
      </p>
    </div>
    <template #footer>
      <Button @click="closeExplainer">Got it</Button>
    </template>
  </Modal>
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
  margin: var(--space-xs) 0 0;
  max-width: 56ch;
}
.muted { color: var(--on-surface-variant); }
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
  gap: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  background: var(--primary-container);
  color: var(--on-primary-container);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-lg);
  position: sticky;
  top: var(--space-md);
  z-index: 1;
  flex-wrap: wrap;
}
.you__rank {
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  font-size: 1.25rem;
}
.you__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.you__line { margin: 0; font-weight: 600; }
.you__metrics {
  margin: 0;
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 4px 6px;
  font-variant-numeric: tabular-nums;
}
.you__metric-label, .you__sep { font-size: 0.85rem; }

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
  grid-template-columns: 56px minmax(0, 1fr) auto auto auto auto;
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
.row__primary, .row__tertiary, .row__secondary {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-end;
  font-variant-numeric: tabular-nums;
  color: var(--on-surface);
  line-height: 1.1;
}
.row__primary strong, .row__tertiary strong, .row__secondary strong {
  font-size: 1.1rem;
  font-weight: 800;
}
.row__primary strong { color: var(--on-surface); }
.row__tertiary strong, .row__secondary strong {
  font-weight: 600;
  color: var(--on-surface-variant);
  font-size: 0.95rem;
}
.row__label {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-top: 1px;
}

.info-btn {
  appearance: none;
  background: transparent;
  border: 1px solid var(--outline-variant);
  color: var(--on-surface-variant);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  font-weight: 700;
  line-height: 1;
  font-size: 0.85rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: color 120ms ease, border-color 120ms ease, background-color 120ms ease;
}
.info-btn:hover {
  color: var(--on-surface);
  border-color: var(--outline);
  background: var(--surface-container-high);
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  margin-top: var(--space-lg);
}

.explainer {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.explainer__line {
  margin: 0;
  color: var(--on-surface);
}
.explainer__line--mono {
  font-family: var(--font-mono, ui-monospace, SFMono-Regular, monospace);
  background: var(--surface-container-lowest);
  padding: 4px 8px;
  border-radius: var(--radius);
  font-size: 0.9rem;
  white-space: pre;
  overflow-x: auto;
}

@media (max-width: 760px) {
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

  /* Two-row layout: rank + user + info on top, metrics on their own row
   * split into three equal columns. The old rule sent all three metrics
   * to the same grid-area with justify-self, which just aligned them
   * within the SAME cell instead of splitting it — they overlapped. */
  .row {
    grid-template-columns: 1fr 1fr 1fr;
    grid-template-rows: auto auto;
    row-gap: var(--space-sm);
    column-gap: var(--space-sm);
    padding: var(--space-sm) var(--space-md);
    align-items: center;
  }
  .row__rank {
    grid-row: 1;
    grid-column: 1;
    justify-self: start;
  }
  .row__user {
    grid-row: 1;
    grid-column: 2 / span 2;
    min-width: 0;
  }
  .info-btn {
    grid-row: 1;
    grid-column: 3;
    justify-self: end;
    width: 32px;
    height: 32px;
  }
  /* When there's no tertiary metric the user cell can stretch further —
   * :has() lets us reclaim the width when the third slot is unused. */
  .row__primary,
  .row__tertiary,
  .row__secondary {
    grid-row: 2;
    flex-direction: row;
    align-items: baseline;
    gap: 4px;
    min-width: 0;
    font-size: 0.9rem;
  }
  .row__primary strong,
  .row__tertiary strong,
  .row__secondary strong {
    font-size: 1rem;
  }
  .row__label { margin-top: 0; }
  .row__primary {
    grid-column: 1;
    justify-self: start;
  }
  .row__tertiary {
    grid-column: 2;
    justify-self: center;
  }
  .row__secondary {
    grid-column: 3;
    justify-self: end;
  }
  /* Authors board has no tertiary — let secondary occupy the middle so
   * primary/secondary sit at opposite ends of the row instead of both
   * hugging the left. */
  .row:not(:has(.row__tertiary)) .row__secondary {
    grid-column: 2 / span 2;
    justify-self: end;
  }

  .you {
    position: static;
    align-items: flex-start;
    padding: var(--space-sm) var(--space-md);
  }
  .you__metrics {
    font-size: 0.9rem;
  }
}

@media (max-width: 420px) {
  /* Very narrow: stack metrics vertically so long labels don't clip. */
  .row {
    grid-template-columns: auto 1fr auto;
  }
  .row__user { grid-column: 2 / span 2; }
  .info-btn { grid-column: 3; }
  .row__primary,
  .row__tertiary,
  .row__secondary {
    grid-column: 1 / -1;
    justify-self: stretch;
    justify-content: space-between;
    width: 100%;
  }
  .row__primary   { grid-row: 2; }
  .row__tertiary  { grid-row: 3; }
  .row__secondary { grid-row: 4; }
  .row:not(:has(.row__tertiary)) .row__secondary { grid-row: 3; }
}
</style>
