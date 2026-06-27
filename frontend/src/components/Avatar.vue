<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    username?: string | null
    displayName?: string | null
    size?: number
    /** Bumped externally when the user's avatar changes so the <img> refetches. */
    version?: number
    /** Optional: force the rendered initials regardless of the real backend state. */
    initialsOnly?: boolean
  }>(),
  { size: 40, version: 0, initialsOnly: false },
)

// FNV-1a hash → hue. Deterministic, decent spread.
function hueFromName(name: string): number {
  let h = 0x811c9dc5
  for (let i = 0; i < name.length; i++) {
    h ^= name.charCodeAt(i)
    h = Math.imul(h, 0x01000193)
  }
  return Math.abs(h) % 360
}

function initialsOf(displayName: string | null | undefined, username: string | null | undefined): string {
  const source = (displayName?.trim() || username || '?').trim()
  const parts = source.split(/\s+/).filter(Boolean)
  if (parts.length >= 2) {
    const first = parts[0] ?? ''
    const second = parts[1] ?? ''
    return (first.charAt(0) + second.charAt(0)).toUpperCase()
  }
  return source.slice(0, 2).toUpperCase()
}

const failed = ref(false)
const src = computed(() =>
  !props.initialsOnly && props.username && !failed.value
    ? `/api/users/${props.username}/avatar?v=${props.version}`
    : null,
)

// Reset the failed flag whenever the source changes (new user or version bump).
watch(
  () => `${props.username}|${props.version}|${props.initialsOnly}`,
  () => { failed.value = false },
)

const hue = computed(() => hueFromName(props.username ?? props.displayName ?? '?'))
const initials = computed(() => initialsOf(props.displayName, props.username))
const fontSize = computed(() => `${Math.max(10, Math.round(props.size * 0.42))}px`)
</script>

<template>
  <span
    class="avatar"
    :style="{
      width: size + 'px',
      height: size + 'px',
      backgroundColor: `hsl(${hue}, 55%, 32%)`,
      fontSize: fontSize,
    }"
    :title="displayName ?? username ?? ''"
  >
    <img
      v-if="src"
      :src="src"
      alt=""
      @error="failed = true"
    />
    <span v-else aria-hidden="true">{{ initials }}</span>
  </span>
</template>

<style scoped>
.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
  user-select: none;
  line-height: 1;
  letter-spacing: 0.02em;
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
</style>
