<script setup lang="ts">
import { ref, computed, useTemplateRef, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGetQuizzes } from '@/api/quiz-controller/quiz-controller'
import {
  useGetAttemptsInProgress,
  useGetFinishedAttempts,
} from '@/api/attempt-controller/attempt-controller'
import { useGetOwnProfile } from '@/api/user-profile-controller/user-profile-controller'
import { useSearch } from '@/api/search-controller/search-controller'
import { useTopPlayers, useTopAuthors } from '@/api/leaderboard-controller/leaderboard-controller'
import { useAuthStore } from '@/stores/auth'
import { useDebouncedRef } from '@/lib/useDebouncedRef'
import Card from '@/components/Card.vue'
import Chip from '@/components/Chip.vue'
import Button from '@/components/Button.vue'
import QuizCard from '@/components/QuizCard.vue'
import UserCard from '@/components/UserCard.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const errorText = ref<string | null>(null)

const page = ref(0)
const quizzes = useGetQuizzes(computed(() => ({ page: page.value })))
const inProgress = useGetAttemptsInProgress({ page: 0 })
const finished = useGetFinishedAttempts({ page: 0 })
const profile = useGetOwnProfile()

const me = computed(() => profile.data.value)
const greetingName = computed(
  () => me.value?.displayName ?? me.value?.username ?? auth.displayName ?? auth.username ?? 'there',
)

// Leaderboard rank — only renders when the user qualifies on either board.
// Asking for page=0 of each is enough: the `you` field is computed across
// the full qualifying population, not just the page.
const players = useTopPlayers({ page: 0 })
const authors = useTopAuthors({ page: 0 })
const playerRank = computed(() => players.data.value?.you ?? null)
const authorRank = computed(() => authors.data.value?.you ?? null)

const items = computed(() => quizzes.data.value?._embedded?.quizzes ?? [])
const inProgressItems = computed(() => inProgress.data.value?._embedded?.attempts ?? [])
const finishedItems = computed(() => finished.data.value?._embedded?.attempts ?? [])
const totalPages = computed(() => quizzes.data.value?.page?.totalPages ?? 1)
const totalQuizzes = computed(() => quizzes.data.value?.page?.totalElements ?? items.value.length)

// Numeric pager: compact list of page indices with leading/trailing ellipses
// once totalPages exceeds 7. Mobile collapses further via CSS.
const pageNumbers = computed<(number | 'ellipsis')[]>(() => {
  const total = totalPages.value
  const current = page.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i)
  const pages: (number | 'ellipsis')[] = [0]
  const start = Math.max(1, current - 1)
  const end = Math.min(total - 2, current + 1)
  if (start > 1) pages.push('ellipsis')
  for (let i = start; i <= end; i++) pages.push(i)
  if (end < total - 2) pages.push('ellipsis')
  pages.push(total - 1)
  return pages
})

// Hash-anchor scroll: when we arrive at /app#past-results the section may
// still be loading. Wait for finished-attempts data, scroll once, then stop.
const hasScrolled = ref(false)
watchEffect(() => {
  if (hasScrolled.value) return
  if (route.hash !== '#past-results') return
  if (finished.isLoading.value) return
  if (!finishedItems.value.length) {
    hasScrolled.value = true
    return
  }
  const el = document.getElementById('past-results')
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  hasScrolled.value = true
})

// Search — debounced so we don't fire a request per keystroke. The backend
// already short-circuits anything under 2 chars to empty results.
const searchInput = useTemplateRef<HTMLInputElement>('searchInput')
const rawQuery = ref('')
const debouncedQuery = useDebouncedRef(rawQuery, 250)
const isSearching = computed(() => rawQuery.value.trim().length >= 2)
const searchParams = computed(() => ({ q: debouncedQuery.value }))
const searchEnabled = computed(() => debouncedQuery.value.trim().length >= 2)
const searchResults = useSearch(searchParams, {
  query: { enabled: searchEnabled },
})
const matchedQuizzes = computed(() => searchResults.data.value?.quizzes ?? [])
const matchedUsers = computed(() => searchResults.data.value?.users ?? [])
const totalMatches = computed(() => matchedQuizzes.value.length + matchedUsers.value.length)
const noMatches = computed(
  () =>
    isSearching.value &&
    !searchResults.isLoading.value &&
    !matchedQuizzes.value.length &&
    !matchedUsers.value.length,
)

