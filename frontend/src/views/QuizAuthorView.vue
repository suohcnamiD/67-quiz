<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGetQuiz, _delete as deleteQuiz, getGetQuizQueryKey } from '@/api/quiz-controller/quiz-controller'
import { addQuizQuestion, deleteQuizQuestion } from '@/api/question-controller/question-controller'
import { useQueryClient } from '@tanstack/vue-query'
import { firstErrorCode } from '@/lib/axios'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import Input from '@/components/Input.vue'
import Chip from '@/components/Chip.vue'
import type { OptionData } from '@/api/openAPIDefinition.schemas'

const route = useRoute()
const router = useRouter()
const qc = useQueryClient()

const quizId = computed(() => route.params.quizId as string)
const quiz = useGetQuiz(quizId)

const questionText = ref('')
const optionRows = ref<OptionData[]>([
  { text: '', correct: false },
  { text: '', correct: false },
])
const submitting = ref(false)
const errorMessage = ref<string | null>(null)

function addOption() {
  optionRows.value.push({ text: '', correct: false })
}
function removeOption(i: number) {
  if (optionRows.value.length > 2) optionRows.value.splice(i, 1)
}
function resetForm() {
  questionText.value = ''
  optionRows.value = [
    { text: '', correct: false },
    { text: '', correct: false },
  ]
}

async function submitQuestion() {
  errorMessage.value = null
  if (!questionText.value.trim()) {
    errorMessage.value = 'Question text is required.'
    return
  }
  submitting.value = true
  try {
    await addQuizQuestion({
      quizId: quizId.value,
      text: questionText.value.trim(),
      options: optionRows.value.map((o) => ({ text: o.text?.trim() ?? '', correct: !!o.correct })),
    })
    resetForm()
    qc.invalidateQueries({ queryKey: getGetQuizQueryKey(quizId.value) })
  } catch (e) {
    const code = firstErrorCode(e)
    errorMessage.value = code === 'BLANK_OPTION_TEXT' ? 'Option text cannot be blank.' : code ?? 'Failed to add question.'
  } finally {
    submitting.value = false
  }
}

const removingId = ref<string | null>(null)
async function removeQuestion(id?: string) {
  if (!id) return
  removingId.value = id
  try {
    await deleteQuizQuestion(id)
    qc.invalidateQueries({ queryKey: getGetQuizQueryKey(quizId.value) })
  } finally {
    removingId.value = null
  }
}

async function removeQuiz() {
  if (!confirm('Delete this quiz? This cannot be undone.')) return
  await deleteQuiz(quizId.value)
  router.push('/app')
}

watch(quizId, () => qc.invalidateQueries({ queryKey: getGetQuizQueryKey(quizId.value) }))
</script>

<template>
  <div v-if="quiz.isLoading.value" class="empty body-md">Loading…</div>
  <div v-else-if="!quiz.data.value" class="empty body-md">Quiz not found.</div>
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
              <span class="label-sm muted">Q{{ i + 1 }}</span>
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
                <input type="checkbox" v-model="o.correct" />
                Correct
              </label>
              <Button variant="ghost" :disabled="optionRows.length <= 2" @click="removeOption(i)">×</Button>
            </div>
            <Button variant="ghost" type="button" @click="addOption">+ Add option</Button>
          </div>
          <p v-if="errorMessage" class="form__error label-md">{{ errorMessage }}</p>
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
  margin-bottom: var(--space-sm);
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
.muted {
  color: var(--on-surface-variant);
}
</style>
