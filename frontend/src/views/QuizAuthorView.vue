<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGetQuiz, _delete as deleteQuiz, getGetQuizQueryKey } from '@/api/quiz-controller/quiz-controller'
import {
  addQuizQuestion,
  deleteQuizQuestion,
  editQuizQuestion,
} from '@/api/question-controller/question-controller'
import { useList as useRatingList, useSummary as useRatingSummary } from '@/api/quiz-rating-controller/quiz-rating-controller'
import { useQueryClient } from '@tanstack/vue-query'
import { errorMessage } from '@/lib/errors'
import { confirmDialog } from '@/lib/confirmDialog'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import Chip from '@/components/Chip.vue'
import Avatar from '@/components/Avatar.vue'
import QuestionForm from '@/components/QuestionForm.vue'
import type { OptionData, AddQuestionRequest, QuestionDto } from '@/api/openAPIDefinition.schemas'

type QuestionType = AddQuestionRequest['type']
const MULTI: QuestionType = 'MULTI_CHOICE'
const SINGLE: QuestionType = 'SINGLE_CHOICE'

const route = useRoute()
const router = useRouter()
const qc = useQueryClient()

const quizId = computed(() => route.params.quizId as string)
const quiz = useGetQuiz(quizId)

// ---------- "Add a question" form ----------
const addText = ref('')
const addType = ref<QuestionType>(MULTI)
const addOptions = ref<OptionData[]>([
  { text: '', correct: false },
  { text: '', correct: false },
])
const submitting = ref(false)
const errorText = ref<string | null>(null)

