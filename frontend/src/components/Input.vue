<script setup lang="ts">
defineProps<{
  label?: string
  modelValue: string
  type?: string
  placeholder?: string
  error?: string
  autocomplete?: string
}>()
defineEmits<{ (e: 'update:modelValue', value: string): void }>()
</script>

<template>
  <label class="field">
    <span v-if="label" class="field__label label-md">{{ label }}</span>
    <input
      :type="type ?? 'text'"
      :value="modelValue"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      :class="['field__input', { 'field__input--error': error }]"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <span v-if="error" class="field__error label-sm">{{ error }}</span>
  </label>
</template>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.field__label {
  color: var(--on-surface-variant);
}
.field__input {
  width: 100%;
  background: var(--surface-container-lowest);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius);
  padding: 10px 12px;
  color: var(--on-surface);
  outline: none;
  transition: border-color 120ms ease;
}
.field__input::placeholder {
  color: var(--on-surface-variant);
  opacity: 0.55;
}
.field__input:focus {
  border-color: var(--primary-container);
}
.field__input--error {
  border-color: var(--error-container);
}
.field__error {
  color: var(--error);
}
</style>
