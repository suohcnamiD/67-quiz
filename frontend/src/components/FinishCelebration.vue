<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  score: number
  max: number
  quizName?: string
}>()
const emit = defineEmits<{ (e: 'close'): void }>()

const visible = ref(false)
const displayedScore = ref(0)

const percent = computed(() => (props.max > 0 ? props.score / props.max : 0))
const tier = computed(() => {
  if (percent.value >= 0.85) return 'great'
  if (percent.value >= 0.5) return 'good'
  return 'tried'
})
const heading = computed(() => {
  if (tier.value === 'great') return 'Brilliant!'
  if (tier.value === 'good') return 'Nice work.'
  return 'Attempt complete.'
})

// Pre-compute deterministic confetti positions so each render looks the same
// and we don't pay for inline random() on every reactive tick.
const confetti = Array.from({ length: 32 }, (_, i) => {
  // Pseudo-random but stable per-index — avoids SSR/hydration drift.
  const rand = (seed: number) => {
    const x = Math.sin(seed * 9301 + 49297) * 233280
    return x - Math.floor(x)
  }
  const left = rand(i + 1) * 100
  const delay = rand(i + 2) * 300
  const duration = 1100 + rand(i + 3) * 900
  const drift = (rand(i + 4) - 0.5) * 120
  const hue = Math.floor(rand(i + 5) * 360)
  return { left, delay, duration, drift, hue, key: i }
})

let countTimer: number | null = null
let dismissTimer: number | null = null

function close() {
  visible.value = false
  emit('close')
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    e.preventDefault()
    close()
  }
}

onMounted(() => {
  // Trigger the entrance animation on the next frame so the initial-mount
  // transform takes effect.
  requestAnimationFrame(() => {
    visible.value = true
  })

  // Count up the score over ~900ms.
  const start = performance.now()
  const duration = 900
  const target = props.score
  const tick = (t: number) => {
    const elapsed = t - start
    const progress = Math.min(1, elapsed / duration)
    // Ease-out quart for a satisfying decel.
    const eased = 1 - Math.pow(1 - progress, 4)
    displayedScore.value = Math.round(target * eased)
    if (progress < 1) countTimer = requestAnimationFrame(tick)
  }
  countTimer = requestAnimationFrame(tick)

  // Auto-dismiss after 3.5s, but let the user click or press Escape to
  // dismiss earlier.
  dismissTimer = window.setTimeout(close, 3500)
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  if (countTimer) cancelAnimationFrame(countTimer)
  if (dismissTimer) clearTimeout(dismissTimer)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div
    :class="['overlay', { 'overlay--visible': visible }]"
    role="dialog"
    aria-modal="true"
    aria-labelledby="celebration-heading"
    @click="close"
  >
    <div class="confetti" aria-hidden="true">
      <span
        v-for="c in confetti"
        :key="c.key"
        class="confetti__piece"
        :style="{
          left: c.left + '%',
          animationDelay: c.delay + 'ms',
          animationDuration: c.duration + 'ms',
          background: `hsl(${c.hue}, 80%, 60%)`,
          ['--drift' as string]: c.drift + 'px',
        }"
      />
    </div>

    <div :class="['popup', `popup--${tier}`]" @click.stop>
      <span class="popup__eyebrow label-sm">{{ quizName ?? 'Attempt finished' }}</span>
      <h2 id="celebration-heading" class="popup__heading">{{ heading }}</h2>
      <div class="popup__score">
        <span class="popup__score-number">{{ displayedScore }}</span>
        <span class="popup__score-total">/ {{ max }}</span>
      </div>
      <button type="button" class="popup__dismiss label-md" @click="close">Tap to see breakdown</button>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0);
  pointer-events: none;
  transition: background 240ms ease;
}
.overlay--visible {
  background: rgba(0, 0, 0, 0.55);
  pointer-events: auto;
}

.popup {
  position: relative;
  min-width: 18rem;
  max-width: min(28rem, calc(100vw - 2rem));
  padding: var(--space-xl) var(--space-xl);
  border-radius: var(--radius-xl);
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  box-shadow: var(--shadow-overlay);
  text-align: center;
  transform: translateY(24px) scale(0.85);
  opacity: 0;
  transition:
    transform 380ms cubic-bezier(0.16, 1, 0.3, 1),
    opacity 240ms ease;
}
.overlay--visible .popup {
  transform: translateY(0) scale(1);
  opacity: 1;
}

.popup--great {
  border-color: var(--secondary-container);
  box-shadow:
    var(--shadow-overlay),
    0 0 80px 0 rgba(129, 210, 124, 0.35);
}
.popup--good {
  border-color: var(--primary-container);
}
.popup--tried {
  border-color: var(--outline);
}

.popup__eyebrow {
  display: block;
  color: var(--on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: var(--space-sm);
}
.popup__heading {
  margin: 0 0 var(--space-lg);
  font-size: 2rem;
  font-weight: 800;
  letter-spacing: -0.01em;
  color: var(--on-surface);
}
.popup--great .popup__heading {
  background: linear-gradient(135deg, #b9f6ca 0%, #81d27c 60%, #ffcac4 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.popup__score {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-lg);
  font-variant-numeric: tabular-nums;
}
.popup__score-number {
  font-size: 4rem;
  font-weight: 800;
  line-height: 1;
  color: var(--on-surface);
}
.popup__score-total {
  font-size: 1.5rem;
  color: var(--on-surface-variant);
}

.popup__dismiss {
  appearance: none;
  background: transparent;
  border: 0;
  color: var(--on-surface-variant);
  cursor: pointer;
  padding: var(--space-xs) var(--space-sm);
}
.popup__dismiss:hover {
  color: var(--on-surface);
}

.confetti {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}
.confetti__piece {
  position: absolute;
  top: -4%;
  width: 8px;
  height: 14px;
  border-radius: 1px;
  opacity: 0;
  transform: translate3d(0, 0, 0) rotate(0deg);
  animation-name: fall;
  animation-timing-function: cubic-bezier(0.4, 0, 0.6, 1);
  animation-fill-mode: forwards;
  animation-iteration-count: 1;
}
.overlay--visible .confetti__piece {
  opacity: 1;
}

@keyframes fall {
  0% {
    transform: translate3d(0, -10vh, 0) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  100% {
    transform: translate3d(var(--drift, 0px), 110vh, 0) rotate(720deg);
    opacity: 0.8;
  }
}

@media (prefers-reduced-motion: reduce) {
  .popup,
  .confetti__piece {
    transition: none;
    animation: none;
  }
  .popup {
    transform: none;
    opacity: 1;
  }
  .confetti__piece {
    display: none;
  }
}
</style>
