<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'
import { useGetFinishedAttempts, attemptQuiz } from '@/api/attempt-controller/attempt-controller'
import {
  useGetMine,
  useUpsertMine,
  getGetMineQueryKey,
  getRatingSummaryQueryKey,
} from '@/api/quiz-rating-controller/quiz-rating-controller'
import { getGetQuizzesQueryKey } from '@/api/quiz-controller/quiz-controller'
import { errorMessage, firstErrorCode } from '@/lib/errors'
import { questionImageUrl, optionImageUrl } from '@/lib/quizImages'
import { dhbwGrade, formatGrade } from '@/lib/dhbwGrade'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import FinishCelebration from '@/components/FinishCelebration.vue'
import type { FinishedQuestionDto, FinishedOptionDto } from '@/api/openAPIDefinition.schemas'

const route = useRoute()
const router = useRouter()
const qc = useQueryClient()
const attemptId = computed(() => route.params.attemptId as string)

const { data, isPending, isFetching } = useGetFinishedAttempts({ page: 0 })
const attempt = computed(() =>
  (data.value?._embedded?.attempts ?? []).find((a) => a.id === attemptId.value),
)
// If the finished list is cached (user visited past results before), isPending
// is false immediately even though the just-finished attempt hasn't landed
// yet. Show the loading state until the background refetch resolves so we
// don't briefly render "Result not found" between the finish call and the
// invalidation completing.
const stillFetching = computed(() => isPending.value || (isFetching.value && !attempt.value))
const quizId = computed(() => attempt.value?.quiz?.id)

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
// German DHBW-Wirtschaft note (1.0 great … 5.0 fail). Displayed alongside
// the raw percent so students used to the German grading scale get an
// immediate read on the result.
const grade = computed(() => dhbwGrade(percent.value * 100))
const gradeLabel = computed(() => formatGrade(grade.value))
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
    if (o.selected) return o.correct ? 'correct' : 'wrong'
    // Unpicked: highlight the correct one as "missed" so the user can see
    // which option was the right answer when they picked wrong; the rest
    // stay neutral "skipped".
    return o.correct ? 'missed' : 'skipped'
  }
  if (o.correct && o.selected) return 'correct'
  if (!o.correct && o.selected) return 'wrong'
  if (o.correct && !o.selected) return 'missed'
  return 'skipped'
}

/**
 * Chip rendered only on options the user actually picked. The chip shows
 * whether that pick earned a point or not. Unpicked options don't get a
 * chip — the question header's earned/max score does that math.
 */
