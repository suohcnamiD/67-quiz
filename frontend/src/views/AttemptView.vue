<script setup lang="ts">
import { ref, computed, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGetAttemptsInProgress, useGetFinishedAttempts, commitAttemptActions, finishAttempt, getGetAttemptsInProgressQueryKey, getGetFinishedAttemptsQueryKey } from '@/api/attempt-controller/attempt-controller'
import { useQueryClient } from '@tanstack/vue-query'
import { errorMessage, firstErrorCode } from '@/lib/errors'
import { confirmDialog } from '@/lib/confirmDialog'
import { questionImageUrl, optionImageUrl } from '@/lib/quizImages'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import ProgressBar from '@/components/ProgressBar.vue'

const route = useRoute()
const router = useRouter()
const qc = useQueryClient()
const attemptId = computed(() => route.params.attemptId as string)

const { data, isPending } = useGetAttemptsInProgress({ page: 0 })
const finished = useGetFinishedAttempts({ page: 0 })
const attempt = computed(() =>
  (data.value?._embedded?.attempts ?? []).find((a) => a.id === attemptId.value),
)
const isFinished = computed(() =>
  (finished.data.value?._embedded?.attempts ?? []).some((a) => a.id === attemptId.value),
)
// Only show the loading state on initial fetch — background refetches keep the
// previous data on screen so it doesn't flash to "Loading…" after every commit.
const settled = computed(() => !isPending.value && !finished.isPending.value)

const now = ref(Date.now())
const ticker = setInterval(() => (now.value = Date.now()), 1000)
onUnmounted(() => clearInterval(ticker))

const remainingMs = computed(() => {
  if (!attempt.value?.finishDeadline) return 0
  return Math.max(0, new Date(attempt.value.finishDeadline).getTime() - now.value)
})
const totalMs = computed(() => {
  if (!attempt.value?.startedAt || !attempt.value?.finishDeadline) return 1
  return new Date(attempt.value.finishDeadline).getTime() - new Date(attempt.value.startedAt).getTime()
})
const remainingPct = computed(() => (totalMs.value > 0 ? remainingMs.value / totalMs.value : 0))

function fmtRemaining(ms: number): string {
  const s = Math.floor(ms / 1000)
  const mm = Math.floor(s / 60)
  const ss = s % 60
  return `${String(mm).padStart(2, '0')}:${String(ss).padStart(2, '0')}`
}

const totalQuestions = computed(() => attempt.value?.questions?.length ?? 0)

const errorText = ref<string | null>(null)
const togglingKey = ref<string | null>(null)
const pulseKey = ref<string | null>(null)
async function toggleOption(
  questionId: string | undefined,
  optionId: string | undefined,
  currentlySelected: boolean | undefined,
  questionType: 'SINGLE_CHOICE' | 'MULTI_CHOICE' | undefined,
) {
  if (!questionId || !optionId) return
  if (remainingMs.value <= 0) return
  const isSingle = questionType === 'SINGLE_CHOICE'
  const key = `${questionId}:${optionId}`
  // Single-choice clicks on the already-picked option are a no-op — once you've
  // picked, you can only switch, not unselect. Pulse the button so the click
  // doesn't feel ignored.
  if (isSingle && currentlySelected) {
    pulseKey.value = key
    setTimeout(() => {
      if (pulseKey.value === key) pulseKey.value = null
    }, 280)
    return
  }
  const next = isSingle ? true : !currentlySelected
  togglingKey.value = key
  errorText.value = null

  // Optimistic cache update — keeps the UI stable across the commit round-trip.
  // For single-choice, set the picked option to selected and clear all siblings;
  // the backend does the same when it processes the action.
  const queryKey = getGetAttemptsInProgressQueryKey({ page: 0 })
  const previous = qc.getQueryData<typeof data.value>(queryKey)
  qc.setQueryData<typeof data.value>(queryKey, (old) => {
    if (!old?._embedded?.attempts) return old
    return {
      ...old,
      _embedded: {
        ...old._embedded,
        attempts: old._embedded.attempts.map((a) =>
          a.id !== attemptId.value
            ? a
            : {
                ...a,
                questions: (a.questions ?? []).map((q) =>
                  q.id !== questionId
                    ? q
                    : {
                        ...q,
                        options: (q.options ?? []).map((o) => {
                          if (isSingle) return { ...o, selected: o.id === optionId }
                          return o.id !== optionId ? o : { ...o, selected: next }
                        }),
                      },
                ),
              },
        ),
      },
    }
  })

  try {
    await commitAttemptActions({
      attemptId: attemptId.value,
      actions: [{ questionId, optionId, selected: next }],
    })
  } catch (e) {
    // Roll back the optimistic update.
    qc.setQueryData(queryKey, previous)
    errorText.value = errorMessage(e)
  } finally {
    togglingKey.value = null
  }
}

