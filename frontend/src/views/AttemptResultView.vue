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
  if (q.type === 'SINGLE_CHOICE') {
    const picked = opts.find((o) => o.selected)
    return { earned: picked?.correct ? 1 : 0, max: 1 }
  }
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
const tone = computed<'great' | 'good' | 'tried'>(() => {
  const p = percent.value
  if (p >= 0.85) return 'great'
  if (p >= 0.5) return 'good'
  return 'tried'
})

// Per-option visual state. For multi-choice this is the same as before. For
// single-choice the question is binary at the question level, so:
//   - the picked option is the one that earns or costs the point ('correct'/'wrong')
//   - every other option is neutral ('skipped'), even the correct one if the
//     user picked something else — single-choice doesn't penalise non-picks.
type OptState = 'correct' | 'wrong' | 'missed' | 'skipped'
function optState(o: FinishedOptionDto, q: FinishedQuestionDto): OptState {
  if (q.type === 'SINGLE_CHOICE') {
    if (!o.selected) return 'skipped'
    return o.correct ? 'correct' : 'wrong'
  }
  if (o.correct && o.selected) return 'correct'
  if (!o.correct && o.selected) return 'wrong'
  if (o.correct && !o.selected) return 'missed'
  return 'skipped'
}
// The chip just says "+1 you got the point" (green) or "0 you didn't" (red).
// Skipped options on single-choice are neutral (no chip shown).
const optMeta: Record<OptState, { score: '+1' | '0' | null; tone: 'win' | 'lose' | 'neutral' }> = {
  correct: { score: '+1', tone: 'win' },
  skipped: { score: '+1', tone: 'win' },
  wrong:   { score: '0',  tone: 'lose' },
  missed:  { score: '0',  tone: 'lose' },
}
function chipFor(o: FinishedOptionDto, q: FinishedQuestionDto): { score: '+1' | '0' | null; tone: 'win' | 'lose' | 'neutral' } {
  const state = optState(o, q)
  if (q.type === 'SINGLE_CHOICE' && state === 'skipped') {
    // Don't show "+1 for skipping" on every non-picked option — the question is
    // worth 1 point total and only the picked option drives it.
    return { score: null, tone: 'neutral' }
  }
  return optMeta[state]
}
// Stable ordering: things you got right first, then things you got wrong.
const sortRank: Record<OptState, number> = { correct: 0, skipped: 1, missed: 2, wrong: 3 }
function sortedOptions(q: FinishedQuestionDto): FinishedOptionDto[] {
  return [...(q.options ?? [])].sort((a, b) => sortRank[optState(a, q)] - sortRank[optState(b, q)])
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
    <Card :class="['hero', `hero--${tone}`]">
      <div class="hero__main">
        <span class="label-sm hero__eyebrow">Result</span>
        <h1 class="hero__title">{{ attempt.quiz?.name ?? 'Untitled quiz' }}</h1>
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
            :class="['opt', `opt--${optState(o, q)}`, `opt--${chipFor(o, q).tone}`, { 'opt--picked': o.selected }]"
          >
            <span class="opt__pick" :aria-label="o.selected ? 'You picked this' : 'You did not pick this'">
              <span class="opt__pick-box">
                <span v-if="o.selected" class="opt__pick-tick">✓</span>
              </span>
            </span>
            <span class="opt__text">{{ o.text }}</span>
            <span v-if="chipFor(o, q).score !== null" class="opt__chip">{{ chipFor(o, q).score }}</span>
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
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: var(--space-xl);
  margin-bottom: var(--space-md);
  padding: var(--space-xl);
}
.hero--great { --ring-color: var(--on-secondary-container); }
.hero--good  { --ring-color: var(--on-surface); }
.hero--tried { --ring-color: var(--on-error-container); }

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
  font-size: 1.75rem;
  font-weight: 800;
  letter-spacing: -0.01em;
  color: var(--on-surface);
}
.hero__points {
  margin: var(--space-md) 0 0;
  color: var(--on-surface-variant);
  font-variant-numeric: tabular-nums;
  font-size: 1rem;
}

.hero__score {
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero__ring {
  position: relative;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero__ring-inner {
  width: 184px;
  height: 184px;
  border-radius: 50%;
  background: var(--surface-container);
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero__percent {
  font-size: 3.5rem;
  font-weight: 800;
  letter-spacing: -0.03em;
  font-variant-numeric: tabular-nums;
  color: var(--on-surface);
  line-height: 1;
}

@media (max-width: 640px) {
  .hero {
    grid-template-columns: 1fr;
    gap: var(--space-lg);
    text-align: left;
  }
  .hero__score { justify-content: flex-start; }
  .hero__ring { width: 180px; height: 180px; }
  .hero__ring-inner { width: 148px; height: 148px; }
  .hero__percent { font-size: 2.75rem; }
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

/* The checkbox communicates what the user did, independent of correctness. */
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
  color: #000;
  font-weight: 800;
  font-size: 0.8rem;
  line-height: 1;
}

.opt__text {
  font-size: 0.975rem;
  color: var(--opt-fg);
}

/* The chip communicates the point outcome only. +1 / 0 — that's it. */
.opt__chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--opt-accent);
  color: #000;
  font-weight: 800;
  font-size: 0.85rem;
  font-variant-numeric: tabular-nums;
}

/* Outcome tones drive the chip + surface tint. */
.opt--win {
  --opt-bg: color-mix(in srgb, var(--on-secondary-container) 12%, var(--surface-container-low));
  --opt-border: color-mix(in srgb, var(--on-secondary-container) 40%, transparent);
  --opt-accent: var(--on-secondary-container);
}
.opt--lose {
  --opt-bg: color-mix(in srgb, var(--on-error-container) 12%, var(--surface-container-low));
  --opt-border: color-mix(in srgb, var(--on-error-container) 45%, transparent);
  --opt-accent: var(--on-error-container);
}
.opt--neutral {
  /* Used for single-choice options the user didn't pick — no score signal. */
  --opt-bg: var(--surface-container-low);
  --opt-border: var(--outline-variant);
  --opt-accent: var(--on-surface-variant);
}

/* Skipped distractors are technically wins but visually quieter so the
   reader's eye is drawn to picks and mistakes first. */
.opt--skipped {
  --opt-bg: var(--surface-container-low);
  --opt-border: var(--outline-variant);
}
.opt--skipped .opt__chip {
  background: transparent;
  color: var(--on-secondary-container);
  border: 1px solid color-mix(in srgb, var(--on-secondary-container) 40%, transparent);
}
</style>
