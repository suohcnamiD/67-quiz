<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createQuiz } from '@/api/quiz-controller/quiz-controller'
import { errorMessage, validationFieldErrors } from '@/lib/errors'
import Button from '@/components/Button.vue'
import Input from '@/components/Input.vue'
import Card from '@/components/Card.vue'

const router = useRouter()
const name = ref('')
const minutes = ref('15')
const submitting = ref(false)
const errorText = ref<string | null>(null)
const nameError = ref<string | null>(null)
const minutesError = ref<string | null>(null)

function clearErrors() {
  errorText.value = null
  nameError.value = null
  minutesError.value = null
}

async function submit() {
  clearErrors()
  const mins = Number(minutes.value)
  if (!name.value.trim()) {
    nameError.value = 'Give the quiz a name.'
    return
  }
  if (!Number.isFinite(mins) || mins <= 0) {
    minutesError.value = 'Duration must be a positive number of minutes.'
    return
  }
  submitting.value = true
  try {
    const quiz = await createQuiz({
      quizName: name.value.trim(),
      quizDuration: `PT${mins}M`,
    })
    if (quiz.id) router.push(`/app/quiz/${quiz.id}`)
  } catch (e) {
    const fieldErrors = validationFieldErrors(e)
    if (fieldErrors.quizName) nameError.value = fieldErrors.quizName
    if (fieldErrors.quizDuration) minutesError.value = fieldErrors.quizDuration
    if (!fieldErrors.quizName && !fieldErrors.quizDuration) {
      errorText.value = errorMessage(e)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <Card>
      <h1 class="headline-lg">New quiz</h1>
      <p class="subtitle body-md">Set the rules. Add questions next.</p>
      <form class="form" @submit.prevent="submit">
        <Input
          v-model="name"
          label="Quiz name"
          placeholder="e.g. Trivia Showdown"
          :error="nameError ?? undefined"
        />
        <Input
          v-model="minutes"
          label="Duration (minutes)"
          type="number"
          :error="minutesError ?? undefined"
        />
        <p v-if="errorText" class="form__error label-md" role="alert">{{ errorText }}</p>
        <div class="actions">
          <Button variant="ghost" @click="router.push('/app')">Cancel</Button>
          <Button type="submit" :loading="submitting">Create</Button>
        </div>
      </form>
    </Card>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  justify-content: center;
}
.page > :deep(.card) {
  width: 100%;
  max-width: 560px;
}
.subtitle {
  color: var(--on-surface-variant);
  margin: var(--space-xs) 0 var(--space-lg);
}
.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.form__error {
  color: var(--error);
}
.actions {
  display: flex;
  gap: var(--space-sm);
  justify-content: flex-end;
}
</style>
