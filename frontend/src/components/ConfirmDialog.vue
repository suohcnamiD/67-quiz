<script setup lang="ts">
import { computed } from 'vue'
import Modal from './Modal.vue'
import Button from './Button.vue'
import { _internals } from '@/lib/confirmDialog'

const active = _internals.active

const isOpen = computed(() => active.value !== null)
const title = computed(() => active.value?.title ?? '')
const body = computed(() => active.value?.body ?? '')
const confirmLabel = computed(() => active.value?.confirmLabel ?? 'Confirm')
const cancelLabel = computed(() => active.value?.cancelLabel ?? 'Cancel')
const danger = computed(() => active.value?.danger ?? false)

function cancel() {
  _internals.settle(false)
}
function confirm() {
  _internals.settle(true)
}
</script>

<template>
  <Modal :open="isOpen" :title="title" @close="cancel">
    <p v-if="body" class="body-md body">{{ body }}</p>
    <template #footer>
      <Button variant="ghost" @click="cancel">{{ cancelLabel }}</Button>
      <Button :variant="danger ? 'danger' : 'primary'" @click="confirm">
        {{ confirmLabel }}
      </Button>
    </template>
  </Modal>
</template>

<style scoped>
.body {
  margin: 0;
  color: var(--on-surface-variant);
}
</style>
