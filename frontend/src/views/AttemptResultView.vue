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

const percent = computed(() => {
  const max = attempt.value?.maximumScore ?? 0
  if (!max) return 0
  return (attempt.value!.score ?? 0) / max
})
const percentLabel = computed(() => `${Math.round(percent.value * 100)}%`)
const verdict = computed(() => {
  const p = percent.value
  if (p >= 0.85) return { label: 'Brilliant', tone: 'great' as const }
  if (p >= 0.5) return { label: 'Solid work', tone: 'good' as const }
  return { label: 'Keep practising', tone: 'tried' as const }
})

type OptState = 'correct' | 'wrong' | 'missed' | 'skipped'
function optState(o: FinishedOptionDto): OptState {
  if (o.correct && o.selected) return 'correct'
  if (!o.correct && o.selected) return 'wrong'
  if (o.correct && !o.selected) return 'missed'
  return 'skipped'
}
// Labels lead with what the *user* did, not what the option is — otherwise
// "Correct" and "Wrong" don't tell you whether you picked the option.
const optMeta: Record<OptState, { label: string; symbol: string }> = {
  correct: { label: 'You picked — correct',   symbol: '✓' },
  wrong:   { label: 'You picked — wrong',     symbol: '✗' },
  missed:  { label: 'You skipped — was correct', symbol: '!' },
  skipped: { label: 'You skipped',            symbol: '' },
}
// Stable ordering so the same question doesn't reshuffle between renders:
// correct picks first, then missed, then wrong picks, then skipped.
const sortRank: Record<OptState, number> = { correct: 0, missed: 1, wrong: 2, skipped: 3 }
function sortedOptions(q: FinishedQuestionDto): FinishedOptionDto[] {
  return [...(q.options ?? [])].sort((a, b) => sortRank[optState(a)] - sortRank[optState(b)])
}

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
        <span class="label-sm hero__eyebrow">Result</span>
        <h1 class="hero__title">{{ attempt.quiz?.name ?? 'Untitled quiz' }}</h1>
        <p class="hero__verdict">{{ verdict.label }}</p>
        <p class="hero__points label-md">
          {{ attempt.score ?? 0 }} of {{ attempt.maximumScore ?? 0 }} points
        </p>
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
          </div>
        </div>
      </div>
    </Card>

    <div class="actions">
      <Button variant="ghost" @click="router.push('/app')">Back to browse</Button>
    </div>

    <ol class="qlist">
      <li v-for="(q, i) in attempt.questions ?? []" :key="q.id" class="question">
        <header class="qhead">
          <div class="qhead__title">
            <span class="label-sm muted">Question {{ i + 1 }} of {{ attempt.questions?.length ?? 0 }}</span>
            <p class="q-text">{{ q.text }}</p>
          </div>
          <div
            :class="[
              'qhead__score',
              {
                'qhead__score--full': questionScore(q).earned === questionScore(q).max,
                'qhead__score--zero': questionScore(q).earned === 0,
              },
            ]"
            :title="`${questionScore(q).earned} of ${questionScore(q).max} points`"
          >
            <span class="qhead__score-value">{{ questionScore(q).earned }}</span>
            <span class="qhead__score-divider">/</span>
            <span class="qhead__score-max">{{ questionScore(q).max }}</span>
          </div>
        </header>
        <ul class="opts">
          <li
            v-for="o in sortedOptions(q)"
            :key="o.id"
            :class="['opt', `opt--${optState(o)}`, { 'opt--picked': o.selected }]"
          >
            <span class="opt__pick" :aria-label="o.selected ? 'You picked this' : 'You did not pick this'">
              <span class="opt__pick-box">
                <span v-if="o.selected" class="opt__pick-tick">✓</span>
              </span>
            </span>
            <span class="opt__text">{{ o.text }}</span>
            <span class="opt__chip">
              <span v-if="optMeta[optState(o)].symbol" class="opt__chip-symbol" aria-hidden="true">{{ optMeta[optState(o)].symbol }}</span>
              {{ optMeta[optState(o)].label }}
            </span>
          </li>
        </ul>
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

/* ----- Hero ----- */
.hero {
  --ring-color: var(--outline);
  --hero-accent: var(--on-surface);
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: var(--space-xl);
  margin-bottom: var(--space-md);
  padding: var(--space-xl);
}
.hero--great { --ring-color: var(--on-secondary-container); --hero-accent: var(--on-secondary-container); }
.hero--good  { --ring-color: var(--on-surface);             --hero-accent: var(--on-surface); }
.hero--tried { --ring-color: var(--on-error-container);     --hero-accent: var(--on-error-container); }

