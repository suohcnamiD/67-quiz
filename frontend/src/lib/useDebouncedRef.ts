import { ref, watch, type Ref } from 'vue'

/**
 * Returns a `debounced` ref that mirrors `source` after `delayMs` of quiet.
 * Use when an input drives a network query and we don't want a fetch per keystroke.
 */
export function useDebouncedRef<T>(source: Ref<T>, delayMs = 250): Ref<T> {
  const debounced = ref(source.value) as Ref<T>
  let handle: number | null = null
  watch(source, (next) => {
    if (handle != null) window.clearTimeout(handle)
    handle = window.setTimeout(() => {
      debounced.value = next
    }, delayMs)
  })
  return debounced
}
