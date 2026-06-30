<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from '@/components/Button.vue'

defineOptions({ name: 'ImageUploader' })

const props = defineProps<{
  /** Public URL to the current image (without ?v cache-buster — we manage it). */
  imageUrl?: string | null
  /** Whether there's an image to delete. */
  hasImage?: boolean
  /** Label shown on the upload button when no image. */
  emptyLabel?: string
  /** Disabled state. */
  disabled?: boolean
  /**
   * Optional fixed aspect ratio for the preview (e.g. "16 / 9" or "2 / 1").
   * When set, the preview becomes a fixed-aspect box that crops the image
   * to match — useful when the upload feeds into a display slot of known
   * dimensions (e.g. quiz cover cards).
   */
  aspectRatio?: string
}>()

const emit = defineEmits<{
  (e: 'upload', file: File): void
  (e: 'delete'): void
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const previewUrl = ref<string | null>(null)
const version = ref(0)

watch(() => props.hasImage, () => {
  // When the server confirms upload/delete completed, drop the local preview
  // and bump the version so the <img> refetches.
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = null
  }
  version.value++
})

const display = computed(() => {
  if (previewUrl.value) return previewUrl.value
  if (props.hasImage && props.imageUrl) {
    const sep = props.imageUrl.includes('?') ? '&' : '?'
    return `${props.imageUrl}${sep}v=${version.value}`
  }
  return null
})

function pick() {
  fileInput.value?.click()
}

function onFilePicked(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(file)
  emit('upload', file)
  // Reset the input so picking the same file twice re-fires.
  target.value = ''
}

function onDelete() {
  emit('delete')
}
</script>

<template>
  <div class="img-uploader">
    <div
      v-if="display"
      class="img-uploader__preview"
      :class="{ 'img-uploader__preview--fixed': aspectRatio }"
      :style="aspectRatio ? { aspectRatio } : undefined"
    >
      <img :src="display" alt="" loading="lazy" />
      <div class="img-uploader__overlay">
        <Button type="button" variant="ghost" :disabled="disabled" @click="pick">Replace</Button>
        <Button type="button" variant="danger" :disabled="disabled" @click="onDelete">Remove</Button>
      </div>
    </div>
    <button
      v-else
      type="button"
      class="img-uploader__add"
      :disabled="disabled"
      :aria-label="emptyLabel ?? 'Add image'"
      :title="emptyLabel ?? 'Add image'"
      @click="pick"
    >
      <svg
        aria-hidden="true"
        viewBox="0 0 24 24"
        width="20"
        height="20"
        fill="none"
        stroke="currentColor"
        stroke-width="1.75"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <rect x="3" y="5" width="18" height="14" rx="2" />
        <circle cx="8.5" cy="10.5" r="1.5" />
        <path d="M21 16l-4.5-4.5L7 21" />
      </svg>
      <span class="visually-hidden">{{ emptyLabel ?? 'Add image' }}</span>
    </button>
    <input
      ref="fileInput"
      type="file"
      accept="image/png,image/jpeg,image/webp"
      class="img-uploader__file"
      @change="onFilePicked"
    />
  </div>
</template>

<style scoped>
.img-uploader {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.img-uploader__preview {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  width: fit-content;
  max-width: 100%;
}
.img-uploader__preview img {
  display: block;
  max-width: 100%;
  max-height: 280px;
  height: auto;
}
/* Fixed-aspect variant: the preview matches its display slot exactly, so
 * what the user sees here is what shows on the consumer (e.g. a quiz card
 * cover). The image fills the box and crops via object-fit: cover. */
.img-uploader__preview--fixed {
  width: 100%;
  max-width: 480px;
}
.img-uploader__preview--fixed img {
  width: 100%;
  height: 100%;
  max-width: none;
  max-height: none;
  object-fit: cover;
}
.img-uploader__overlay {
  position: absolute;
  inset: auto 0 0 0;
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-sm);
  background: linear-gradient(to top, rgba(0, 0, 0, 0.65), transparent);
}
.img-uploader__add {
  appearance: none;
  background: transparent;
  border: 1px dashed var(--outline-variant);
  color: var(--on-surface-variant);
  border-radius: var(--radius);
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 120ms ease, border-color 120ms ease, background-color 120ms ease;
}
.img-uploader__add:hover:not(:disabled) {
  color: var(--on-surface);
  border-color: var(--outline);
  background: var(--surface-container-high);
  border-style: solid;
}
.img-uploader__add:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.img-uploader__file {
  display: none;
}
.visually-hidden {
  position: absolute !important;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
