<script setup lang="ts">
defineOptions({ name: 'UiChip' })

defineProps<{ tone?: 'neutral' | 'success' | 'danger' | 'warning' }>()
</script>

<template>
  <span :class="['chip', `chip--${tone ?? 'neutral'}`, 'label-sm']">
    <slot />
  </span>
</template>

<style scoped>
.chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: var(--radius);
  background: var(--surface-container-high);
  color: var(--on-surface);
}
.chip--success {
  background: var(--secondary-container);
  color: var(--on-secondary-container);
}
.chip--danger {
  background: var(--error-container);
  color: var(--on-error-container);
}
.chip--warning {
  background: transparent;
  color: var(--on-surface-variant);
  border: 1px solid var(--outline-variant);
}
</style>
