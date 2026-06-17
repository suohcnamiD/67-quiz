<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useGetQuizzes, _delete as deleteQuiz } from '@/api/quiz-controller/quiz-controller'
import { useGetAttemptsInProgress, useGetFinishedAttempts, attemptQuiz } from '@/api/attempt-controller/attempt-controller'
import { useQueryClient } from '@tanstack/vue-query'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import Chip from '@/components/Chip.vue'

const router = useRouter()
const qc = useQueryClient()

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

const startingId = ref<string | null>(null)
async function startAttempt(quizId?: string) {
  if (!quizId) return
  startingId.value = quizId
  try {
    const attempt = await attemptQuiz({ quizId })
    qc.invalidateQueries({ queryKey: ['attempt', 'in-progress'] })
    if (attempt.id) router.push(`/app/attempt/${attempt.id}`)
  } finally {
    startingId.value = null
  }
}

const deletingId = ref<string | null>(null)
async function removeQuiz(id?: string) {
  if (!id) return
  if (!confirm('Delete this quiz? This cannot be undone.')) return
  deletingId.value = id
  try {
    await deleteQuiz(id)
    qc.invalidateQueries({ queryKey: ['quiz'] })
  } finally {
    deletingId.value = null
  }
}
</script>

<template>
  <section class="section">
    <header class="section__head">
      <h2 class="headline-md">Continue</h2>
    </header>
    <div v-if="inProgressItems.length" class="grid">
      <Card v-for="a in inProgressItems" :key="a.id" interactive @click="router.push(`/app/attempt/${a.id}`)">
        <div class="row">
          <Chip>In progress</Chip>
          <span class="label-sm muted">{{ a.questions?.length ?? 0 }} questions</span>
        </div>
        <p class="meta body-md">Resume your run.</p>
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
          <span class="headline-md">{{ a.score ?? 0 }} / {{ a.maximumScore ?? 0 }}</span>
          <Chip tone="success">Finished</Chip>
        </div>
        <p class="meta body-md">{{ a.questions?.length ?? 0 }} questions</p>
      </Card>
    </div>
  </section>
</template>

<style scoped>
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