function pickedChip(o: FinishedOptionDto, q: FinishedQuestionDto): '+1' | '0' | null {
  if (!o.selected) return null
  const state = optState(o, q)
  return state === 'correct' ? '+1' : '0'
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

const tryAgainPending = ref(false)
const tryAgainError = ref<string | null>(null)
async function tryAgain() {
  const quizId = attempt.value?.quiz?.id
  if (!quizId) return
  tryAgainPending.value = true
  tryAgainError.value = null
  try {
    const fresh = await attemptQuiz({ quizId })
    router.push(`/app/attempt/${fresh.id}`)
  } catch (e) {
    tryAgainError.value = errorMessage(e)
  } finally {
    tryAgainPending.value = false
  }
}

// ----- Rating widget ---------------------------------------------------
// We fetch the user's existing rating (if any) and let them upsert. The
// widget is dismissible per-quiz via localStorage so re-visits don't nag.
const ratingEnabled = computed(() => !!quizId.value)
const myRatingQuery = useGetMine(
  computed(() => quizId.value ?? ''),
  { query: { enabled: ratingEnabled } },
)
// Backend returns 204 No Content when there's no rating yet; vue-query then
// resolves data to undefined/null. Treat null/undefined as "no rating yet".
const myRating = computed(() => myRatingQuery.data.value ?? null)

// Dismiss is session-only: closing the widget once should NOT hide it
// forever on future visits. Users kept coming back to a result page for a
// quiz they'd dismissed and thought the rating feature was broken. Keep
// the flag in-memory (dismissed ref); no localStorage.
const dismissed = ref(false)
watch(quizId, () => { dismissed.value = false })

const ratingScore = ref<number | null>(null)
const ratingHover = ref<number | null>(null)
const ratingComment = ref('')
const ratingSaving = ref(false)
const ratingError = ref<string | null>(null)
const ratingSaved = ref(false)

watch(myRating, (r) => {
  if (r) {
    ratingScore.value = r.score ?? null
    ratingComment.value = r.comment ?? ''
  }
}, { immediate: true })

const showRatingWidget = computed(() => {
  if (!quizId.value) return false
  // Always show once the user has rated (so they can see + update it).
  if (myRating.value) return true
  return !dismissed.value
})

const upsertRating = useUpsertMine()
async function saveRating() {
  if (!quizId.value || ratingScore.value == null) return
  ratingSaving.value = true
  ratingError.value = null
  try {
    await upsertRating.mutateAsync({
      quizId: quizId.value,
      data: { score: ratingScore.value, comment: ratingComment.value || undefined },
    })
    ratingSaved.value = true
    qc.invalidateQueries({ queryKey: getGetMineQueryKey(quizId.value) })
    qc.invalidateQueries({ queryKey: getRatingSummaryQueryKey(quizId.value) })
    qc.invalidateQueries({ queryKey: getGetQuizzesQueryKey() })
    // No dismissal on save: the widget flips to "Your rating" mode and
    // becomes an update form, which is more useful than hiding it.
  } catch (e) {
    if (firstErrorCode(e) !== 'RATING_NOT_ELIGIBLE') console.error(e)
    ratingError.value = errorMessage(e)
  } finally {
    ratingSaving.value = false
  }
}

function dismissRating() {
  // Session-only dismiss. Refreshing the page brings the widget back so
  // the user can still rate later without hunting for a hidden action.
  dismissed.value = true
}
</script>

<template>
  <div v-if="stillFetching" class="empty body-md">Loading…</div>
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
        <p class="hero__grade" :title="'DHBW-Wirtschaft grade for this percentage'">
          <span class="hero__grade-value">{{ gradeLabel }}</span>
          <span class="hero__grade-label label-sm">DHBW-Note</span>
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
      <Button
        v-if="attempt.quiz?.id"
        :loading="tryAgainPending"
        @click="tryAgain"
      >Try again</Button>
      <Button variant="ghost" @click="router.push('/app')">Back to browse</Button>
    </div>
    <p v-if="tryAgainError" class="banner label-md" role="alert">{{ tryAgainError }}</p>

    <Card v-if="showRatingWidget" class="rate" aria-labelledby="rate-heading">
      <div class="rate__head">
        <h2 id="rate-heading" class="headline-md rate__title">
          {{ myRating ? 'Your rating' : 'Rate this quiz' }}
        </h2>
        <button
          v-if="!myRating"
          type="button"
          class="rate__dismiss label-sm"
          aria-label="Dismiss rating prompt"
          @click="dismissRating"
        >Maybe later</button>
      </div>
      <div
        class="rate__stars"
        role="radiogroup"
        aria-label="Score from 1 to 10"
        @mouseleave="ratingHover = null"
      >
        <label
          v-for="n in 10"
          :key="n"
          :class="[
            'rate__star',
            { 'rate__star--filled': (ratingHover ?? ratingScore ?? 0) >= n },
            { 'rate__star--preview': ratingHover != null && ratingHover >= n && (ratingScore ?? 0) < n },
          ]"
          @mouseenter="ratingHover = n"
        >
          <input
            type="radio"
            name="rating-score"
            class="visually-hidden"
            :value="n"
            :checked="ratingScore === n"
            :disabled="ratingSaving"
            @change="ratingScore = n"
          />
          <span aria-hidden="true">★</span>
          <span class="visually-hidden">{{ n }} out of 10</span>
        </label>
      </div>
      <p class="rate__hint body-md muted" v-if="ratingScore != null">
        You picked {{ ratingScore }} / 10.
      </p>
      <textarea
        v-model="ratingComment"
        class="rate__comment"
        :disabled="ratingSaving"
        maxlength="500"
        rows="3"
        placeholder="Anything you want to say about this quiz? (optional)"
        aria-label="Optional comment"
      ></textarea>
      <p v-if="ratingError" class="banner label-md" role="alert">{{ ratingError }}</p>
      <div class="rate__actions">
        <Button
          :loading="ratingSaving"
          :disabled="ratingScore == null"
          @click="saveRating"
        >{{ myRating ? 'Update rating' : 'Submit rating' }}</Button>
        <span v-if="ratingSaved && !ratingError" class="rate__saved body-md">Saved.</span>
      </div>
    </Card>

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
        <img
          v-if="q.hasImage && q.id"
          :src="questionImageUrl(q.id)"
          alt=""
          class="q-image"
          loading="lazy"
        />
        <ul class="opts">
          <li
            v-for="o in sortedOptions(q)"
            :key="o.id"
            :class="['opt', `opt--${optState(o, q)}`, { 'opt--picked': o.selected }]"
          >
            <span class="opt__pick" :aria-label="o.selected ? 'You picked this' : 'You did not pick this'">
              <span class="opt__pick-box">
                <span v-if="o.selected" class="opt__pick-tick">✓</span>
              </span>
            </span>
            <span class="opt__text">{{ o.text }}</span>
            <img
              v-if="o.hasImage && o.id"
              :src="optionImageUrl(o.id)"
              alt=""
              class="opt__image"
              loading="lazy"
            />
            <span v-if="pickedChip(o, q)" class="opt__chip">{{ pickedChip(o, q) }}</span>
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
.hero__grade {
  margin: var(--space-sm) 0 0;
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  color: var(--on-surface-variant);
  font-variant-numeric: tabular-nums;
}
.hero__grade-value {
  font-size: 1rem;
  font-weight: 700;
  color: var(--on-surface);
  line-height: 1;
}
.hero__grade-label {
  font-size: 0.75rem;
  color: var(--on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.05em;
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
  gap: var(--space-sm);
  margin: 0 0 var(--space-lg);
}
.banner {
  margin: 0 0 var(--space-lg);
  padding: var(--space-sm) var(--space-md);
  background: var(--error-container);
  color: var(--on-error-container);
  border-radius: var(--radius);
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

.q-image {
  display: block;
  max-width: 100%;
  max-height: 320px;
  border-radius: var(--radius-lg);
  margin: 0 0 var(--space-md);
}
.opt__image {
  grid-column: 2 / -1;
  grid-row: 2;
  justify-self: start;
  max-width: 100%;
  max-height: 140px;
  border-radius: var(--radius);
  margin-top: var(--space-xs);
}

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
  grid-column: 3;
  grid-row: 1;
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

/* Palette by option state. Green only when the user actually picked the
 * right answer; red only when they picked a wrong one. Everything else
 * stays neutral so the eye lands on what the user did, not what they
 * could've done. */
.opt--correct {
  --opt-bg: color-mix(in srgb, var(--on-secondary-container) 14%, var(--surface-container-low));
  --opt-border: color-mix(in srgb, var(--on-secondary-container) 45%, transparent);
  --opt-accent: var(--on-secondary-container);
}
.opt--wrong {
  --opt-bg: color-mix(in srgb, var(--on-error-container) 14%, var(--surface-container-low));
  --opt-border: color-mix(in srgb, var(--on-error-container) 50%, transparent);
  --opt-accent: var(--on-error-container);
}
/* The right answer the user didn't pick — neutral fill so it doesn't
 * compete with the user's actual correct picks, but a dashed accent
 * border marks it as "this is what you should've picked". */
.opt--missed {
  --opt-border: color-mix(in srgb, var(--on-secondary-container) 55%, transparent);
  border-style: dashed;
}
/* Skipped wrong distractors: plain neutral, no signal. */
.opt--skipped { /* defaults */ }

/* ----- Rating widget ----- */
.rate {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  margin: 0 0 var(--space-lg);
}
.rate__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-md);
}
.rate__title {
  margin: 0;
}
.rate__dismiss {
  appearance: none;
  background: transparent;
  border: 0;
  color: var(--on-surface-variant);
  font: inherit;
  text-decoration: underline;
  cursor: pointer;
  padding: 0;
}
.rate__dismiss:hover { color: var(--on-surface); }
.rate__stars {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 4px 8px;
  border-radius: var(--radius-lg);
  background: var(--surface-container-lowest);
  border: 1px solid var(--outline-variant);
  width: fit-content;
  max-width: 100%;
  flex-wrap: wrap;
}
.rate__star {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 40px;
  font-size: 1.5rem;
  line-height: 1;
  color: var(--on-surface-variant);
  cursor: pointer;
  transition: color 120ms ease, transform 120ms ease;
  user-select: none;
}
.rate__star:hover { transform: scale(1.08); }
.rate__star--filled { color: var(--primary-container); }
.rate__star--preview { color: color-mix(in srgb, var(--primary-container) 65%, transparent); }
.rate__star input:focus-visible + span {
  outline: 2px solid var(--primary-container);
  outline-offset: 2px;
  border-radius: 4px;
}
.rate__hint {
  margin: 0;
}
.rate__comment {
  width: 100%;
  background: var(--surface-container-lowest);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius);
  color: var(--on-surface);
  padding: 10px 12px;
  font: inherit;
  resize: vertical;
  min-height: 72px;
}
.rate__comment:focus {
  outline: none;
  border-color: var(--primary-container);
}
.rate__actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}
.rate__saved {
  color: var(--on-secondary-container);
}
.visually-hidden {
  position: absolute !important;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
@media (max-width: 640px) {
  .rate__star {
    width: 28px;
    height: 36px;
    font-size: 1.35rem;
  }
}
</style>
