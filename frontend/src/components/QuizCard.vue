<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'
import {
  attemptQuiz,
  getGetAttemptsInProgressQueryKey,
} from '@/api/attempt-controller/attempt-controller'
import { _delete as deleteQuiz, getGetQuizzesQueryKey } from '@/api/quiz-controller/quiz-controller'
import { getGetQuizzesByAuthorQueryKey } from '@/api/user-profile-controller/user-profile-controller'
import { errorMessage } from '@/lib/errors'
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

const starting = ref(false)
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

async function startAttempt() {
  if (!props.quiz.id) return
  starting.value = true
  try {
    const attempt = await attemptQuiz({ quizId: props.quiz.id })
    qc.invalidateQueries({ queryKey: getGetAttemptsInProgressQueryKey() })
    if (attempt.id) router.push(`/app/attempt/${attempt.id}`)
  } catch (e) {
    emit('error', errorMessage(e))
  } finally {
    starting.value = false
  }
}

async function removeQuiz() {
  if (!props.quiz.id) return
  if (!confirm('Delete this quiz? This cannot be undone.')) return
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
  <Card>
    <div class="row">
      <h3 class="headline-md">{{ quiz.name }}</h3>
      <Chip v-if="quiz.youAreAuthor">Your quiz</Chip>
    </div>
    <RouterLink
      v-if="showAuthor && quiz.author?.username"
      :to="{ name: 'user-profile', params: { username: quiz.author.username } }"
      class="author"
      @click.stop
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
      <span v-if="quiz.maximumScore != null">·</span>
      <span v-if="quiz.maximumScore != null">Max {{ quiz.maximumScore }} pts</span>
    </div>
    <div class="actions">
      <Button type="button" :loading="starting" @click="startAttempt">Start attempt</Button>
      <Button
        v-if="showAuthorActions && quiz.youAreAuthor"
        type="button"
        variant="ghost"
        @click="router.push(`/app/quiz/${quiz.id}`)"
      >Edit</Button>
      <Button
        v-if="showAuthorActions && quiz.youAreAuthor"
        type="button"
        variant="danger"
        :loading="deleting"
        @click="removeQuiz"
      >Delete</Button>
    </div>
  </Card>
</template>

<style scoped>
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
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
</style>
