<script setup lang="ts">
const props = defineProps<{
  /** value between 0 and 1 */
  value: number
}>()

const pct = () => Math.max(0, Math.min(1, props.value)) * 100
</script>

<template>
  <div class="track" role="progressbar" :aria-valuenow="Math.round(pct())" aria-valuemin="0" aria-valuemax="100">
    <div class="fill" :style="{ width: pct() + '%' }" />
  </div>
</template>

<style scoped>
.track {
  width: 100%;
  height: 4px;
  background: var(--surface-container-low);
}
.fill {
  height: 100%;
  background: var(--primary-container);
  transition: width 200ms linear;
}
</style>
