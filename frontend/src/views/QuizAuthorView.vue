<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGetQuiz, _delete as deleteQuiz, getGetQuizQueryKey } from '@/api/quiz-controller/quiz-controller'
import { addQuizQuestion, deleteQuizQuestion } from '@/api/question-controller/question-controller'
import { useQueryClient } from '@tanstack/vue-query'
import { errorMessage } from '@/lib/errors'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import Input from '@/components/Input.vue'
import Chip from '@/components/Chip.vue'
import type { OptionData, AddQuestionRequest } from '@/api/openAPIDefinition.schemas'

type QuestionType = AddQuestionRequest['type']
const SINGLE: QuestionType = 'SINGLE_CHOICE'
const MULTI: QuestionType = 'MULTI_CHOICE'

const route = useRoute()
const router = useRouter()
const qc = useQueryClient()

const quizId = computed(() => route.params.quizId as string)
const quiz = useGetQuiz(quizId)

const questionText = ref('')
const questionType = ref<QuestionType>(MULTI)
const optionRows = ref<OptionData[]>([
  { text: '', correct: false },
  { text: '', correct: false },
])
const submitting = ref(false)
const errorText = ref<string | null>(null)

function addOption() {
  optionRows.value.push({ text: '', correct: false })
}
function removeOption(i: number) {
  if (optionRows.value.length > 2) optionRows.value.splice(i, 1)
}
function resetForm() {
  questionText.value = ''
  questionType.value = MULTI
  optionRows.value = [
    { text: '', correct: false },
    { text: '', correct: false },
  ]
}

// When the author flips to single-choice, leave at most one option marked correct.
// Picking another "correct" radio later will unset the others (handled via @change).
watch(questionType, (next) => {
  if (next === SINGLE) {
    let kept = false
    for (const row of optionRows.value) {
      if (row.correct && !kept) {
        kept = true
      } else {
        row.correct = false
      }
    }
  }
})

function setSingleCorrect(i: number) {
  // Radio behaviour for single-choice: clicking row i marks it correct and
  // unticks everyone else.
  for (let j = 0; j < optionRows.value.length; j++) {
    const row = optionRows.value[j]
    if (row) row.correct = j === i
  }
}

async function submitQuestion() {
  errorText.value = null
  if (!questionText.value.trim()) {
    errorText.value = 'Question text is required.'
    return
  }
  if (questionType.value === SINGLE) {
    const correctCount = optionRows.value.filter((o) => o.correct).length
    if (correctCount !== 1) {
      errorText.value = 'Single-choice questions need exactly one correct option.'
      return
    }
  }
  submitting.value = true
  try {
    await addQuizQuestion({
      quizId: quizId.value,
      text: questionText.value.trim(),
      type: questionType.value,
      options: optionRows.value.map((o) => ({ text: o.text?.trim() ?? '', correct: !!o.correct })),
    })
    resetForm()
    qc.invalidateQueries({ queryKey: getGetQuizQueryKey(quizId.value) })
  } catch (e) {
    errorText.value = errorMessage(e)
  } finally {
    submitting.value = false
  }
}

const removingId = ref<string | null>(null)
async function removeQuestion(id?: string) {
  if (!id) return
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
  if (!confirm('Delete this quiz? This cannot be undone.')) return
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
      <h2 class="headline-md">Questions</h2>
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
              <Button variant="danger" :loading="removingId === q.id" @click="removeQuestion(q.id)">Remove</Button>
            </div>
            <p class="body-lg">{{ q.text }}</p>
            <ul class="opts">
              <li v-for="o in q.options ?? []" :key="o.id" :class="['opt', { 'opt--correct': o.correct }]">
                <span>{{ o.text }}</span>
                <Chip v-if="o.correct" tone="success">Correct</Chip>
              </li>
            </ul>
          </Card>
        </li>
      </ol>
    </section>

    <section class="section">
      <h2 class="headline-md">Add a question</h2>
      <Card>
        <form class="form" @submit.prevent="submitQuestion">
          <div class="type-picker" role="radiogroup" aria-label="Question type">
            <button
              type="button"
              :class="['type-pill', { 'type-pill--on': questionType === MULTI }]"
              @click="questionType = MULTI"
            >
              Multi select
            </button>
            <button
              type="button"
              :class="['type-pill', { 'type-pill--on': questionType === SINGLE }]"
              @click="questionType = SINGLE"
            >
              Single choice
            </button>
          </div>
          <p class="type-hint label-sm muted">
            <template v-if="questionType === SINGLE">Worth 1 point. Pick one correct option.</template>
            <template v-else>+1 point per correctly classified option.</template>
          </p>

          <Input v-model="questionText" label="Question text" />
          <div class="opts-form">
            <p class="label-md muted">Options</p>
            <div v-for="(o, i) in optionRows" :key="i" class="opt-row">
              <input
                type="text"
                v-model="o.text"
                placeholder="Option text"
                class="opt-row__text"
              />
              <label class="opt-row__correct label-sm">
                <input
                  v-if="questionType === MULTI"
                  type="checkbox"
                  v-model="o.correct"
                />
                <input
                  v-else
                  type="radio"
                  name="single-correct"
                  :checked="o.correct"
                  @change="setSingleCorrect(i)"
                />
                Correct
              </label>
              <Button variant="ghost" :disabled="optionRows.length <= 2" @click="removeOption(i)">×</Button>
            </div>
            <Button variant="ghost" type="button" @click="addOption">+ Add option</Button>
          </div>
          <p v-if="errorText" class="form__error label-md">{{ errorText }}</p>
          <Button type="submit" :loading="submitting">Add question</Button>
        </form>
      </Card>
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
  margin-bottom: var(--space-xl);
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
}
.qhead__title {
  display: inline-flex;
  align-items: center;
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
.type-picker {
  display: inline-flex;
  background: var(--surface-container-high);
  border: 1px solid var(--outline-variant);
  border-radius: 999px;
  padding: 4px;
  gap: 4px;
  width: fit-content;
}
.type-pill {
  appearance: none;
  background: transparent;
  border: 0;
  color: var(--on-surface-variant);
  padding: 6px 14px;
  border-radius: 999px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 120ms ease, color 120ms ease;
}
.type-pill:hover {
  color: var(--on-surface);
}
.type-pill--on {
  background: var(--surface-container-low);
  color: var(--on-surface);
}
.type-hint {
  margin: 0;
}
.opts-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.opt-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: var(--space-sm);
  align-items: center;
}
.opt-row__text {
  background: var(--surface-container-lowest);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius);
  padding: 8px 12px;
  color: var(--on-surface);
  outline: none;
}
.opt-row__text:focus {
  border-color: var(--primary-container);
}
.opt-row__correct {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--on-surface-variant);
  white-space: nowrap;
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
</style>
