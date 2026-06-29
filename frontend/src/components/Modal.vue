<script setup lang="ts">
import { onMounted, onUnmounted, watch, nextTick, ref } from 'vue'

defineOptions({ name: 'UiModal' })

const props = defineProps<{
  open: boolean
  title: string
  /** Label for the close button; defaults to "Close". */
  closeLabel?: string
}>()
const emit = defineEmits<{ (e: 'close'): void }>()

const dialogRef = ref<HTMLElement | null>(null)
let previouslyFocused: HTMLElement | null = null

function close() {
  emit('close')
}

function onKeydown(e: KeyboardEvent) {
  if (!props.open) return
  if (e.key === 'Escape') {
    e.preventDefault()
    close()
    return
  }
  if (e.key !== 'Tab') return
  // Light focus trap — keep Tab inside the dialog while it's open.
  const container = dialogRef.value
  if (!container) return
  const focusable = container.querySelectorAll<HTMLElement>(
    'a[href], button:not([disabled]), textarea, input, select, [tabindex]:not([tabindex="-1"])',
  )
  if (focusable.length === 0) return
  const first = focusable[0]!
  const last = focusable[focusable.length - 1]!
  const active = document.activeElement as HTMLElement | null
  if (e.shiftKey && active === first) {
    e.preventDefault()
    last.focus()
  } else if (!e.shiftKey && active === last) {
    e.preventDefault()
    first.focus()
  }
}

// Lock background scroll while the modal is open, restore focus on close.
watch(
  () => props.open,
  async (isOpen) => {
    if (isOpen) {
      previouslyFocused = document.activeElement as HTMLElement | null
      document.body.style.overflow = 'hidden'
      await nextTick()
      // Focus the dialog itself so screen readers announce its title before
      // the user lands on the first form field.
      dialogRef.value?.focus()
    } else {
      document.body.style.overflow = ''
      previouslyFocused?.focus?.()
    }
  },
  { immediate: true },
)

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})
</script>

<template>
  <Transition name="modal">
    <div
      v-if="open"
      class="overlay"
      role="dialog"
      aria-modal="true"
      :aria-label="title"
      @click.self="close"
    >
      <div ref="dialogRef" class="dialog" tabindex="-1">
        <header class="dialog__head">
          <h2 class="dialog__title">{{ title }}</h2>
          <button
            type="button"
            class="dialog__close"
            :aria-label="closeLabel ?? 'Close'"
            @click="close"
          >✕</button>
        </header>
        <div class="dialog__body">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="dialog__footer">
          <slot name="footer" />
        </footer>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-md);
}
.dialog {
  width: 100%;
  max-width: 32rem;
  max-height: calc(100vh - 4rem);
  display: flex;
  flex-direction: column;
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-overlay);
  outline: none;
}
.dialog__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-lg) var(--space-lg) var(--space-md);
  border-bottom: 1px solid var(--outline-variant);
}
.dialog__title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 800;
  letter-spacing: -0.01em;
  color: var(--on-surface);
}
.dialog__close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 0;
  background: transparent;
  color: var(--on-surface-variant);
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 120ms ease, color 120ms ease;
}
.dialog__close:hover {
  background: var(--surface-container-high);
  color: var(--on-surface);
}
.dialog__body {
  padding: var(--space-lg);
  overflow-y: auto;
}
.dialog__footer {
  padding: var(--space-md) var(--space-lg) var(--space-lg);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
  border-top: 1px solid var(--outline-variant);
}

/* Mount/unmount transitions. Skipped under reduced-motion. */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 180ms ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-active .dialog,
.modal-leave-active .dialog {
  transition: transform 220ms cubic-bezier(0.16, 1, 0.3, 1);
}
.modal-enter-from .dialog,
.modal-leave-to .dialog {
  transform: translateY(12px) scale(0.96);
}
@media (prefers-reduced-motion: reduce) {
  .modal-enter-active,
  .modal-leave-active,
  .modal-enter-active .dialog,
  .modal-leave-active .dialog {
    transition: none;
  }
  .modal-enter-from .dialog,
  .modal-leave-to .dialog {
    transform: none;
  }
}
</style>
