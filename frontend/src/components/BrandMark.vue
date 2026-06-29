<script setup lang="ts">
defineOptions({ name: 'BrandMark' })

withDefaults(
  defineProps<{
    /** sm = topbar (22px), md = card/modal headers (32px), lg = landing hero (clamp). */
    size?: 'sm' | 'md' | 'lg'
    /** Render as a heading tag for landing/hero use. Defaults to a <span>. */
    as?: 'span' | 'h1'
  }>(),
  { size: 'sm', as: 'span' },
)
</script>

<template>
  <component
    :is="as"
    :class="['brand-mark', `brand-mark--${size}`]"
    aria-label="67quiz"
  >67quiz</component>
</template>

<style scoped>
.brand-mark {
  color: var(--on-surface);
  font-family: var(--font-display);
  font-weight: 800;
  font-style: italic;
  letter-spacing: -0.03em;
  line-height: 1;
  display: inline-block;
}
.brand-mark--sm {
  font-size: 22px;
}
.brand-mark--md {
  font-size: 32px;
  letter-spacing: -0.035em;
}
.brand-mark--lg {
  font-size: clamp(2.75rem, 8vw, 4.5rem);
  font-weight: 900;
  letter-spacing: -0.03em;
  line-height: 1.05;
  padding-bottom: 0.1em;
}
</style>
