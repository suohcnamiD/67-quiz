<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

defineOptions({ name: 'MarkdownInline' })

const props = defineProps<{ source: string | null | undefined }>()

// Inline-only markdown for places where block elements would produce invalid
// HTML — most importantly option labels, which live inside <button>. marked's
// parseInline handles bold/italic/code/links; DOMPurify then strips anything
// block-level (or unsafe) that slipped through so we can safely v-html the
// result into an inline span.
const INLINE_TAGS = ['b', 'strong', 'i', 'em', 'code', 'a', 'br', 'span', 'u', 's', 'del', 'sub', 'sup']

const rendered = computed(() => {
  const raw = props.source?.trim()
  if (!raw) return ''
  const html = marked.parseInline(raw, { async: false, gfm: true }) as string
  return DOMPurify.sanitize(html, { ALLOWED_TAGS: INLINE_TAGS, ALLOWED_ATTR: ['href', 'title', 'target', 'rel'] })
})
</script>

<template><span class="md-inline" v-html="rendered" /></template>

<style scoped>
.md-inline :deep(code) {
  background: var(--surface-container-high);
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  font-family: var(--font-mono, ui-monospace, SFMono-Regular, monospace);
  font-size: 0.9em;
}
.md-inline :deep(a) {
  color: var(--primary-container);
  text-decoration: underline;
}
</style>
