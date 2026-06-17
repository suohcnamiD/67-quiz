<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGetFinishedAttempts } from '@/api/attempt-controller/attempt-controller'
import Card from '@/components/Card.vue'
import Chip from '@/components/Chip.vue'
import Button from '@/components/Button.vue'
import type { FinishedQuestionDto } from '@/api/openAPIDefinition.schemas'

const route = useRoute()
const router = useRouter()
const attemptId = computed(() => route.params.attemptId as string)

const { data, isLoading, isFetching } = useGetFinishedAttempts({ page: 0 })
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
</script>

<template>
  <div v-if="isLoading || (isFetching && !attempt)" class="empty body-md">Loading…</div>
  <div v-else-if="!attempt" class="empty body-md">Result not found.</div>
  <template v-else>
    <header class="head">
      <div>
        <span class="label-sm muted">{{ attempt.quiz?.name ?? 'Untitled quiz' }}</span>
        <p class="headline-xl score">
          {{ attempt.score ?? 0 }} <span class="muted">/ {{ attempt.maximumScore ?? 0 }}</span>
        </p>
      </div>
      <Button variant="ghost" @click="router.push('/app')">Back to browse</Button>
    </header>

    <ol class="qlist">
      <li v-for="(q, i) in attempt.questions ?? []" :key="q.id">
        <Card>
          <div class="qhead">
            <span class="label-sm muted">Question {{ i + 1 }}</span>
            <span class="qhead__score label-md">
              {{ questionScore(q).earned }} / {{ questionScore(q).max }}
            </span>
          </div>
          <p class="body-lg q-text">{{ q.text }}</p>
          <ul class="opts">
            <li
              v-for="o in q.options ?? []"
              :key="o.id"
              :class="[
                'opt',
                {
                  'opt--correct': o.correct && o.selected,
                  'opt--missed': o.correct && !o.selected,
                  'opt--wrong': !o.correct && o.selected,
                },
              ]"
            >
              <span class="opt__text">{{ o.text }}</span>
              <Chip v-if="!o.correct && o.selected" tone="danger">Wrong</Chip>
              <Chip v-else-if="o.correct && !o.selected" tone="warning">Missed</Chip>
            </li>
          </ul>
        </Card>
      </li>
    </ol>
  </template>
</template>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: var(--space-md);
  margin-bottom: var(--space-xl);
}
.score {
  margin: var(--space-xs) 0 var(--space-xs);
}
.muted {
  color: var(--on-surface-variant);
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
.qhead__score {
  font-variant-numeric: tabular-nums;
  color: var(--on-surface);
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
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-md);
  background: var(--surface-container-low);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius);
}
.opt--correct {
  border-color: var(--secondary-container);
  box-shadow: 0 0 0 5px rgba(0, 91, 20, 0.25);
}
.opt--missed {
  border-color: var(--secondary-container);
}
.opt--wrong {
  border-color: var(--error-container);
}
.opt__text {
  flex: 1;
}
.empty {
  color: var(--on-surface-variant);
}
</style>
