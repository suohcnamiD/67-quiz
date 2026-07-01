<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'
import { useGetQuizOverview, deleteQuiz, getGetQuizzesQueryKey } from '@/api/quiz-controller/quiz-controller'
import { attemptQuiz, getGetAttemptsInProgressQueryKey } from '@/api/attempt-controller/attempt-controller'
import { useListRatings } from '@/api/quiz-rating-controller/quiz-rating-controller'
import { errorMessage } from '@/lib/errors'
import { confirmDialog } from '@/lib/confirmDialog'
import { coverUrl } from '@/lib/quizImages'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import Chip from '@/components/Chip.vue'
import Avatar from '@/components/Avatar.vue'
import MarkdownView from '@/components/MarkdownView.vue'

defineOptions({ name: 'QuizOverviewView' })

const route = useRoute()
const router = useRouter()
const qc = useQueryClient()

const quizId = computed(() => route.params.quizId as string)
const { data, isPending } = useGetQuizOverview(quizId)
const quiz = computed(() => data.value)

const ratingsQuery = useListRatings(quizId, { page: 0 })
const ratings = computed(() => ratingsQuery.data.value?._embedded?.ratings ?? [])
const totalRatings = computed(() => ratingsQuery.data.value?.page?.totalElements ?? 0)

const errorText = ref<string | null>(null)
const starting = ref(false)
const deleting = ref(false)

const canStart = computed(() => (quiz.value?.questionCount ?? 0) > 0)

async function startAttempt() {
  if (!quizId.value || !canStart.value) return
  starting.value = true
  errorText.value = null
  try {
    const attempt = await attemptQuiz({ quizId: quizId.value })
    qc.invalidateQueries({ queryKey: getGetAttemptsInProgressQueryKey() })
    if (attempt.id) router.push(`/app/attempt/${attempt.id}`)
  } catch (e) {
    errorText.value = errorMessage(e)
  } finally {
    starting.value = false
  }
}

async function removeQuiz() {
  if (!quizId.value) return
  const ok = await confirmDialog.open({
    title: 'Delete this quiz?',
    body: 'This cannot be undone.',
    confirmLabel: 'Delete',
    danger: true,
  })
  if (!ok) return
  deleting.value = true
  try {
    await deleteQuiz(quizId.value)
    qc.invalidateQueries({ queryKey: getGetQuizzesQueryKey() })
    router.push('/app')
  } catch (e) {
    errorText.value = errorMessage(e)
  } finally {
    deleting.value = false
  }
}

function fmtRating(avg: number | null | undefined): string {
  if (avg == null) return '—'
  return avg.toFixed(1).replace(/\.0$/, '')
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
  if (Math.abs(diffSec) < 45) return 'just now'
  const diffMin = Math.round(diffSec / 60)
  if (Math.abs(diffMin) < 60) return `${diffMin} min ago`
  const diffHr = Math.round(diffSec / 3600)
  if (Math.abs(diffHr) < 24) return `${diffHr} h ago`
  const diffDay = Math.round(diffSec / 86400)
  if (Math.abs(diffDay) < 30) return `${diffDay} d ago`
  return new Date(iso).toLocaleDateString()
}
</script>

