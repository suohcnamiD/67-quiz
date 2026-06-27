import { ref } from 'vue'

export interface ConfirmDialogOptions {
  title: string
  body?: string
  confirmLabel?: string
  cancelLabel?: string
  /** Style the confirm button as destructive. */
  danger?: boolean
}

interface ActiveDialog extends ConfirmDialogOptions {
  resolve: (ok: boolean) => void
}

// Shared singleton state. `<ConfirmDialog />` (mounted once in App.vue)
// subscribes to this; call sites use `confirmDialog.open(...)` and await
// the boolean.
const active = ref<ActiveDialog | null>(null)

function open(options: ConfirmDialogOptions): Promise<boolean> {
  // If a dialog is already open, resolve it as cancelled before replacing.
  if (active.value) {
    active.value.resolve(false)
  }
  return new Promise((resolve) => {
    active.value = { ...options, resolve }
  })
}

function settle(ok: boolean) {
  const a = active.value
  if (!a) return
  active.value = null
  a.resolve(ok)
}

export const confirmDialog = { open }
// Internal API for the renderer component — not exported from a public path.
export const _internals = { active, settle }