const finishing = ref(false)
// Auto-finish lifecycle when the timer hits zero. We track this separately
// from `finishing` so a failed auto-attempt leaves a Retry banner without
// stranding the user.
type AutoFinishState = 'idle' | 'attempting' | 'succeeded' | 'failed'
const autoFinishState = ref<AutoFinishState>('idle')
const autoFinished = computed(() => autoFinishState.value !== 'idle')
async function finishCore(viaAuto = false) {
  finishing.value = true
  errorText.value = null
  try {
    await finishAttempt({ attemptId: attemptId.value })
  } catch (e) {
    // If the backend already auto-finished this attempt (deadline passed),
    // treat the call as a no-op and still navigate to the result.
    if (firstErrorCode(e) !== 'ATTEMPT_ALREADY_FINISHED') {
      errorText.value = errorMessage(e)
      finishing.value = false
      if (viaAuto) autoFinishState.value = 'failed'
      return
    }
  }
  if (viaAuto) autoFinishState.value = 'succeeded'
  // Refetch the finished list BEFORE we navigate so ResultView finds the
  // just-finished attempt in cache — otherwise it renders "Result not
  // found" while the background fetch runs (Firefox tends to abort our
  // post-nav refetch as NS_BINDING_ABORTED when ResultView starts its
  // own query).
  //
  // Deliberately do NOT invalidate the in-progress query yet. Invalidating
  // it here triggers a refetch that drops this attempt from the cache,
  // which makes AttemptView.attempt become undefined and the template
  // flips to the "Loading…" fallback for a frame before router.push
  // completes. Instead, evict the stale entry from the in-progress cache
  // AFTER navigation so no re-render of AttemptView is triggered while
  // we're still on the page.
  await qc.refetchQueries({ queryKey: getGetFinishedAttemptsQueryKey() })
  router.push({ path: `/app/attempt/${attemptId.value}/result`, query: { just: '1' } })
  qc.invalidateQueries({ queryKey: getGetAttemptsInProgressQueryKey() })
  finishing.value = false
}
async function finish() {
  const ok = await confirmDialog.open({
    title: 'Finish this attempt?',
    body: "You won't be able to change answers after finishing.",
    confirmLabel: 'Finish',
  })
  if (!ok) return
  await finishCore()
}
async function retryAutoFinish() {
  autoFinishState.value = 'attempting'
  await finishCore(true)
}

watch([attempt, isFinished, settled], ([a, fin, s]) => {
  if (s && !a && fin) {
    router.replace(`/app/attempt/${attemptId.value}/result`)
  }
})

// When the timer runs out, finish on the user's behalf and bounce to result.
// If the auto-attempt fails, show a Retry banner instead of stranding the user.
watch(remainingMs, (ms) => {
  if (ms > 0) return
  if (autoFinishState.value !== 'idle' || finishing.value) return
  if (!attempt.value) return
  autoFinishState.value = 'attempting'
  void finishCore(true)
})
</script>

<template>
  <div v-if="!settled" class="empty body-md">Loading…</div>
  <template v-else-if="attempt">
    <div class="bar">
      <ProgressBar :value="remainingPct" />
    </div>
    <header class="head">
      <div class="meta-stack title-stack">
        <span class="label-sm muted">Attempting</span>
        <h1 class="headline-md">{{ attempt.quiz?.name ?? 'Untitled quiz' }}</h1>
      </div>
      <div class="meta-stack right">
        <span class="label-sm muted">Time remaining</span>
        <span class="headline-lg time">{{ fmtRemaining(remainingMs) }}</span>
      </div>
      <Button :loading="finishing" @click="finish">Finish attempt</Button>
    </header>

    <p v-if="errorText" class="banner label-md">{{ errorText }}</p>

    <div v-if="autoFinishState === 'failed'" class="banner banner--retry" role="alert">
      <span>Time's up, but we couldn't finish your attempt automatically.</span>
      <Button variant="primary" :loading="finishing" @click="retryAutoFinish">Retry finish</Button>
    </div>

    <ol class="qlist">
      <li v-for="(q, i) in attempt.questions ?? []" :key="q.id">
        <Card>
          <div class="qhead">
            <span class="label-sm muted">Question {{ i + 1 }} / {{ totalQuestions }}</span>
            <span class="label-sm muted qhead__rule">
              {{ q.type === 'SINGLE_CHOICE' ? 'Single answer' : 'Select all that apply' }}
            </span>
          </div>
          <p class="body-lg q-text">{{ q.text }}</p>
          <img
            v-if="q.hasImage && q.questionId"
            :src="questionImageUrl(q.questionId)"
            alt=""
            class="q-image"
            loading="lazy"
          />
          <ul class="opts">
            <li v-for="o in q.options ?? []" :key="o.id">
              <button
                type="button"
                :class="[
                  'opt',
                  { 'opt--selected': o.selected },
                  q.type === 'SINGLE_CHOICE' ? 'opt--radio' : 'opt--checkbox',
                ]"
                :data-pulse="pulseKey === `${q.id}:${o.id}` ? 'true' : null"
                :disabled="togglingKey === `${q.id}:${o.id}`"
                @click="toggleOption(q.id, o.id, o.selected, q.type)"
              >
                <span class="opt__marker">
                  <span v-if="o.selected" class="opt__dot" />
                </span>
                <span class="opt__text">{{ o.text }}</span>
                <img
                  v-if="o.hasImage && o.id"
                  :src="optionImageUrl(o.id)"
                  alt=""
                  class="opt__image"
                  loading="lazy"
                />
              </button>
            </li>
          </ul>
        </Card>
      </li>
    </ol>
    <div v-if="remainingMs > 0" class="finish-foot">
      <Button :loading="finishing" @click="finish">Finish attempt</Button>
    </div>
  </template>
  <div v-else-if="finishing || autoFinished" class="empty body-md">Loading…</div>
  <Card v-else class="notfound">
    <h1 class="headline-md">Attempt not found</h1>
    <p class="body-md muted">This attempt doesn't exist or you don't have access to it.</p>
    <Button @click="router.push('/app')">Back to browse</Button>
  </Card>
