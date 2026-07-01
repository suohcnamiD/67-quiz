<script setup lang="ts">
import { computed } from 'vue'

defineOptions({ name: 'CircleProgress' })

const props = withDefaults(
  defineProps<{
    /** Value in [0, 1]. Values outside are clamped. */
    value: number
    /** Outer diameter in px. */
    size?: number
    /** Ring thickness in px. */
    thickness?: number
    /** ARIA label describing what this circle represents. */
    label?: string
    /** Tone drives the ring colour: great = green, good = amber, tried = red. */
    tone?: 'great' | 'good' | 'tried'
  }>(),
  { size: 56, thickness: 6, tone: 'great' },
)

const clamped = computed(() => Math.max(0, Math.min(1, props.value)))
const radius = computed(() => (props.size - props.thickness) / 2)
const circumference = computed(() => 2 * Math.PI * radius.value)
const dashOffset = computed(() => circumference.value * (1 - clamped.value))
const center = computed(() => props.size / 2)
</script>

<template>
  <div
    class="circle"
    :class="`circle--${tone}`"
    :style="{ width: size + 'px', height: size + 'px' }"
    role="img"
    :aria-label="label ?? `${Math.round(clamped * 100)}%`"
  >
    <svg :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`" aria-hidden="true">
      <circle
        class="circle__track"
        :cx="center"
        :cy="center"
        :r="radius"
        fill="none"
        :stroke-width="thickness"
      />
      <circle
        class="circle__fill"
        :cx="center"
        :cy="center"
        :r="radius"
        fill="none"
        :stroke-width="thickness"
        stroke-linecap="round"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="dashOffset"
        :transform="`rotate(-90 ${center} ${center})`"
      />
    </svg>
    <div class="circle__label">
      <slot>{{ Math.round(clamped * 100) }}%</slot>
    </div>
  </div>
</template>

<style scoped>
.circle {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.circle__track {
  stroke: var(--outline-variant);
}
.circle__fill {
  transition: stroke-dashoffset 400ms ease;
}
.circle--great .circle__fill { stroke: var(--on-secondary-container); }
.circle--good .circle__fill  { stroke: #d9a24a; }
.circle--tried .circle__fill { stroke: var(--on-error-container); }

.circle__label {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-variant-numeric: tabular-nums;
  font-weight: 800;
  color: var(--on-surface);
  font-size: 0.9rem;
  letter-spacing: -0.01em;
}
</style>
