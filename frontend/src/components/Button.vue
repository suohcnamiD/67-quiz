<script setup lang="ts">
defineProps<{
  variant?: 'primary' | 'ghost' | 'danger'
  type?: 'button' | 'submit'
  disabled?: boolean
  loading?: boolean
  fullWidth?: boolean
}>()
</script>

<template>
  <button
    :type="type ?? 'button'"
    :disabled="disabled || loading"
    :class="['btn', `btn--${variant ?? 'primary'}`, { 'btn--block': fullWidth }]"
  >
    <span v-if="loading" class="btn__spinner" aria-hidden="true" />
    <slot />
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  padding: 10px 20px;
  border-radius: var(--radius);
  border: 1px solid transparent;
  font-family: var(--font-text);
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: background-color 120ms ease, border-color 120ms ease, color 120ms ease;
  user-select: none;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn--block {
  width: 100%;
}

.btn--primary {
  background: var(--primary-container);
  color: var(--on-primary-container);
}
.btn--primary:hover:not(:disabled) {
  background: #c92424;
}

.btn--ghost {
  background: transparent;
  border-color: var(--outline-variant);
  color: var(--on-surface);
}
.btn--ghost:hover:not(:disabled) {
  border-color: var(--outline);
}

.btn--danger {
  background: transparent;
  border-color: var(--error-container);
  color: var(--error);
}
.btn--danger:hover:not(:disabled) {
  background: var(--error-container);
  color: var(--on-error-container);
}

.btn__spinner {
  width: 12px;
  height: 12px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 600ms linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@media (prefers-reduced-motion: reduce) {
  .btn { transition: none; }
  .btn__spinner { animation-duration: 1800ms; }
}
</style>
