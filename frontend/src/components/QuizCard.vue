<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'
import { deleteQuiz, getGetQuizzesQueryKey } from '@/api/quiz-controller/quiz-controller'
import { getGetQuizzesByAuthorQueryKey } from '@/api/user-profile-controller/user-profile-controller'
import { errorMessage } from '@/lib/errors'
import { confirmDialog } from '@/lib/confirmDialog'
import { coverUrl } from '@/lib/quizImages'
import { useAuthStore } from '@/stores/auth'
import type { QuizSummaryDto } from '@/api/openAPIDefinition.schemas'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import Chip from '@/components/Chip.vue'
import Avatar from '@/components/Avatar.vue'

const props = withDefaults(
  defineProps<{
    quiz: QuizSummaryDto
    showAuthorActions?: boolean
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
const auth = useAuthStore()

const deleting = ref(false)

// The Edit/Delete row shows for the author of the quiz OR any admin. Admin
// gets a red-tinted variant so the destructive action is obvious even on
// someone else's quiz. Ownership is still gated by the backend.
const canManage = computed(() => !!props.quiz.youAreAuthor || !!auth.isAdmin)
const isAdminActingOnOthers = computed(() => !props.quiz.youAreAuthor && !!auth.isAdmin)

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
    :class="['quiz-card', { 'quiz-card--pinned': quiz.pinned }]"
    @click="openOverview"
    @keydown.enter.prevent="openOverview"
    @keydown.space.prevent="openOverview"
  >
    <div class="cover-wrap">
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
      <div
        v-if="(quiz.ratingSummary?.count ?? 0) > 0"
        class="rating-badge"
        :title="`Average rating: ${fmtRating(quiz.ratingSummary?.average ?? undefined)} from ${quiz.ratingSummary?.count} rating${quiz.ratingSummary?.count === 1 ? '' : 's'}`"
      >
        <svg class="rating-badge__star" viewBox="0 0 24 24" width="14" height="14" fill="currentColor" aria-hidden="true">
          <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
        </svg>
        <span class="rating-badge__avg">{{ fmtRating(quiz.ratingSummary?.average ?? undefined) }}</span>
      </div>
      <div class="chips-overlay">
        <Chip v-if="quiz.pinned" tone="warning" class="overlay-chip">
          <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <line x1="12" y1="17" x2="12" y2="22"/>
            <path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a1 1 0 0 0 1-1V4a1 1 0 0 0-1-1H8a1 1 0 0 0-1 1v1a1 1 0 0 0 1 1h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24z"/>
          </svg>
          Pinned
        </Chip>
        <Chip v-if="quiz.youAreAuthor" class="overlay-chip">Your quiz</Chip>
      </div>
    </div>

    <div class="body">
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

      <h3 class="headline-md quiz-card__title">{{ quiz.name }}</h3>

      <div class="meta-row label-sm">
        <span>{{ quiz.questionCount ?? 0 }} questions</span>
        <span>·</span>
        <span>{{ fmtDuration(quiz.duration) }}</span>
      </div>

      <div v-if="showAuthorActions && canManage" class="actions" @click.stop>
        <Button
          v-if="quiz.youAreAuthor"
          type="button"
          variant="ghost"
          @click.stop="router.push(`/app/quiz/${quiz.id}/edit`)"
        >Edit</Button>
        <Button
          type="button"
          variant="danger"
          :loading="deleting"
          @click.stop="removeQuiz"
        >{{ isAdminActingOnOthers ? 'Delete (admin)' : 'Delete' }}</Button>
      </div>
    </div>
  </Card>
</template>

<style scoped>
.quiz-card {
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
  position: relative;
}
/* Pinned quizzes get an amber accent bar + a soft glow so they read as
 * "editorially surfaced" without shouting. The badge on the cover already
 * says "Pinned"; the card treatment just reinforces it in the grid. */
.quiz-card--pinned {
  box-shadow: 0 0 0 1px color-mix(in srgb, #d9a24a 55%, transparent),
    0 6px 24px -12px color-mix(in srgb, #d9a24a 60%, transparent);
}
.quiz-card--pinned::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 3px;
  background: linear-gradient(90deg, #f5c04a, #d9a24a);
  z-index: 1;
  pointer-events: none;
}
.cover-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  background: var(--surface-container-low);
  flex-shrink: 0;
}
.cover {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--on-surface-variant);
  opacity: 0.5;
}
.rating-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(0, 0, 0, 0.72);
  color: #fff;
  border-radius: 999px;
  font-variant-numeric: tabular-nums;
  backdrop-filter: blur(4px);
}
.rating-badge__star {
  color: #f5c04a;
}
.rating-badge__avg {
  font-weight: 700;
  font-size: 0.9rem;
  line-height: 1;
}
.chips-overlay {
  position: absolute;
  top: 8px;
  left: 8px;
  display: inline-flex;
  gap: 6px;
  flex-wrap: wrap;
  max-width: calc(100% - 100px);
}
.overlay-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}
.body {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  padding: var(--space-lg);
  flex: 1;
}
.author {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
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
  color: var(--on-surface);
  text-decoration: none;
}
.author__name {
  font-variant-numeric: tabular-nums;
  text-transform: none;
  letter-spacing: normal;
  font-weight: 600;
}
.quiz-card__title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
  margin: 0;
  min-height: 2.6em;
}
.meta-row {
  display: flex;
  gap: var(--space-xs);
  color: var(--on-surface-variant);
}
.actions {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
  margin-top: auto;
  padding-top: var(--space-sm);
}
</style>
