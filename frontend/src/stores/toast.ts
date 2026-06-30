import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastTone = 'error' | 'warning' | 'info' | 'success'

export interface Toast {
  id: number
  tone: ToastTone
  message: string
  // Optional inline action (e.g. "Retry") rendered as a button on the toast.
  action?: { label: string; handler: () => void }
  // Milliseconds before auto-dismiss. 0 = sticky.
  durationMs: number
}

const DEFAULTS: Record<ToastTone, number> = {
  error: 6000,
  warning: 5000,
  info: 4000,
  success: 3000,
}

export const useToastStore = defineStore('toast', () => {
  const toasts = ref<Toast[]>([])
  let nextId = 1

  function push(input: {
    tone?: ToastTone
    message: string
    action?: { label: string; handler: () => void }
    durationMs?: number
  }): number {
    const tone = input.tone ?? 'info'
    const durationMs = input.durationMs ?? DEFAULTS[tone]
    const id = nextId++
    toasts.value.push({ id, tone, message: input.message, action: input.action, durationMs })
    if (durationMs > 0) {
      window.setTimeout(() => dismiss(id), durationMs)
    }
    return id
  }

  function dismiss(id: number) {
    const i = toasts.value.findIndex((t) => t.id === id)
    if (i >= 0) toasts.value.splice(i, 1)
  }

  function clear() {
    toasts.value = []
  }

  return { toasts, push, dismiss, clear }
})
