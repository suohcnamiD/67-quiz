<script setup lang="ts">
import { ref, computed, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGetAttemptsInProgress, commitAttemptActions, finishAttempt } from '@/api/attempt-controller/attempt-controller'
import { useQueryClient } from '@tanstack/vue-query'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import Chip from '@/components/Chip.vue'

const route = useRoute()
const router = useRouter()
const qc = useQueryClient()
const attemptId = computed(() => route.params.attemptId as string)

const { data, isLoading } = useGetAttemptsInProgress({ page: 0 })
const attempt = computed(() =>
  (data.value?._embedded?.attempts ?? []).find((a) => a.id === attemptId.value),
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

const answeredCount = computed(
  () => (attempt.value?.questions ?? []).filter((q) => q.options?.some((o) => o.selected)).length,
)
const totalQuestions = computed(() => attempt.value?.questions?.length ?? 0)

const togglingKey = ref<string | null>(null)
async function toggleOption(questionId?: string, optionId?: string, currentlySelected?: boolean) {
  if (!questionId || !optionId) return
  const key = `${questionId}:${optionId}`
  togglingKey.value = key
  try {
    await commitAttemptActions({
      attemptId: attemptId.value,
      actions: [{ questionId, optionId, selected: !currentlySelected }],
    })
    qc.invalidateQueries({ queryKey: ['attempt', 'in-progress'] })
  } finally {
    togglingKey.value = null
  }
}

const finishing = ref(false)
async function finish() {
  if (!confirm('Finish this attempt? You won’t be able to change answers afterward.')) return
  finishing.value = true
  try {
    await finishAttempt({ attemptId: attemptId.value })
    qc.invalidateQueries({ queryKey: ['attempt', 'in-progress'] })
    qc.invalidateQueries({ queryKey: ['attempt', 'finished'] })
    router.push(`/app/attempt/${attemptId.value}/result`)
  } finally {
    finishing.value = false
  }
}

watch(attempt, (a) => {
  // If the attempt isn't in the in-progress list, it may already be finished — bounce to result.
  if (!isLoading.value && !a && data.value) {
    router.replace(`/app/attempt/${attemptId.value}/result`)
  }
})
</script>

<template>
  <div v-if="isLoading" class="empty body-md">Loading…</div>
  <template v-else-if="attempt">
    <div class="bar">
      <ProgressBar :value="remainingPct" />
    </div>
    <header class="head">
      <div class="meta-stack">
        <span class="label-sm muted">Time remaining</span>
        <span class="headline-lg time">{{ fmtRemaining(remainingMs) }}</span>
      </div>
      <div class="meta-stack right">
        <span class="label-sm muted">Answered</span>
        <span class="headline-md">{{ answeredCount }} / {{ totalQuestions }}</span>
      </div>
      <Button :loading="finishing" @click="finish">Finish attempt</Button>
    </header>

    <ol class="qlist">
      <li v-for="(q, i) in attempt.questions ?? []" :key="q.id">
        <Card>
          <div class="qhead">
            <span class="label-sm muted">Question {{ i + 1 }} / {{ totalQuestions }}</span>
            <Chip v-if="q.options?.some((o) => o.selected)" tone="success">Answered</Chip>
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
.muted {
  color: var(--on-surface-variant);
}
.time {
  font-variant-numeric: tabular-nums;
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
</style>