function clearSearch() {
  rawQuery.value = ''
  searchInput.value?.focus()
}

function onSearchKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && rawQuery.value) {
    e.preventDefault()
    clearSearch()
  }
}

function isStale(iso?: string): boolean {
  if (!iso) return false
  const then = new Date(iso).getTime()
  if (Number.isNaN(then)) return false
  return Date.now() - then > 60 * 60 * 1000
}

function fmtDuration(iso?: string): string {
  if (!iso) return '—'
  const m = iso.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/)
  if (!m) return iso
  const [, h, mins, s] = m
  const parts: string[] = []
  if (h) parts.push(`${h}h`)
  if (mins) parts.push(`${mins}m`)
  if (s) parts.push(`${s}s`)
  return parts.join(' ') || '0s'
}

function fmtRelative(iso?: string): string {
  if (!iso) return ''
  const then = new Date(iso).getTime()
  if (Number.isNaN(then)) return ''
  const diffSec = Math.round((Date.now() - then) / 1000)
  const abs = Math.abs(diffSec)
  if (abs < 45) return 'just now'
  if (abs < 90) return diffSec >= 0 ? '1 minute ago' : 'in 1 minute'
  const diffMin = Math.round(diffSec / 60)
  if (Math.abs(diffMin) < 60) return diffSec >= 0 ? `${diffMin} minutes ago` : `in ${Math.abs(diffMin)} minutes`
  const diffHr = Math.round(diffSec / 3600)
  if (Math.abs(diffHr) < 24) return diffSec >= 0 ? `${diffHr} hours ago` : `in ${Math.abs(diffHr)} hours`
  const diffDay = Math.round(diffSec / 86400)
  if (Math.abs(diffDay) < 30) return diffSec >= 0 ? `${diffDay} days ago` : `in ${Math.abs(diffDay)} days`
  return new Date(iso).toLocaleDateString()
}
</script>