const addSectionRef = ref<HTMLElement | null>(null)
function scrollToAddForm() {
  addSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function resetAddForm() {
  addText.value = ''
  addType.value = MULTI
  addOptions.value = [
    { text: '', correct: false },
    { text: '', correct: false },
  ]
}

function validateShape(text: string, type: QuestionType, options: OptionData[]): string | null {
  if (!text.trim()) return 'Question text is required.'
  if (type === SINGLE && options.filter((o) => o.correct).length !== 1) {
    return 'Single-choice questions need exactly one correct option.'
  }
  return null
}

async function submitQuestion() {
  errorText.value = null
  const err = validateShape(addText.value, addType.value, addOptions.value)
  if (err) {
    errorText.value = err
    return
  }
  submitting.value = true
  try {
    await addQuizQuestion({
      quizId: quizId.value,
      text: addText.value.trim(),
      type: addType.value,
      options: addOptions.value.map((o) => ({ text: o.text?.trim() ?? '', correct: !!o.correct })),
    })
    resetAddForm()
    qc.invalidateQueries({ queryKey: getGetQuizQueryKey(quizId.value) })
  } catch (e) {
    errorText.value = errorMessage(e)
  } finally {
    submitting.value = false
  }
}

// ---------- Inline edit ----------
const editingId = ref<string | null>(null)
const editText = ref('')
const editType = ref<QuestionType>(MULTI)
const editOptions = ref<OptionData[]>([])
const savingEdit = ref(false)
const editError = ref<string | null>(null)

function startEdit(q: QuestionDto) {
  if (!q.id) return
  editingId.value = q.id
  editText.value = q.text ?? ''
  editType.value = (q.type as QuestionType) ?? MULTI
  editOptions.value = (q.options ?? []).map((o) => ({
    text: o.text ?? '',
    correct: !!o.correct,
  }))
  editError.value = null
}

function cancelEdit() {
  editingId.value = null
  editError.value = null
}

async function saveEdit() {
  if (!editingId.value) return
  editError.value = null
  const err = validateShape(editText.value, editType.value, editOptions.value)
  if (err) {
    editError.value = err
    return
  }
  savingEdit.value = true
  try {
    await editQuizQuestion(editingId.value, {
      text: editText.value.trim(),
      type: editType.value,
      options: editOptions.value.map((o) => ({ text: o.text?.trim() ?? '', correct: !!o.correct })),
    })
    qc.invalidateQueries({ queryKey: getGetQuizQueryKey(quizId.value) })
    editingId.value = null
  } catch (e) {
    editError.value = errorMessage(e)
  } finally {
    savingEdit.value = false
  }
}

// ---------- Remove question ----------
const removingId = ref<string | null>(null)
async function removeQuestion(id?: string) {
  if (!id) return
  const ok = await confirmDialog.open({
    title: 'Remove this question?',
    body: 'This cannot be undone.',
    confirmLabel: 'Remove',
    danger: true,
  })
  if (!ok) return
  // If the user happens to be editing the same question, drop the edit state.
  if (editingId.value === id) cancelEdit()
  removingId.value = id
  errorText.value = null
  try {
    await deleteQuizQuestion(id)
    qc.invalidateQueries({ queryKey: getGetQuizQueryKey(quizId.value) })
  } catch (e) {
    errorText.value = errorMessage(e)
  } finally {
    removingId.value = null
  }
}

async function removeQuiz() {
  const ok = await confirmDialog.open({
    title: 'Delete this quiz?',
    body: 'This cannot be undone.',
    confirmLabel: 'Delete',
    danger: true,
  })
  if (!ok) return
  errorText.value = null
  try {
    await deleteQuiz(quizId.value)
    router.push('/app')
  } catch (e) {
    errorText.value = errorMessage(e)
  }
}

function typeLabel(t: QuestionType | undefined | null): string {
  return t === SINGLE ? 'Single choice' : 'Multi select'
}

watch(quizId, () => qc.invalidateQueries({ queryKey: getGetQuizQueryKey(quizId.value) }))

// ---------- Ratings (paginated list + summary) ----------
const ratingsPage = ref(0)
const ratingsParams = computed(() => ({ page: ratingsPage.value }))
const ratingsList = useRatingList(quizId, ratingsParams)
const ratingsSummary = useRatingSummary(quizId)
const ratings = computed(() => ratingsList.data.value?._embedded?.ratings ?? [])
const ratingsTotalPages = computed(() => ratingsList.data.value?.page?.totalPages ?? 1)

function fmtRating(avg: number | null | undefined): string {
  if (avg == null) return '—'
  return avg.toFixed(1).replace(/\.0$/, '')
}

function fmtRelative(iso?: string): string {
  if (!iso) return ''
  const then = new Date(iso).getTime()
  if (Number.isNaN(then)) return ''
  const diffSec = Math.round((Date.now() - then) / 1000)
  const abs = Math.abs(diffSec)
  if (abs < 45) return 'just now'
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
  <div v-if="quiz.isLoading.value" class="empty body-md">Loading…</div>
  <Card v-else-if="quiz.isError.value || !quiz.data.value" class="notfound">
    <h1 class="headline-md">Quiz not found</h1>
    <p class="body-md muted">This quiz doesn't exist or you don't have access to it.</p>
    <Button @click="router.push('/app')">Back to browse</Button>
  </Card>
  <template v-else>
    <header class="head">
      <div>
        <h1 class="headline-lg">{{ quiz.data.value.name }}</h1>
        <p class="meta body-md">
          {{ quiz.data.value.questionCount ?? 0 }} questions · {{ quiz.data.value.duration }}
        </p>
      </div>
      <div class="head__actions">
        <Button variant="ghost" @click="router.push('/app')">Back</Button>
        <Button variant="danger" @click="removeQuiz">Delete quiz</Button>
      </div>
    </header>

    <section class="section">
      <div class="section__head">
        <h2 class="headline-md">Questions</h2>
        <Button
          v-if="(quiz.data.value.questions?.length ?? 0) >= 3"
          variant="ghost"
          @click="scrollToAddForm"
        >+ Add another</Button>
      </div>
      <div v-if="!(quiz.data.value.questions?.length)" class="empty body-md">
        No questions yet. Add one below.
      </div>
      <ol class="qlist">
        <li v-for="(q, i) in quiz.data.value.questions ?? []" :key="q.id">
          <Card>
            <div class="qhead">
              <div class="qhead__title">
                <span class="label-sm muted">Q{{ i + 1 }}</span>
                <Chip>{{ typeLabel(q.type) }}</Chip>
              </div>
              <div class="qhead__actions">
                <template v-if="editingId !== q.id">
                  <Button variant="ghost" @click="startEdit(q)">Edit</Button>
                  <Button
                    variant="danger"
                    :loading="removingId === q.id"
                    @click="removeQuestion(q.id)"
                  >Remove</Button>
                </template>
              </div>
            </div>

            <template v-if="editingId !== q.id">
              <p class="body-lg">{{ q.text }}</p>
              <ul class="opts">
                <li
                  v-for="o in q.options ?? []"
                  :key="o.id"
                  :class="['opt', { 'opt--correct': o.correct }]"
                >
                  <span>{{ o.text }}</span>
                  <Chip v-if="o.correct" tone="success">Correct</Chip>
                </li>
              </ul>
            </template>
            <template v-else>
              <form class="form" @submit.prevent="saveEdit">
                <QuestionForm
                  v-model:text="editText"
                  v-model:type="editType"
                  v-model:options="editOptions"
                  :scope-id="`edit-${q.id}`"
                />
                <p v-if="editError" class="form__error label-md" role="alert">{{ editError }}</p>
                <div class="edit-actions">
                  <Button type="button" variant="ghost" @click="cancelEdit">Cancel</Button>
                  <Button type="submit" :loading="savingEdit">Save changes</Button>
                </div>
              </form>
            </template>
          </Card>
        </li>
      </ol>
    </section>

    <section class="section" ref="addSectionRef">
      <h2 class="headline-md">Add a question</h2>
      <Card>
        <form class="form" @submit.prevent="submitQuestion">
          <QuestionForm
            v-model:text="addText"
            v-model:type="addType"
            v-model:options="addOptions"
            scope-id="add"
          />
          <p v-if="errorText" class="form__error label-md" role="alert">{{ errorText }}</p>
          <Button type="submit" :loading="submitting">Add question</Button>
        </form>
      </Card>
    </section>

    <section class="section">
      <div class="section__head">
        <h2 class="headline-md">
          Ratings
          <span v-if="(ratingsSummary.data.value?.count ?? 0) > 0" class="muted label-md">
            · {{ fmtRating(ratingsSummary.data.value?.average ?? null) }} / 10 from {{ ratingsSummary.data.value?.count }}
          </span>
        </h2>
      </div>
      <p v-if="ratingsList.isLoading.value" class="empty body-md">Loading…</p>
      <p v-else-if="!ratings.length" class="empty body-md">
        No ratings yet. Once people finish your quiz they can leave a rating + comment here.
      </p>
      <ul v-else class="rating-list">
        <li v-for="r in ratings" :key="r.id" class="rating-item">
          <div class="rating-item__head">
            <RouterLink
              v-if="r.author?.username"
              :to="{ name: 'user-profile', params: { username: r.author.username } }"
              class="rating-item__user"
            >
              <Avatar
                :username="r.author.username"
                :display-name="r.author.displayName"
                :initials-only="!r.author.hasAvatar"
                :size="28"
              />
              <span class="label-md">{{ r.author.displayName ?? r.author.username }}</span>
            </RouterLink>
            <span class="rating-item__score" :title="`${r.score} of 10`">
              <span aria-hidden="true">★</span>
              <span class="rating-item__score-value">{{ r.score }}</span>
              <span class="muted">/ 10</span>
            </span>
            <span class="label-sm muted">{{ fmtRelative(r.createdAt) }}</span>
          </div>
          <p v-if="r.comment" class="rating-item__body body-md">{{ r.comment }}</p>
        </li>
      </ul>
      <div v-if="ratingsTotalPages > 1" class="rating-pager">
        <Button
          variant="ghost"
          :disabled="ratingsPage === 0"
          @click="ratingsPage = Math.max(0, ratingsPage - 1)"
        >Previous</Button>
        <span class="label-sm muted">Page {{ ratingsPage + 1 }} / {{ ratingsTotalPages }}</span>
        <Button
          variant="ghost"
          :disabled="ratingsPage + 1 >= ratingsTotalPages"
          @click="ratingsPage = ratingsPage + 1"
        >Next</Button>
      </div>
    </section>
  </template>
</template>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-md);
  margin-bottom: var(--space-xl);
}
.head__actions {
  display: flex;
  gap: var(--space-sm);
}
.meta {
  color: var(--on-surface-variant);
  margin: var(--space-xs) 0 0;
}
.section {
  margin: var(--space-xl) 0;
}
.section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}
.section__head h2 {
  margin: 0;
}
.section h2 {
  margin: 0 0 var(--space-md);
}
.qlist {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.qhead {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-sm);
  flex-wrap: wrap;
}
.qhead__title {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
}
.qhead__actions {
  display: inline-flex;
  gap: var(--space-sm);
}
.opts {
  list-style: none;
  padding: 0;
  margin: var(--space-md) 0 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.opt {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-sm) var(--space-md);
  background: var(--surface-container-low);
  border-radius: var(--radius);
}
.opt--correct {
  border: 1px solid var(--secondary-container);
}
.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.form__error {
  color: var(--error);
}
.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
}
.empty {
  color: var(--on-surface-variant);
}
.notfound {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  align-items: flex-start;
}
.notfound h1 {
  margin: 0;
}
.muted {
  color: var(--on-surface-variant);
}

/* ----- Ratings list ----- */
.rating-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.rating-item {
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.rating-item__head {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: wrap;
}
.rating-item__user {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--on-surface);
  text-decoration: none;
}
.rating-item__user:hover {
  text-decoration: underline;
}
.rating-item__score {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--on-surface);
  font-variant-numeric: tabular-nums;
  font-weight: 700;
}
.rating-item__score-value {
  font-size: 1.05rem;
}
.rating-item__body {
  margin: 0;
  white-space: pre-wrap;
  color: var(--on-surface);
}
.rating-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  margin-top: var(--space-md);
}

@media (max-width: 480px) {
  .head {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-md);
  }
  .head__actions {
    width: 100%;
    justify-content: flex-start;
  }
  .rating-item__score {
    margin-left: 0;
  }
}
</style>