<template>
  <div v-if="isPending" class="empty body-md">Loading…</div>
  <Card v-else-if="!quiz" class="notfound">
    <h1 class="headline-md">Quiz not found</h1>
    <p class="body-md muted">This quiz doesn't exist or you don't have access to it.</p>
    <Button @click="router.push('/app')">Back to browse</Button>
  </Card>
  <template v-else>
    <img
      v-if="quiz.hasCover && quiz.id"
      :src="coverUrl(quiz.id)"
      alt=""
      class="cover"
      loading="lazy"
    />

    <header class="head">
      <div class="head__main">
        <div class="head__title-row">
          <h1 class="head__title">{{ quiz.name }}</h1>
          <Chip v-if="quiz.youAreAuthor" class="head__chip">Your quiz</Chip>
        </div>
        <RouterLink
          v-if="quiz.author?.username"
          :to="{ name: 'user-profile', params: { username: quiz.author.username } }"
          class="author"
        >
          <Avatar
            :username="quiz.author.username"
            :display-name="quiz.author.displayName"
            :initials-only="!quiz.author.hasAvatar"
            :size="24"
          />
          <span class="author__name label-md">{{ quiz.author.displayName ?? quiz.author.username }}</span>
        </RouterLink>
        <div class="meta label-sm">
          <span>{{ quiz.questionCount ?? 0 }} {{ quiz.questionCount === 1 ? 'question' : 'questions' }}</span>
          <span>·</span>
          <span>{{ fmtDuration(quiz.duration) }}</span>
          <template v-if="(quiz.ratingSummary?.count ?? 0) > 0">
            <span>·</span>
            <span
              class="rating"
              :title="`Average rating: ${fmtRating(quiz.ratingSummary?.average)} / 10 from ${quiz.ratingSummary?.count} rating${quiz.ratingSummary?.count === 1 ? '' : 's'}`"
            >
              <span aria-hidden="true">★</span>
              <strong>{{ fmtRating(quiz.ratingSummary?.average) }}</strong>
              <span class="muted">({{ quiz.ratingSummary?.count }})</span>
            </span>
          </template>
        </div>
      </div>
      <div class="head__actions">
        <Button
          type="button"
          :loading="starting"
          :disabled="!canStart"
          :title="!canStart ? 'Add at least one question to start' : undefined"
          @click="startAttempt"
        >Start attempt</Button>
        <Button
          v-if="quiz.youAreAuthor"
          type="button"
          variant="ghost"
          @click="router.push(`/app/quiz/${quiz.id}/edit`)"
        >Edit</Button>
        <Button
          v-if="quiz.youAreAuthor"
          type="button"
          variant="danger"
          :loading="deleting"
          @click="removeQuiz"
        >Delete</Button>
      </div>
    </header>

    <p v-if="errorText" class="banner label-md" role="alert">{{ errorText }}</p>

    <section v-if="quiz.description" class="description" aria-label="Quiz description">
      <MarkdownView :source="quiz.description" />
    </section>

    <section v-if="ratings.length" class="ratings" aria-labelledby="ratings-heading">
      <header class="ratings__head">
        <h2 id="ratings-heading" class="headline-md">Recent ratings</h2>
        <span class="label-sm muted">{{ totalRatings }} total</span>
      </header>
      <ul class="ratings__list">
        <li v-for="r in ratings" :key="r.createdAt" class="rating-row">
          <div class="rating-row__head">
            <RouterLink
              v-if="r.author?.username"
              :to="{ name: 'user-profile', params: { username: r.author.username } }"
              class="rating-row__author"
            >
              <Avatar
                :username="r.author.username"
                :display-name="r.author.displayName"
                :initials-only="!r.author.hasAvatar"
                :size="20"
              />
              <span class="label-md">{{ r.author.displayName ?? r.author.username }}</span>
            </RouterLink>
            <span class="rating-row__score">
              <span aria-hidden="true">★</span>
              <strong>{{ r.score }}</strong><span class="muted">/10</span>
            </span>
            <span class="rating-row__time label-sm muted">{{ fmtRelative(r.updatedAt ?? r.createdAt) }}</span>
          </div>
          <p v-if="r.comment" class="rating-row__comment body-md">{{ r.comment }}</p>
        </li>
      </ul>
    </section>

    <div class="bottom-actions">
      <Button variant="ghost" @click="router.push('/app')">Back to browse</Button>
    </div>
  </template>
</template>

<style scoped>
.empty { color: var(--on-surface-variant); }
.muted { color: var(--on-surface-variant); }
.banner {
  margin: 0 0 var(--space-lg);
  padding: var(--space-sm) var(--space-md);
  background: var(--error-container);
  color: var(--on-error-container);
  border-radius: var(--radius);
}

.cover {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: cover;
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-lg);
}

.head {
  display: flex;
  gap: var(--space-lg);
  align-items: flex-start;
  margin-bottom: var(--space-xl);
  flex-wrap: wrap;
}
.head__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.head__title-row {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: wrap;
}
.head__title {
  margin: 0;
  font-size: 2rem;
  font-weight: 800;
  letter-spacing: -0.01em;
  color: var(--on-surface);
  min-width: 0;
  word-break: break-word;
}
.head__chip {
  flex-shrink: 0;
  white-space: nowrap;
}
.author {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  padding: 2px 10px 2px 2px;
  border-radius: 999px;
  background: var(--surface-container-high);
  color: var(--on-surface-variant);
  text-decoration: none;
  width: fit-content;
  transition: background-color 120ms ease, color 120ms ease;
}
.author:hover {
  background: var(--surface-container);
  color: var(--on-surface);
}
.author__name {
  font-weight: 600;
  text-transform: none;
  letter-spacing: normal;
}
.meta {
  display: flex;
  gap: var(--space-xs);
  color: var(--on-surface-variant);
  font-variant-numeric: tabular-nums;
}
.rating {
  display: inline-flex;
  align-items: baseline;
  gap: 3px;
  color: var(--on-surface);
}
.head__actions {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.description {
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  margin-bottom: var(--space-xl);
}

.ratings {
  margin-bottom: var(--space-xl);
}
.ratings__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}
.ratings__head h2 {
  margin: 0;
}
.ratings__list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.rating-row {
  padding: var(--space-md);
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
}
.rating-row__head {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: wrap;
}
.rating-row__author {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--on-surface);
  text-decoration: none;
  font-weight: 600;
}
.rating-row__author:hover {
  text-decoration: underline;
}
.rating-row__score {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  color: var(--on-surface);
  font-variant-numeric: tabular-nums;
}
.rating-row__time {
  margin-left: auto;
}
.rating-row__comment {
  margin: var(--space-sm) 0 0;
  color: var(--on-surface);
}

.bottom-actions {
  margin-top: var(--space-lg);
}

.notfound {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  align-items: flex-start;
}
.notfound h1 { margin: 0; }

@media (max-width: 640px) {
  .head { flex-direction: column; }
  .head__actions { width: 100%; }
  .head__actions :deep(button) { flex: 1; }
}
</style>
