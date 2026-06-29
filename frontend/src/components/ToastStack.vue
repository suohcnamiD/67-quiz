<script setup lang="ts">
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
</script>

<template>
  <Teleport to="body">
    <div
      class="toast-stack"
      aria-live="polite"
      aria-atomic="false"
    >
      <transition-group name="toast">
        <div
          v-for="t in toast.toasts"
          :key="t.id"
          :class="['toast', `toast--${t.tone}`]"
          :role="t.tone === 'error' ? 'alert' : 'status'"
        >
          <span class="toast__message body-md">{{ t.message }}</span>
          <button
            v-if="t.action"
            type="button"
            class="toast__action label-md"
            @click="t.action.handler(); toast.dismiss(t.id)"
          >{{ t.action.label }}</button>
          <button
            type="button"
            class="toast__close"
            aria-label="Dismiss"
            @click="toast.dismiss(t.id)"
          >✕</button>
        </div>
      </transition-group>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-stack {
  position: fixed;
  z-index: 1000;
  right: var(--space-md);
  bottom: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  max-width: min(420px, calc(100vw - 2 * var(--space-md)));
  pointer-events: none;
}
.toast {
  pointer-events: auto;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-lg);
  border: 1px solid var(--outline-variant);
  background: var(--surface-container);
  color: var(--on-surface);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
}
.toast--error {
  background: var(--error-container);
  color: var(--on-error-container);
  border-color: transparent;
}
.toast--warning {
  background: var(--surface-container-high);
  border-color: var(--outline-variant);
}
.toast--success {
  background: var(--surface-container-high);
}
.toast__message {
  flex: 1;
  min-width: 0;
}
.toast__action {
  appearance: none;
  background: transparent;
  border: 1px solid currentColor;
  color: inherit;
  font: inherit;
  padding: 4px 10px;
  border-radius: var(--radius);
  cursor: pointer;
}
.toast__action:hover {
  background: rgba(255, 255, 255, 0.1);
}
.toast__close {
  appearance: none;
  background: transparent;
  border: 0;
  color: inherit;
  font: inherit;
  opacity: 0.7;
  cursor: pointer;
  padding: 2px 4px;
  line-height: 1;
}
.toast__close:hover {
  opacity: 1;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
.toast-enter-active,
.toast-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}
@media (max-width: 640px) {
  .toast-stack {
    left: var(--space-md);
    right: var(--space-md);
    max-width: none;
    /* Sit above the FAB on mobile (FAB is ~56px + offset). */
    bottom: calc(var(--space-md) + 80px);
  }
}
@media (prefers-reduced-motion: reduce) {
  .toast-enter-active,
  .toast-leave-active {
    transition: none;
  }
}
</style>
