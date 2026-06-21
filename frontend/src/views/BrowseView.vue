<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useGetQuizzes, _delete as deleteQuiz, getGetQuizzesQueryKey } from '@/api/quiz-controller/quiz-controller'
import { useGetAttemptsInProgress, useGetFinishedAttempts, attemptQuiz, getGetAttemptsInProgressQueryKey } from '@/api/attempt-controller/attempt-controller'
import { useQueryClient } from '@tanstack/vue-query'
import { errorMessage } from '@/lib/errors'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import Chip from '@/components/Chip.vue'

const router = useRouter()
const qc = useQueryClient()
const errorText = ref<string | null>(null)

const page = ref(0)

const quizzes = useGetQuizzes(computed(() => ({ page: page.value })))
const inProgress = useGetAttemptsInProgress({ page: 0 })
const finished = useGetFinishedAttempts({ page: 0 })

const items = computed(() => quizzes.data.value?._embedded?.quizzes ?? [])
const inProgressItems = computed(() => inProgress.data.value?._embedded?.attempts ?? [])
const finishedItems = computed(() => finished.data.value?._embedded?.attempts ?? [])
const totalPages = computed(() => quizzes.data.value?.page?.totalPages ?? 1)

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

const startingId = ref<string | null>(null)
async function startAttempt(quizId?: string) {
  if (!quizId) return
  startingId.value = quizId
  errorText.value = null
  try {
    const attempt = await attemptQuiz({ quizId })
    qc.invalidateQueries({ queryKey: getGetAttemptsInProgressQueryKey() })
    if (attempt.id) router.push(`/app/attempt/${attempt.id}`)
  } catch (e) {
    errorText.value = errorMessage(e)
  } finally {
    startingId.value = null
  }
}

const deletingId = ref<string | null>(null)
async function removeQuiz(id?: string) {
  if (!id) return
  if (!confirm('Delete this quiz? This cannot be undone.')) return
  deletingId.value = id
  errorText.value = null
  try {
    await deleteQuiz(id)
    qc.invalidateQueries({ queryKey: getGetQuizzesQueryKey() })
  } catch (e) {
    errorText.value = errorMessage(e)
  } finally {
    deletingId.value = null
  }
}
</script>

<template>
  <p v-if="errorText" class="banner label-md">{{ errorText }}</p>
  <section class="section">
    <header class="section__head">
      <h2 class="headline-md">Continue</h2>
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

  <section class="section">
    <header class="section__head">
      <h2 class="headline-md">Browse quizzes</h2>
    </header>
    <p v-if="quizzes.isLoading.value" class="empty body-md">Loading…</p>
    <p v-else-if="!items.length" class="empty body-md">No quizzes yet. Create one.</p>
    <div v-else class="grid">
      <Card v-for="q in items" :key="q.id">
        <div class="row">
          <h3 class="headline-md">{{ q.name }}</h3>
          <Chip v-if="q.youAreAuthor">Your quiz</Chip>
        </div>
        <div class="meta-row label-sm">
          <span>{{ q.questionCount ?? 0 }} questions</span>
          <span>·</span>
          <span>{{ fmtDuration(q.duration) }}</span>
          <span v-if="q.maximumScore != null">·</span>
          <span v-if="q.maximumScore != null">Max {{ q.maximumScore }} pts</span>
        </div>
        <div class="actions">
          <Button @click="startAttempt(q.id)" :loading="startingId === q.id">Start attempt</Button>
          <Button v-if="q.youAreAuthor" variant="ghost" @click="router.push(`/app/quiz/${q.id}`)">Edit</Button>
          <Button v-if="q.youAreAuthor" variant="danger" @click="removeQuiz(q.id)">Delete</Button>
        </div>
      </Card>
    </div>
    <div v-if="totalPages > 1" class="pager">
      <Button variant="ghost" :disabled="page === 0" @click="page = Math.max(0, page - 1)">Previous</Button>
      <span class="label-sm muted">Page {{ page + 1 }} / {{ totalPages }}</span>
      <Button variant="ghost" :disabled="page + 1 >= totalPages" @click="page = page + 1">Next</Button>
    </div>
  </section>

  <section v-if="finishedItems.length" class="section">
    <header class="section__head">
      <h2 class="headline-md">Past results</h2>
    </header>
    <div class="grid">
      <Card v-for="a in finishedItems" :key="a.id" interactive @click="router.push(`/app/attempt/${a.id}/result`)">
        <div class="row">
          <h3 class="headline-md">{{ a.quiz?.name ?? 'Untitled quiz' }}</h3>
          <Chip tone="success">Finished</Chip>
        </div>
        <div class="row">
          <span class="headline-md">{{ a.score ?? 0 }} <span class="muted">/ {{ a.maximumScore ?? 0 }}</span></span>
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
.actions {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
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
