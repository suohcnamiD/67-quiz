<script setup lang="ts">
import { ref, computed, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGetAttemptsInProgress, useGetFinishedAttempts, commitAttemptActions, finishAttempt, getGetAttemptsInProgressQueryKey, getGetFinishedAttemptsQueryKey } from '@/api/attempt-controller/attempt-controller'
import { useQueryClient } from '@tanstack/vue-query'
import { errorMessage } from '@/lib/errors'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import ProgressBar from '@/components/ProgressBar.vue'

const route = useRoute()
const router = useRouter()
const qc = useQueryClient()
const attemptId = computed(() => route.params.attemptId as string)

const { data, isLoading, isFetching } = useGetAttemptsInProgress({ page: 0 })
const finished = useGetFinishedAttempts({ page: 0 })
const attempt = computed(() =>
  (data.value?._embedded?.attempts ?? []).find((a) => a.id === attemptId.value),
)
const isFinished = computed(() =>
  (finished.data.value?._embedded?.attempts ?? []).some((a) => a.id === attemptId.value),
)
const settled = computed(
  () => !isLoading.value && !isFetching.value && !finished.isLoading.value && !finished.isFetching.value,
)

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
async function toggleOption(questionId?: string, optionId?: string, currentlySelected?: boolean) {
  if (!questionId || !optionId) return
  const key = `${questionId}:${optionId}`
  togglingKey.value = key
  errorText.value = null
  try {
    await commitAttemptActions({
      attemptId: attemptId.value,
      actions: [{ questionId, optionId, selected: !currentlySelected }],
    })
    qc.invalidateQueries({ queryKey: getGetAttemptsInProgressQueryKey() })
  } catch (e) {
    errorText.value = errorMessage(e)
  } finally {
    togglingKey.value = null
  }
}

const finishing = ref(false)
async function finish() {
  if (!confirm('Finish this attempt?')) return
  finishing.value = true
  errorText.value = null
  try {
    await finishAttempt({ attemptId: attemptId.value })
    await Promise.all([
      qc.invalidateQueries({ queryKey: getGetAttemptsInProgressQueryKey() }),
      qc.invalidateQueries({ queryKey: getGetFinishedAttemptsQueryKey() }),
    ])
    router.push(`/app/attempt/${attemptId.value}/result`)
  } catch (e) {
    errorText.value = errorMessage(e)
  } finally {
    finishing.value = false
  }
}

watch([attempt, isFinished, settled], ([a, fin, s]) => {
  if (s && !a && fin) {
    router.replace(`/app/attempt/${attemptId.value}/result`)
  }
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

    <ol class="qlist">
      <li v-for="(q, i) in attempt.questions ?? []" :key="q.id">
        <Card>
          <div class="qhead">
            <span class="label-sm muted">Question {{ i + 1 }} / {{ totalQuestions }}</span>
          </div>
          <p class="body-lg q-text">{{ q.text }}</p>
          <ul class="opts">
            <li v-for="o in q.options ?? []" :key="o.id">
              <button
                type="button"
                :class="['opt', { 'opt--selected': o.selected }]"
                :disabled="togglingKey === `${q.id}:${o.id}`"
                @click="toggleOption(q.id, o.id, o.selected)"
              >
                <span class="opt__marker">
                  <span v-if="o.selected" class="opt__dot" />
                </span>
                <span class="opt__text">{{ o.text }}</span>
              </button>
            </li>
          </ul>
        </Card>
      </li>
    </ol>
  </template>
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
.q-text {
  margin: 0 0 var(--space-md);
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
</style>
