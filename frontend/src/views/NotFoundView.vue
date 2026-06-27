<script setup lang="ts">
import { useRouter } from 'vue-router'
import Button from '@/components/Button.vue'

const router = useRouter()
</script>

<template>
  <main class="page">
    <div class="art" aria-hidden="true">
      <span class="digit">4</span>
      <span class="digit digit--mid">0</span>
      <span class="digit">4</span>
    </div>
    <h1 class="title">Page not found</h1>
    <p class="body-lg subtitle">
      We couldn't find that one. The link might be wrong, or the page might have moved.
    </p>
    <div class="actions">
      <Button @click="router.push('/app')">Back to browse</Button>
      <Button variant="ghost" @click="router.back()">Go back</Button>
    </div>
  </main>
</template>

<style scoped>
.page {
  min-height: calc(100vh - 12rem);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: var(--space-md);
  padding: var(--space-xl) var(--margin-mobile);
}
.art {
  display: inline-flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
  font-variant-numeric: tabular-nums;
}
.digit {
  font-size: 6rem;
  font-weight: 800;
  letter-spacing: -0.05em;
  color: var(--on-surface);
  line-height: 1;
}
.digit--mid {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 6rem;
  height: 6rem;
  border-radius: 50%;
  border: 6px solid var(--on-secondary-container);
  color: var(--on-secondary-container);
  font-size: 4.5rem;
  animation: bob 4s ease-in-out infinite;
}
.title {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 800;
  letter-spacing: -0.01em;
}
.subtitle {
  margin: 0 0 var(--space-md);
  max-width: 32rem;
  color: var(--on-surface-variant);
}
.actions {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
  justify-content: center;
}

@keyframes bob {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
@media (prefers-reduced-motion: reduce) {
  .digit--mid { animation: none; }
}
@media (max-width: 480px) {
  .digit { font-size: 4.5rem; }
  .digit--mid { width: 4.5rem; height: 4.5rem; font-size: 3.25rem; border-width: 5px; }
}
</style>
