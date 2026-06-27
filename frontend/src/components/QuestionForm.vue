<script setup lang="ts">
import { watch } from 'vue'
import type { OptionData, AddQuestionRequest } from '@/api/openAPIDefinition.schemas'

type QuestionType = AddQuestionRequest['type']

const SINGLE: QuestionType = 'SINGLE_CHOICE'
const MULTI: QuestionType = 'MULTI_CHOICE'

// Note: `questionType` and `optionRows` are bound via v-model from the parent
// so this component is purely the inputs — saving/cancelling belongs upstream.
const questionType = defineModel<QuestionType>('type', { required: true })
const optionRows = defineModel<OptionData[]>('options', { required: true })
const questionText = defineModel<string>('text', { required: true })

defineProps<{
  /** Retained for parent disambiguation, no longer drives the input names
   *  (a single styled checkbox handles both single- and multi-choice now). */
  scopeId: string
}>()

function addOption() {
  optionRows.value = [...optionRows.value, { text: '', correct: false }]
}
function removeOption(i: number) {
  if (optionRows.value.length > 2) {
    optionRows.value = optionRows.value.filter((_, idx) => idx !== i)
  }
}
function setSingleCorrect(i: number) {
  optionRows.value = optionRows.value.map((row, j) => ({ ...row, correct: j === i }))
}
function onCorrectToggle(i: number, next: boolean) {
  if (questionType.value === SINGLE) {
    // Single-choice: ticking an option clears all siblings; unticking the
    // currently-correct one is a no-op (the form needs exactly one correct).
    if (!next) return
    setSingleCorrect(i)
    return
  }
  optionRows.value = optionRows.value.map((row, j) =>
    j === i ? { ...row, correct: next } : row,
  )
}

// Switching to single-choice must trim multiple-correct down to one (or zero
// if nothing was picked). Otherwise the server-side INVALID_QUESTION_SHAPE
// would reject and the form would have to surface a stale error.
watch(questionType, (next) => {
  if (next === SINGLE) {
    let kept = false
    optionRows.value = optionRows.value.map((row) => {
      if (row.correct && !kept) {
        kept = true
        return row
      }
      return { ...row, correct: false }
    })
  }
})
</script>

<template>
  <div class="qform">
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

    <label class="field">
      <span class="field__label label-md">Question text</span>
      <input
        v-model="questionText"
        type="text"
        class="field__input"
        aria-label="Question text"
      />
    </label>

    <div class="opts-form">
      <p class="label-md muted">Options</p>
      <div v-for="(o, i) in optionRows" :key="i" class="opt-row">
        <input
          type="text"
          v-model="o.text"
          placeholder="Option text"
          class="opt-row__text"
        />
        <label class="opt-row__correct">
          <input
            type="checkbox"
            class="visually-hidden"
            :checked="o.correct"
            :aria-label="questionType === SINGLE ? 'Correct (only one)' : 'Correct'"
            @change="onCorrectToggle(i, ($event.target as HTMLInputElement).checked)"
          />
          <span
            :class="[
              'opt-row__marker',
              questionType === SINGLE ? 'opt-row__marker--radio' : 'opt-row__marker--checkbox',
              { 'opt-row__marker--on': o.correct },
            ]"
            aria-hidden="true"
          />
          <span class="opt-row__correct-label">Correct</span>
        </label>
        <button
          type="button"
          class="opt-row__remove"
          :disabled="optionRows.length <= 2"
          aria-label="Remove option"
          @click="removeOption(i)"
        >×</button>
      </div>
      <button type="button" class="opt-row__add" @click="addOption">+ Add option</button>
    </div>
  </div>
</template>

<style scoped>
.qform {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
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

.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.field__label {
  color: var(--on-surface-variant);
}
.field__input {
  background: var(--surface-container-lowest);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius);
  padding: 8px 12px;
  color: var(--on-surface);
  outline: none;
}
.field__input:focus {
  border-color: var(--primary-container);
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

/* Custom-styled radio / checkbox. The native <input> is visually hidden but
 * stays in the DOM for keyboard/screen-reader semantics — the visible part
 * is a sibling <span> that's driven by .opt-row__marker--on. */
.opt-row__correct {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--on-surface-variant);
  white-space: nowrap;
  font-size: 0.875rem;
  letter-spacing: 0.01em;
}
.opt-row__correct-label {
  user-select: none;
}
.opt-row__marker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: 1.5px solid var(--outline);
  background: var(--surface-container-lowest);
  transition: border-color 120ms ease, background-color 120ms ease;
  flex-shrink: 0;
}
.opt-row__marker--checkbox {
  border-radius: 5px;
}
.opt-row__marker--radio {
  border-radius: 50%;
}
.opt-row__correct:hover .opt-row__marker {
  border-color: var(--on-surface);
}
.opt-row__marker--on {
  border-color: var(--on-secondary-container);
  background: var(--on-secondary-container);
}
.opt-row__marker--checkbox.opt-row__marker--on::after {
  content: '';
  display: block;
  width: 6px;
  height: 11px;
  margin-top: -2px;
  border-right: 2px solid #000;
  border-bottom: 2px solid #000;
  transform: rotate(45deg);
}
.opt-row__marker--radio.opt-row__marker--on::after {
  content: '';
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #000;
}
.opt-row__correct:has(input:focus-visible) .opt-row__marker {
  outline: 2px solid var(--on-secondary-container);
  outline-offset: 2px;
}
.opt-row__correct:has(input:checked) {
  color: var(--on-surface);
}

.visually-hidden {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  opacity: 0;
  cursor: pointer;
  z-index: 1;
}

.opt-row__remove {
  appearance: none;
  background: transparent;
  border: 1px solid var(--outline-variant);
  color: var(--on-surface-variant);
  width: 32px;
  height: 32px;
  border-radius: var(--radius);
  cursor: pointer;
  font-size: 1.1rem;
  line-height: 1;
}
.opt-row__remove:not(:disabled):hover {
  border-color: var(--outline);
  color: var(--on-surface);
}
.opt-row__remove:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.opt-row__add {
  appearance: none;
  background: transparent;
  border: 1px dashed var(--outline-variant);
  color: var(--on-surface-variant);
  padding: 8px 12px;
  border-radius: var(--radius);
  cursor: pointer;
  align-self: flex-start;
  font: inherit;
}
.opt-row__add:hover {
  border-style: solid;
  color: var(--on-surface);
}
</style>