.hero__main {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  min-width: 0;
}
.hero__eyebrow {
  color: var(--on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
.hero__title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.01em;
  color: var(--on-surface);
}
.hero__verdict {
  margin: var(--space-sm) 0 0;
  font-weight: 800;
  font-size: 2rem;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--hero-accent);
}
.hero__points {
  margin: var(--space-sm) 0 0;
  color: var(--on-surface-variant);
  font-variant-numeric: tabular-nums;
}

.hero__score {
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero__ring {
  position: relative;
  width: 144px;
  height: 144px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero__ring-inner {
  width: 116px;
  height: 116px;
  border-radius: 50%;
  background: var(--surface-container);
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero__percent {
  font-size: 2.25rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
  color: var(--on-surface);
}

@media (max-width: 640px) {
  .hero { grid-template-columns: 1fr; gap: var(--space-lg); text-align: left; }
  .hero__score { justify-content: flex-start; }
}

/* ----- Floating top action ----- */
.actions {
  display: flex;
  justify-content: flex-end;
  margin: 0 0 var(--space-lg);
}

/* ----- Question list — flat, no nested cards ----- */
.qlist {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
.question {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.qhead {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md);
}
.qhead__title {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  min-width: 0;
}
.q-text {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--on-surface);
  line-height: 1.4;
}
.qhead__score {
  flex-shrink: 0;
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--surface-container-high);
  color: var(--on-surface);
  font-variant-numeric: tabular-nums;
  font-weight: 700;
}
.qhead__score--full {
  background: var(--secondary-container);
  color: var(--on-secondary-container);
}
.qhead__score--zero {
  background: var(--error-container);
  color: var(--on-error-container);
}
.qhead__score-value { font-size: 1rem; }
.qhead__score-divider { opacity: 0.5; }
.qhead__score-max { font-size: 0.875rem; opacity: 0.7; }

/* ----- Options ----- */
.opts {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.opt {
  --opt-bg: var(--surface-container-low);
  --opt-border: var(--outline-variant);
  --opt-accent: var(--on-surface-variant);
  --opt-fg: var(--on-surface);

  display: grid;
  grid-template-columns: 24px 1fr auto;
  align-items: center;
  gap: var(--space-md);
  padding: 12px var(--space-md);
  background: var(--opt-bg);
  border: 1px solid var(--opt-border);
  border-radius: var(--radius-md);
  color: var(--opt-fg);
}

/* Filled checkbox if the user picked this option, hollow otherwise.
   This is what tells the reader what *they* did, independent of correctness. */
.opt__pick {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.opt__pick-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 5px;
  border: 1.5px solid var(--on-surface-variant);
  background: transparent;
  transition: background-color 120ms, border-color 120ms;
}
.opt--picked .opt__pick-box {
  background: var(--opt-accent);
  border-color: var(--opt-accent);
}
.opt__pick-tick {
  color: var(--opt-bg);
  font-weight: 800;
  font-size: 0.8rem;
  line-height: 1;
}
.opt--picked.opt--correct .opt__pick-tick,
.opt--picked.opt--wrong .opt__pick-tick {
  color: #000;
}

.opt__text {
  font-size: 0.975rem;
  color: var(--opt-fg);
}

.opt__chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px 4px 8px;
  border-radius: 999px;
  background: var(--opt-accent);
  color: #000;
  font-weight: 700;
  letter-spacing: 0.02em;
  font-size: 0.75rem;
  white-space: nowrap;
}
.opt__chip-symbol {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.25);
  color: inherit;
  font-size: 0.7rem;
  font-weight: 800;
  line-height: 1;
}

.opt--correct {
  --opt-bg: color-mix(in srgb, var(--on-secondary-container) 14%, var(--surface-container-low));
  --opt-border: color-mix(in srgb, var(--on-secondary-container) 50%, transparent);
  --opt-accent: var(--on-secondary-container);
}
.opt--wrong {
  --opt-bg: color-mix(in srgb, var(--on-error-container) 12%, var(--surface-container-low));
  --opt-border: color-mix(in srgb, var(--on-error-container) 50%, transparent);
  --opt-accent: var(--on-error-container);
}
.opt--missed {
  --opt-bg: var(--surface-container-low);
  --opt-border: color-mix(in srgb, var(--on-secondary-container) 35%, transparent);
  --opt-accent: var(--on-secondary-container);
}
.opt--skipped {
  --opt-bg: transparent;
  --opt-border: var(--outline-variant);
  --opt-accent: var(--on-surface-variant);
  --opt-fg: var(--on-surface-variant);
}
.opt--skipped .opt__chip {
  background: transparent;
  border: 1px solid var(--outline-variant);
  color: var(--on-surface-variant);
}
.opt--skipped .opt__chip-symbol {
  display: none;
}
</style>
