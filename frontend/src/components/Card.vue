<script setup lang="ts">
defineProps<{
  interactive?: boolean
}>()
</script>

<template>
  <div :class="['card', { 'card--interactive': interactive }]">
    <slot />
  </div>
</template>

<style scoped>
.card {
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  transition: border-color 120ms ease;
}
.card--interactive {
  cursor: pointer;
}
.card--interactive:hover {
  border-color: var(--outline);
}
</style>