<template>
  <p v-if="errorText" class="banner label-md" role="alert">{{ errorText }}</p>

  <section class="hero" aria-label="Your dashboard">
    <div class="hero__head">
      <span class="label-sm muted">Signed in</span>
      <h1 class="hero__title">Welcome back, {{ greetingName }}.</h1>
    </div>
    <ul class="stats">
      <li class="stat">
        <span class="stat__label label-sm muted">Quizzes authored</span>
        <span class="stat__value">{{ me?.quizzesAuthored ?? 0 }}</span>
      </li>
      <li class="stat">
        <span class="stat__label label-sm muted">Attempts taken</span>
        <span class="stat__value">{{ me?.attemptsTaken ?? 0 }}</span>
      </li>
      <li class="stat">
        <span class="stat__label label-sm muted">Average score</span>
        <span class="stat__value">
          {{ me?.averageScorePercent != null ? `${me.averageScorePercent}%` : '—' }}
        </span>
      </li>
    </ul>
    <div v-if="playerRank || authorRank" class="rank-row" aria-label="Your leaderboard standings">
      <RouterLink v-if="playerRank" to="/app/leaderboards" class="rank-pill">
        <span class="rank-pill__hash">#{{ playerRank.rank }}</span>
        <span class="rank-pill__label body-md">Top players</span>
        <span class="rank-pill__chevron" aria-hidden="true">›</span>
      </RouterLink>
      <RouterLink v-if="authorRank" to="/app/leaderboards" class="rank-pill">
        <span class="rank-pill__hash">#{{ authorRank.rank }}</span>
        <span class="rank-pill__label body-md">Top authors</span>
        <span class="rank-pill__chevron" aria-hidden="true">›</span>
      </RouterLink>
    </div>
  </section>

  <section class="search" aria-label="Search">
    <label class="search__field">
      <span class="visually-hidden">Search quizzes and people</span>
      <input
        ref="searchInput"
        v-model="rawQuery"
        type="search"
        class="search__input"
        placeholder="Search quizzes and people…"
        autocomplete="off"
        spellcheck="false"
        aria-label="Search quizzes and people"
        @keydown="onSearchKeydown"
      />
      <button
        v-if="rawQuery"
        type="button"
        class="search__clear"
        aria-label="Clear search"
        @click="clearSearch"
      >✕</button>
    </label>
  </section>

  <!-- Search overlay: sits above the dashboard while searching, dashboard
       sections below remain mounted so clearing search is instant. -->
  <section
    v-if="isSearching"
    class="search-results"
    role="region"
    aria-label="Search results"
  >
    <p class="search-status label-sm">
      <span>
        Searching: <strong>"{{ debouncedQuery }}"</strong>
        <span v-if="!searchResults.isLoading.value" aria-live="polite" class="muted">
          · {{ totalMatches }} {{ totalMatches === 1 ? 'match' : 'matches' }}
        </span>
      </span>
      <button type="button" class="search-status__clear" @click="clearSearch">Clear</button>
    </p>

    <template v-if="searchResults.isLoading.value">
      <div class="grid" aria-hidden="true">
        <div v-for="i in 2" :key="i" class="skeleton skeleton--card" />
      </div>
    </template>
    <template v-else>
      <section v-if="matchedQuizzes.length" class="section" aria-labelledby="search-quizzes-heading">
        <header class="section__head">
          <h2 id="search-quizzes-heading" class="headline-md">
            Quizzes <span class="muted label-md">({{ matchedQuizzes.length }})</span>
          </h2>
        </header>
        <div class="grid">
          <QuizCard
            v-for="q in matchedQuizzes"
            :key="q.id"
            :quiz="q"
            @error="errorText = $event"
          />
        </div>
      </section>
      <section v-if="matchedUsers.length" class="section" aria-labelledby="search-users-heading">
        <header class="section__head">
          <h2 id="search-users-heading" class="headline-md">
            People <span class="muted label-md">({{ matchedUsers.length }})</span>
          </h2>
        </header>
        <ul class="people-list">
          <li v-for="u in matchedUsers" :key="u.username">
            <UserCard :user="u" />
          </li>
        </ul>
      </section>
      <p v-if="noMatches" class="empty body-md">
        Nothing matches "{{ debouncedQuery }}". Try a different search.
      </p>
    </template>
  </section>

  <section
    v-if="inProgressItems.length"
    class="section section--accent"
    aria-labelledby="continue-heading"
  >
    <header class="section__head">
      <h2 id="continue-heading" class="headline-md">Continue</h2>
    </header>
    <div class="grid">
      <Card
        v-for="a in inProgressItems"
        :key="a.id"
        interactive
        class="resume-card"
        @click="router.push(`/app/attempt/${a.id}`)"
      >
        <div class="row">
          <h3 class="headline-md">{{ a.quiz?.name ?? 'Untitled quiz' }}</h3>
          <Chip :tone="isStale(a.startedAt) ? 'warning' : undefined">Resume</Chip>
        </div>
        <div class="meta-row label-sm">
          <span>{{ a.questions?.length ?? 0 }} questions</span>
          <span v-if="a.quiz?.duration">·</span>
          <span v-if="a.quiz?.duration">{{ fmtDuration(a.quiz.duration) }}</span>
          <span v-if="a.startedAt">·</span>
          <span v-if="a.startedAt">Started {{ fmtRelative(a.startedAt) }}</span>
        </div>
      </Card>
    </div>
  </section>

  <section class="section" aria-labelledby="browse-heading">
    <header class="section__head">
      <h2 id="browse-heading" class="headline-md">
        Browse quizzes
        <span v-if="totalQuizzes" class="muted label-md">({{ totalQuizzes }})</span>
      </h2>
    </header>
    <div v-if="quizzes.isLoading.value" class="grid" aria-hidden="true">
      <div v-for="i in 3" :key="i" class="skeleton skeleton--card" />
    </div>
    <div v-else-if="!items.length" class="empty-state">
      <p class="empty body-md">No quizzes yet — be the first to author one.</p>
      <Button @click="router.push('/app/quiz/new')">New quiz</Button>
    </div>
    <div v-else class="grid">
      <QuizCard v-for="q in items" :key="q.id" :quiz="q" @error="errorText = $event" />
    </div>
    <nav v-if="totalPages > 1" class="pager" aria-label="Browse pagination">
      <button
        type="button"
        class="pager__btn pager__btn--nav"
        :disabled="page === 0"
        aria-label="Previous page"
        @click="page = Math.max(0, page - 1)"
      >‹</button>
      <ul class="pager__list">
        <li v-for="(p, i) in pageNumbers" :key="`${p}-${i}`">
          <span v-if="p === 'ellipsis'" class="pager__ellipsis" aria-hidden="true">…</span>
          <button
            v-else
            type="button"
            class="pager__btn pager__btn--num"
            :class="{ 'pager__btn--active': p === page }"
            :aria-current="p === page ? 'page' : undefined"
            :aria-label="`Page ${p + 1}`"
            @click="page = p"
          >{{ p + 1 }}</button>
        </li>
      </ul>
      <button
        type="button"
        class="pager__btn pager__btn--nav"
        :disabled="page + 1 >= totalPages"
        aria-label="Next page"
        @click="page = page + 1"
      >›</button>
    </nav>
  </section>

  <section v-if="finishedItems.length" id="past-results" class="section" aria-labelledby="past-heading">
    <header class="section__head">
      <h2 id="past-heading" class="headline-md">Past results</h2>
    </header>
    <div class="grid">
      <Card v-for="a in finishedItems" :key="a.id" interactive @click="router.push(`/app/attempt/${a.id}/result`)">
        <div class="row">
          <h3 class="headline-md">{{ a.quiz?.name ?? 'Untitled quiz' }}</h3>
          <Chip tone="success">Finished</Chip>
        </div>
        <div class="row">
          <div class="score-stack">
            <span class="label-sm muted">Score</span>
            <span class="headline-md">{{ a.score ?? 0 }} <span class="muted">/ {{ a.maximumScore ?? 0 }}</span></span>
          </div>
          <span class="label-sm muted">{{ a.questions?.length ?? 0 }} questions</span>
        </div>
        <p v-if="a.startedAt" class="meta body-md">Attempted {{ fmtRelative(a.startedAt) }}</p>
      </Card>
    </div>
  </section>
