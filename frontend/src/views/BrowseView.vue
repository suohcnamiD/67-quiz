<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGetQuizzes } from '@/api/quiz-controller/quiz-controller'
import {
  useGetAttemptsInProgress,
  useGetFinishedAttempts,
} from '@/api/attempt-controller/attempt-controller'
import { useSearch } from '@/api/search-controller/search-controller'
import { useDebouncedRef } from '@/lib/useDebouncedRef'
import Card from '@/components/Card.vue'
import Chip from '@/components/Chip.vue'
import Button from '@/components/Button.vue'
import QuizCard from '@/components/QuizCard.vue'
import UserCard from '@/components/UserCard.vue'

const router = useRouter()
const route = useRoute()
const errorText = ref<string | null>(null)

const page = ref(0)
const quizzes = useGetQuizzes(computed(() => ({ page: page.value })))
const inProgress = useGetAttemptsInProgress({ page: 0 })
const finished = useGetFinishedAttempts({ page: 0 })

const items = computed(() => quizzes.data.value?._embedded?.quizzes ?? [])
const inProgressItems = computed(() => inProgress.data.value?._embedded?.attempts ?? [])
const finishedItems = computed(() => finished.data.value?._embedded?.attempts ?? [])
const totalPages = computed(() => quizzes.data.value?.page?.totalPages ?? 1)

// Hash-anchor scroll: when we arrive at /app#past-results (e.g. from the
// profile's "Attempts taken" stat) the past-results section may not be in the
// DOM yet — it's behind v-if and the finished-attempts query is still in
// flight. Watch both signals and scroll once the target exists.
function scrollToHashTarget() {
  const id = route.hash.replace(/^#/, '')
  if (!id) return
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
onMounted(() => {
  void nextTick(scrollToHashTarget)
})
watch(
  [() => route.hash, finishedItems],
  () => void nextTick(scrollToHashTarget),
)

// Search — debounced so we don't fire a request per keystroke. The backend
// already short-circuits anything under 2 chars to empty results.
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
const noMatches = computed(
  () =>
    isSearching.value &&
    !searchResults.isLoading.value &&
    !matchedQuizzes.value.length &&
    !matchedUsers.value.length,
)

function clearSearch() {
  rawQuery.value = ''
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

  <section class="search" aria-label="Search">
    <label class="search__field">
      <span class="visually-hidden">Search quizzes and people</span>
      <input
        v-model="rawQuery"
        type="search"
        class="search__input"
        placeholder="Search quizzes and people…"
        autocomplete="off"
        spellcheck="false"
        aria-label="Search quizzes and people"
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

  <!-- Search mode hides the default Browse/Continue/Past sections. -->
  <template v-if="isSearching">
    <p class="search-status label-sm">
      <span>Searching: <strong>"{{ debouncedQuery }}"</strong></span>
      <button type="button" class="search-status__clear" @click="clearSearch">Clear</button>
    </p>
    <section class="section" aria-labelledby="search-quizzes-heading">
      <header class="section__head">
        <h2 id="search-quizzes-heading" class="headline-md">
          Quizzes <span v-if="matchedQuizzes.length" class="muted label-md">({{ matchedQuizzes.length }})</span>
        </h2>
      </header>
      <p v-if="searchResults.isLoading.value" class="empty body-md">Searching…</p>
      <p v-else-if="!matchedQuizzes.length" class="empty body-md">No matching quizzes.</p>
      <div v-else class="grid">
        <QuizCard
          v-for="q in matchedQuizzes"
          :key="q.id"
          :quiz="q"
          @error="errorText = $event"
        />
      </div>
    </section>
    <section class="section" aria-labelledby="search-users-heading">
      <header class="section__head">
        <h2 id="search-users-heading" class="headline-md">
          People <span v-if="matchedUsers.length" class="muted label-md">({{ matchedUsers.length }})</span>
        </h2>
      </header>
      <p v-if="searchResults.isLoading.value" class="empty body-md">Searching…</p>
      <p v-else-if="!matchedUsers.length" class="empty body-md">No matching people.</p>
      <ul v-else class="people-list">
        <li v-for="u in matchedUsers" :key="u.username">
          <UserCard :user="u" />
        </li>
      </ul>
    </section>
    <p v-if="noMatches" class="empty body-md">
      Nothing matches "{{ debouncedQuery }}". Try a different search.
    </p>
  </template>

  <template v-else>
    <section class="section" aria-labelledby="continue-heading">
      <header class="section__head">
        <h2 id="continue-heading" class="headline-md">Continue</h2>
      </header>
      <div v-if="inProgressItems.length" class="grid">
        <Card v-for="a in inProgressItems" :key="a.id" interactive @click="router.push(`/app/attempt/${a.id}`)">
          <div class="row">
            <h3 class="headline-md">{{ a.quiz?.name ?? 'Untitled quiz' }}</h3>
            <Chip>In progress</Chip>
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
      <p v-else class="empty body-md">No active attempts.</p>
    </section>

    <section class="section" aria-labelledby="browse-heading">
      <header class="section__head">
        <h2 id="browse-heading" class="headline-md">Browse quizzes</h2>
      </header>
      <p v-if="quizzes.isLoading.value" class="empty body-md">Loading…</p>
      <div v-else-if="!items.length" class="empty-state">
        <p class="empty body-md">No quizzes yet — be the first to author one.</p>
        <Button @click="router.push('/app/quiz/new')">New quiz</Button>
      </div>
      <div v-else class="grid">
        <QuizCard v-for="q in items" :key="q.id" :quiz="q" @error="errorText = $event" />
      </div>
      <div v-if="totalPages > 1" class="pager">
        <Button variant="ghost" :disabled="page === 0" @click="page = Math.max(0, page - 1)">Previous</Button>
        <span class="label-sm muted">Page {{ page + 1 }} / {{ totalPages }}</span>
        <Button variant="ghost" :disabled="page + 1 >= totalPages" @click="page = page + 1">Next</Button>
      </div>
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
</template>

<style scoped>
.banner {
  margin: 0 0 var(--space-lg);
  padding: var(--space-sm) var(--space-md);
  background: var(--error-container);
  color: var(--on-error-container);
  border-radius: var(--radius);
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
  gap: var(--space-md);
  margin-top: var(--space-lg);
}
.muted {
  color: var(--on-surface-variant);
}
</style>
