<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGetFinishedAttempts } from '@/api/attempt-controller/attempt-controller'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import FinishCelebration from '@/components/FinishCelebration.vue'
import type { FinishedQuestionDto, FinishedOptionDto } from '@/api/openAPIDefinition.schemas'

const route = useRoute()
const router = useRouter()
const attemptId = computed(() => route.params.attemptId as string)

const { data, isPending } = useGetFinishedAttempts({ page: 0 })
const attempt = computed(() =>
  (data.value?._embedded?.attempts ?? []).find((a) => a.id === attemptId.value),
)

function questionScore(q: FinishedQuestionDto): { earned: number; max: number } {
  const opts = q.options ?? []
  return {
    earned: opts.filter((o) => o.correctlySelected).length,
    max: opts.length,
  }
}

// Verdict for the hero block.
const percent = computed(() => {
  const max = attempt.value?.maximumScore ?? 0
  if (!max) return 0
  return (attempt.value!.score ?? 0) / max
})
const percentLabel = computed(() => `${Math.round(percent.value * 100)}%`)
const verdict = computed(() => {
  const p = percent.value
  if (p >= 0.85) return { label: 'Brilliant', tone: 'great' }
  if (p >= 0.5) return { label: 'Solid work', tone: 'good' }
  return { label: 'Keep practising', tone: 'tried' }
})

// Per-option visual state.
type OptState = 'correct' | 'wrong' | 'missed' | 'skipped'
function optState(o: FinishedOptionDto): OptState {
  if (o.correct && o.selected) return 'correct'
  if (!o.correct && o.selected) return 'wrong'
  if (o.correct && !o.selected) return 'missed'
  return 'skipped'
}
const optMeta: Record<OptState, { label: string; symbol: string }> = {
  correct: { label: 'Correct', symbol: '✓' },
  wrong: { label: 'Wrong', symbol: '✗' },
  missed: { label: 'Missed', symbol: '–' },
  skipped: { label: 'Distractor', symbol: '·' },
}

// Show the celebration once when arriving from a fresh finish (?just=1).
// Strip the flag from the URL after capturing it so a refresh doesn't replay
// it and the user can share/bookmark the result URL cleanly.
const justFinished = ref(route.query.just === '1')
if (justFinished.value) {
  router.replace({ path: route.path, query: { ...route.query, just: undefined } })
}
const showCelebration = ref(false)
watch([justFinished, attempt], ([just, a]) => {
  if (just && a && !showCelebration.value) showCelebration.value = true
}, { immediate: true })
</script>

<template>
  <div v-if="isPending" class="empty body-md">Loading…</div>
  <Card v-else-if="!attempt" class="notfound">
    <h1 class="headline-md">Result not found</h1>
    <p class="body-md muted">This attempt doesn't exist, isn't finished yet, or you don't have access to it.</p>
    <Button @click="router.push('/app')">Back to browse</Button>
  </Card>
  <template v-else>
    <Card :class="['hero', `hero--${verdict.tone}`]">
      <div class="hero__main">
        <span class="label-sm muted">Result</span>
        <h1 class="headline-md hero__title">{{ attempt.quiz?.name ?? 'Untitled quiz' }}</h1>
        <p class="hero__verdict">{{ verdict.label }}</p>
      </div>
      <div class="hero__score">
        <div
          class="hero__ring"
          :style="{
            background: `conic-gradient(var(--ring-color) ${percent * 360}deg, var(--surface-container-high) 0)`,
          }"
        >
          <div class="hero__ring-inner">
            <span class="hero__percent">{{ percentLabel }}</span>
            <span class="hero__points label-sm muted">
              {{ attempt.score ?? 0 }} / {{ attempt.maximumScore ?? 0 }} pts
            </span>
          </div>
        </div>
      </div>
      <div class="hero__action">
        <Button variant="ghost" @click="router.push('/app')">Back to browse</Button>
      </div>
    </Card>

    <ol class="qlist">
      <li v-for="(q, i) in attempt.questions ?? []" :key="q.id">
        <Card>
          <div class="qhead">
            <div class="qhead__title">
              <span class="label-sm muted">Question {{ i + 1 }} of {{ attempt.questions?.length ?? 0 }}</span>
              <p class="body-lg q-text">{{ q.text }}</p>
            </div>
            <div
              :class="[
                'qhead__score',
                {
                  'qhead__score--full': questionScore(q).earned === questionScore(q).max,
                  'qhead__score--zero': questionScore(q).earned === 0,
                },
              ]"
            >
              <span class="label-sm">Score</span>
              <span class="qhead__score-value">
                {{ questionScore(q).earned }} <span class="muted">/ {{ questionScore(q).max }}</span>
              </span>
            </div>
          </div>
          <ul class="opts">
            <li
              v-for="o in q.options ?? []"
              :key="o.id"
              :class="['opt', `opt--${optState(o)}`]"
            >
              <span class="opt__icon" aria-hidden="true">{{ optMeta[optState(o)].symbol }}</span>
              <span class="opt__text">{{ o.text }}</span>
              <span class="opt__chip label-sm">{{ optMeta[optState(o)].label }}</span>
            </li>
          </ul>
        </Card>
      </li>
    </ol>

    <FinishCelebration
      v-if="showCelebration"
      :score="attempt.score ?? 0"
      :max="attempt.maximumScore ?? 0"
      :quiz-name="attempt.quiz?.name"
      @close="showCelebration = false"
    />
  </template>