</template>

<style scoped>
.banner {
  margin: 0 0 var(--space-lg);
  padding: var(--space-sm) var(--space-md);
  background: var(--error-container);
  color: var(--on-error-container);
  border-radius: var(--radius);
}
.hero {
  margin: 0 0 var(--space-xl);
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
.hero__head {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.hero__title {
  margin: 0;
  font-size: clamp(1.5rem, 4vw, 2.25rem);
  font-weight: 800;
  letter-spacing: -0.01em;
  color: var(--on-surface);
}
.stats {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-md);
}
.stat {
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
  padding: var(--space-md) var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.stat__value {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--on-surface);
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
}
@media (max-width: 640px) {
  .stats {
    grid-template-columns: 1fr;
  }
}
.rank-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  margin-top: var(--space-sm);
}
.rank-pill {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 6px 10px 6px 12px;
  border-radius: 999px;
  background: var(--primary-container);
  color: var(--on-primary-container);
  text-decoration: none;
  font-weight: 600;
  transition: transform 120ms ease, background-color 120ms ease;
}
.rank-pill:hover {
  background: color-mix(in srgb, var(--primary-container) 88%, white);
}
.rank-pill__hash {
  font-variant-numeric: tabular-nums;
  font-weight: 800;
}
.rank-pill__chevron {
  font-size: 1.1rem;
  line-height: 1;
  opacity: 0.7;
  transition: transform 120ms ease;
}
.rank-pill:hover .rank-pill__chevron {
  transform: translateX(2px);
  opacity: 1;
}
.search {
  margin-bottom: var(--space-lg);
}
.search-status {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin: 0 0 var(--space-md);
  color: var(--on-surface-variant);
}
.search-status__clear {
  appearance: none;
  background: transparent;
  border: 0;
  color: var(--on-surface);
  font: inherit;
  text-decoration: underline;
  cursor: pointer;
  padding: 0;
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-sm);
}
.search__field {
  position: relative;
  display: block;
}
.search__input {
  width: 100%;
  padding: 12px 44px 12px 16px;
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
  color: var(--on-surface);
  font-size: 1rem;
  outline: none;
  transition: border-color 120ms ease;
}
.search__input:focus {
  border-color: var(--primary-container);
}
.search__input::-webkit-search-cancel-button {
  /* Native clear button looks awful on dark — we provide our own. */
  display: none;
}
.search__clear {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 0;
  background: var(--surface-container-high);
  color: var(--on-surface-variant);
  cursor: pointer;
  font-size: 0.85rem;
  line-height: 1;
}
.search__clear:hover {
  color: var(--on-surface);
}
.visually-hidden {
  position: absolute !important;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
.section {
  margin-bottom: var(--space-xl);
}
.section__head {
  margin-bottom: var(--space-md);
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-md);
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
}
.meta {
  color: var(--on-surface-variant);
  margin: 0 0 var(--space-md);
}
.meta-row {
  display: flex;
  gap: var(--space-xs);
  color: var(--on-surface-variant);
  margin-bottom: var(--space-md);
}
.score-stack {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.people-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.empty {
  color: var(--on-surface-variant);
}
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  margin-top: var(--space-lg);
  flex-wrap: wrap;
}
.pager__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  align-items: center;
  gap: 4px;
}
.pager__btn {
  appearance: none;
  min-width: 44px;
  height: 44px;
  padding: 0 var(--space-sm);
  border-radius: var(--radius);
  border: 1px solid var(--outline-variant);
  background: var(--surface-container);
  color: var(--on-surface);
  font: inherit;
  font-variant-numeric: tabular-nums;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background-color 120ms ease, border-color 120ms ease, color 120ms ease;
}
.pager__btn:hover:not(:disabled) {
  background: var(--surface-container-high);
}
.pager__btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.pager__btn--nav {
  font-size: 1.25rem;
  line-height: 1;
}
.pager__btn--active {
  background: var(--primary-container);
  color: var(--on-primary-container);
  border-color: var(--primary-container);
  cursor: default;
}
.pager__ellipsis {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 44px;
  color: var(--on-surface-variant);
}
.muted {
  color: var(--on-surface-variant);
}

/* Search-overlay container — sits above dashboard sections while searching. */
.search-results {
  margin-bottom: var(--space-xl);
  padding-block-end: var(--space-xl);
  border-bottom: 1px solid var(--outline-variant);
}

/* Accent treatment for the Continue section. */
.section--accent {
  position: relative;
  padding-left: var(--space-md);
  border-left: 3px solid var(--primary-container);
}
.section--accent .section__head h2 {
  font-size: 1.375rem;
}
.resume-card {
  background: var(--surface-container-low);
}

/* Inline skeleton loaders — match Card footprint so layout doesn't jump. */
.skeleton {
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
  position: relative;
  overflow: hidden;
  animation: skeleton-pulse 1400ms ease-in-out infinite;
}
.skeleton--card {
  min-height: 132px;
}
@keyframes skeleton-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.55; }
}
@media (prefers-reduced-motion: reduce) {
  .skeleton {
    animation: none;
    opacity: 0.7;
  }
}

@media (max-width: 640px) {
  .section--accent {
    padding-left: var(--space-sm);
  }
  .pager {
    gap: var(--space-xs);
  }
  .pager__btn {
    min-width: 40px;
    height: 40px;
  }
}
</style>