</template>

<style scoped>
.bar {
  position: sticky;
  top: 0;
  margin: calc(-1 * var(--space-xl)) calc(-1 * var(--margin-desktop)) var(--space-lg);
  z-index: 5;
}
.head {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  margin-bottom: var(--space-xl);
}
.meta-stack {
  display: flex;
  flex-direction: column;
}
.meta-stack.right {
  margin-left: auto;
  text-align: right;
}
.title-stack h1 {
  margin: 0;
}
.title-stack {
  margin-right: var(--space-lg);
}
.muted {
  color: var(--on-surface-variant);
}
.time {
  font-variant-numeric: tabular-nums;
}
.banner {
  margin: 0 0 var(--space-lg);
  padding: var(--space-sm) var(--space-md);
  background: var(--error-container);
  color: var(--on-error-container);
  border-radius: var(--radius);
}
.banner--retry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  flex-wrap: wrap;
}
.qlist {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.finish-foot {
  margin-top: var(--space-xl);
  display: flex;
  justify-content: center;
}
.finish-foot :deep(button) {
  min-width: 16rem;
}
@media (max-width: 640px) {
  .finish-foot :deep(button) {
    width: 100%;
    min-width: 0;
  }
}
.qhead {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-sm);
}
.q-text {
  margin: 0 0 var(--space-md);
}
.q-image {
  display: block;
  max-width: 100%;
  max-height: 240px;
  width: auto;
  object-fit: contain;
  border-radius: var(--radius-lg);
  margin: 0 auto var(--space-lg);
}
.opt__image {
  display: block;
  max-width: 160px;
  max-height: 100px;
  width: auto;
  object-fit: contain;
  border-radius: var(--radius);
  margin-top: var(--space-xs);
}
.opts {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.opt {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  width: 100%;
  text-align: left;
  padding: var(--space-md);
  background: var(--surface-container-low);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius);
  color: var(--on-surface);
  cursor: pointer;
  transition: border-color 120ms ease, background-color 120ms ease;
}
.opt:hover:not(:disabled) {
  border-color: var(--outline);
}
.opt--selected {
  border-color: var(--primary-container);
  background: var(--surface-container);
}
.opt[data-pulse] {
  animation: opt-pulse 280ms ease;
}
@keyframes opt-pulse {
  0% { transform: scale(1); }
  40% { transform: scale(1.015); border-color: var(--primary-container); }
  100% { transform: scale(1); }
}
@media (prefers-reduced-motion: reduce) {
  .opt[data-pulse] { animation: none; }
}
.opt__marker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border: 1px solid var(--outline);
  border-radius: var(--radius);
  flex-shrink: 0;
}
/* Round marker for single-choice (radio) options. */
.opt--radio .opt__marker {
  border-radius: 50%;
}
.opt--radio .opt__dot {
  border-radius: 50%;
}
.opt--selected .opt__marker {
  border-color: var(--primary-container);
}
.opt__dot {
  width: 8px;
  height: 8px;
  background: var(--primary-container);
  border-radius: var(--radius-sm);
}
.opt__text {
  flex: 1;
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

/* Mobile: the three header pieces (title / time / Finish) overflow the
 * 375px row. Stack them vertically and let the Finish button become a
 * full-width tap target. The timer keeps tabular numerals so it doesn't
 * dance as digits change. */
@media (max-width: 640px) {
  .head {
    flex-wrap: wrap;
    gap: var(--space-sm);
    margin-bottom: var(--space-lg);
  }
  .meta-stack.right {
    margin-left: 0;
    text-align: left;
  }
  .title-stack {
    margin-right: 0;
    flex: 1 1 100%;
  }
  .head :deep(.btn) {
    width: 100%;
  }
}
</style>