</template>

<style scoped>
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

/* Hero */
.hero {
  --ring-color: var(--outline);
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: var(--space-lg);
  margin-bottom: var(--space-xl);
}
.hero--great { --ring-color: var(--on-secondary-container); }
.hero--good  { --ring-color: var(--on-primary-container); }
.hero--tried { --ring-color: var(--outline); }
.hero__main {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.hero__title {
  margin: 0;
}
.hero__verdict {
  margin: var(--space-xs) 0 0;
  font-weight: 700;
  font-size: 1.25rem;
  color: var(--on-surface);
}
.hero--great .hero__verdict { color: var(--on-secondary-container); }
.hero--good  .hero__verdict { color: var(--on-primary-container); }

.hero__score {
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero__ring {
  position: relative;
  width: 112px;
  height: 112px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero__ring-inner {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: var(--surface-container);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
}
.hero__percent {
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.01em;
  font-variant-numeric: tabular-nums;
  color: var(--on-surface);
}
.hero__points {
  font-variant-numeric: tabular-nums;
}

@media (max-width: 640px) {
  .hero {
    grid-template-columns: 1fr;
    text-align: left;
  }
  .hero__score { justify-content: flex-start; }
  .hero__action { justify-self: flex-start; }
}

/* Question list */
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
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}
.qhead__title {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.q-text {
  margin: 0;
}
.qhead__score {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  background: var(--surface-container-high);
  color: var(--on-surface-variant);
  white-space: nowrap;
}
.qhead__score--full {
  background: var(--secondary-container);
  color: var(--on-secondary-container);
}
.qhead__score--zero {
  background: var(--error-container);
  color: var(--on-error-container);
}
.qhead__score-value {
  font-size: 1.25rem;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: inherit;
}
.qhead__score--full .qhead__score-value .muted,
.qhead__score--zero .qhead__score-value .muted {
  color: inherit;
  opacity: 0.7;
}

/* Options */
.opts {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.opt {
  --opt-bg: var(--surface-container-low);
  --opt-border: var(--outline-variant);
  --opt-fg: var(--on-surface);
  --opt-accent: var(--on-surface-variant);

  display: grid;
  grid-template-columns: 28px 1fr auto;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md);
  background: var(--opt-bg);
  border: 1px solid var(--opt-border);
  border-left-width: 4px;
  border-radius: var(--radius);
  color: var(--opt-fg);
}
.opt__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--opt-accent);
  color: var(--opt-bg);
  font-weight: 800;
  font-size: 0.95rem;
  line-height: 1;
}
.opt__text {
  font-size: 1rem;
}
.opt__chip {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--opt-accent);
  color: var(--opt-bg);
  font-weight: 700;
  letter-spacing: 0.02em;
  text-transform: uppercase;
  font-size: 0.7rem;
}

.opt--correct {
  --opt-bg: color-mix(in srgb, var(--secondary-container) 35%, var(--surface-container-low));
  --opt-border: var(--on-secondary-container);
  --opt-fg: var(--on-surface);
  --opt-accent: var(--on-secondary-container);
}
.opt--wrong {
  --opt-bg: color-mix(in srgb, var(--error-container) 28%, var(--surface-container-low));
  --opt-border: var(--on-error-container);
  --opt-accent: var(--on-error-container);
}
.opt--missed {
  --opt-bg: var(--surface-container-low);
  --opt-border: var(--on-secondary-container);
  --opt-accent: var(--on-secondary-container);
  border-left-style: dashed;
}
.opt--skipped {
  --opt-bg: var(--surface-container-low);
  --opt-border: var(--outline-variant);
  --opt-accent: var(--on-surface-variant);
  opacity: 0.7;
}
</style>
