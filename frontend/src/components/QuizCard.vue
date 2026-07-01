<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'
import { deleteQuiz, getGetQuizzesQueryKey } from '@/api/quiz-controller/quiz-controller'
import { getGetQuizzesByAuthorQueryKey } from '@/api/user-profile-controller/user-profile-controller'
import { errorMessage } from '@/lib/errors'
import { confirmDialog } from '@/lib/confirmDialog'
import { coverUrl } from '@/lib/quizImages'
import type { QuizSummaryDto } from '@/api/openAPIDefinition.schemas'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import Chip from '@/components/Chip.vue'
import Avatar from '@/components/Avatar.vue'

const props = withDefaults(
  defineProps<{
    quiz: QuizSummaryDto
    /** Show edit/delete affordances when the viewer authored this quiz. Default true. */
    showAuthorActions?: boolean
    /** Show the author chip with avatar (off by default for "Your quizzes" lists). */
    showAuthor?: boolean
  }>(),
  { showAuthorActions: true, showAuthor: true },
)
const emit = defineEmits<{
  (e: 'error', message: string): void
  (e: 'deleted', id: string): void
}>()

const router = useRouter()
const qc = useQueryClient()

const deleting = ref(false)

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

function fmtRating(avg: number | undefined): string {
  if (avg == null) return '—'
  return avg.toFixed(1).replace(/\.0$/, '')
}

function openOverview() {
  if (props.quiz.id) router.push(`/app/quiz/${props.quiz.id}`)
}

async function removeQuiz() {
  if (!props.quiz.id) return
  const ok = await confirmDialog.open({
    title: 'Delete this quiz?',
    body: 'This cannot be undone.',
    confirmLabel: 'Delete',
    danger: true,
  })
  if (!ok) return
  deleting.value = true
  try {
    await deleteQuiz(props.quiz.id)
    qc.invalidateQueries({ queryKey: getGetQuizzesQueryKey() })
    // Also invalidate any "quizzes by author" lists so they remove this card
    // without needing a route change.
    if (props.quiz.author?.username) {
      qc.invalidateQueries({ queryKey: getGetQuizzesByAuthorQueryKey(props.quiz.author.username) })
    }
    emit('deleted', props.quiz.id)
  } catch (e) {
    emit('error', errorMessage(e))
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <Card
    interactive
    :aria-label="`View quiz: ${quiz.name}`"
    role="button"
    :tabindex="0"
    @click="openOverview"
    @keydown.enter.prevent="openOverview"
    @keydown.space.prevent="openOverview"
  >
    <img
      v-if="quiz.hasCover && quiz.id"
      :src="coverUrl(quiz.id)"
      alt=""
      class="cover"
      loading="lazy"
    />
    <div v-else class="cover cover--placeholder" aria-hidden="true">
      <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <rect x="3" y="5" width="18" height="14" rx="2" />
        <circle cx="8.5" cy="10.5" r="1.5" />
        <path d="M21 16l-4.5-4.5L7 21" />
      </svg>
    </div>
    <div class="row">
      <h3 class="headline-md quiz-card__title">{{ quiz.name }}</h3>
      <div class="quiz-card__chips">
        <Chip v-if="quiz.pinned" tone="warning" class="quiz-card__author-chip">
          <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <line x1="12" y1="17" x2="12" y2="22"/>
            <path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a1 1 0 0 0 1-1V4a1 1 0 0 0-1-1H8a1 1 0 0 0-1 1v1a1 1 0 0 0 1 1h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24z"/>
          </svg>
          Pinned
        </Chip>
        <Chip v-if="quiz.youAreAuthor" class="quiz-card__author-chip">Your quiz</Chip>
      </div>
    </div>
    <RouterLink
      v-if="showAuthor && quiz.author?.username"
      :to="{ name: 'user-profile', params: { username: quiz.author.username } }"
      class="author"
      @click.stop
      @keydown.enter.stop
      @keydown.space.stop
    >
      <Avatar
        :username="quiz.author.username"
        :display-name="quiz.author.displayName"
        :initials-only="!quiz.author.hasAvatar"
        :size="20"
      />
      <span class="author__name label-sm">{{ quiz.author.displayName ?? quiz.author.username }}</span>
    </RouterLink>
    <div class="meta-row label-sm">
      <span>{{ quiz.questionCount ?? 0 }} questions</span>
      <span>·</span>
      <span>{{ fmtDuration(quiz.duration) }}</span>
      <span v-if="(quiz.ratingSummary?.count ?? 0) > 0">·</span>
      <span
        v-if="(quiz.ratingSummary?.count ?? 0) > 0"
        class="rating"
        :title="`Average rating: ${fmtRating(quiz.ratingSummary?.average ?? undefined)} / 10 from ${quiz.ratingSummary?.count} rating${quiz.ratingSummary?.count === 1 ? '' : 's'}`"
      >
        <span aria-hidden="true">★</span>
        <span class="rating__avg">{{ fmtRating(quiz.ratingSummary?.average ?? undefined) }}</span>
        <span class="rating__count muted">({{ quiz.ratingSummary?.count }})</span>
      </span>
    </div>
    <div v-if="showAuthorActions && quiz.youAreAuthor" class="actions" @click.stop>
      <Button
        type="button"
        variant="ghost"
        @click.stop="router.push(`/app/quiz/${quiz.id}/edit`)"
      >Edit</Button>
      <Button
        type="button"
        variant="danger"
        :loading="deleting"
        @click.stop="removeQuiz"
      >Delete</Button>
    </div>
    <p v-if="!quiz.questionCount" class="quiz-card__empty label-sm muted">No questions yet.</p>
  </Card>
</template>

<style scoped>
.cover {
  display: block;
  width: calc(100% + 2 * var(--space-lg) + 2px);
  aspect-ratio: 16 / 9;
  height: auto;
  object-fit: cover;
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  margin: calc(-1 * var(--space-lg) - 1px) calc(-1 * var(--space-lg) - 1px) var(--space-md);
}
.cover--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-container-low);
  color: var(--on-surface-variant);
  opacity: 0.5;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
}
.quiz-card__title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}
/* Keep the chip on one line even when the title is long — flex-shrink: 0
 * prevents it from wrapping into a second row, white-space: nowrap keeps
 * the label itself intact. */
.quiz-card__author-chip {
  flex-shrink: 0;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.quiz-card__chips {
  display: inline-flex;
  gap: 6px;
  flex-shrink: 0;
  flex-wrap: wrap;
}
.quiz-card__empty {
  margin: var(--space-sm) 0 0;
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
.author {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  margin: 0 0 var(--space-sm);
  padding: 2px 8px 2px 2px;
  border-radius: 999px;
  color: var(--on-surface-variant);
  background: var(--surface-container-high);
  text-decoration: none;
  transition: background-color 120ms ease;
  width: fit-content;
}
.author:hover {
  background: var(--surface-container);
  text-decoration: none;
  color: var(--on-surface);
}
.author__name {
  font-variant-numeric: tabular-nums;
  text-transform: none;
  letter-spacing: normal;
  font-weight: 600;
}
.rating {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--on-surface);
  font-variant-numeric: tabular-nums;
}
.rating__avg {
  font-weight: 700;
}
.rating__count {
  color: var(--on-surface-variant);
}
.muted {
  color: var(--on-surface-variant);
}
</style>
