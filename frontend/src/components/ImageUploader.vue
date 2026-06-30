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
    <div v-if="display" class="img-uploader__preview">
      <img :src="display" alt="" loading="lazy" />
      <div class="img-uploader__overlay">
        <Button type="button" variant="ghost" :disabled="disabled" @click="pick">Replace</Button>
        <Button type="button" variant="danger" :disabled="disabled" @click="onDelete">Remove</Button>
      </div>
    </div>
    <Button v-else type="button" variant="ghost" :disabled="disabled" @click="pick">
      {{ emptyLabel ?? 'Add image' }}
    </Button>
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
.img-uploader__overlay {
  position: absolute;
  inset: auto 0 0 0;
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-sm);
  background: linear-gradient(to top, rgba(0, 0, 0, 0.65), transparent);
}
.img-uploader__file {
  display: none;
}
</style>
