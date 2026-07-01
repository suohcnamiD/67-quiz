<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

defineOptions({ name: 'MarkdownView' })

const props = defineProps<{ source: string | null | undefined }>()

/**
 * Render the source string as safe HTML. marked handles the CommonMark
 * grammar; DOMPurify strips <script>/<iframe>/on*-handlers so untrusted
 * quiz descriptions can't ship XSS. GFM extensions (tables, strikethrough,
 * task lists) stay on — they're useful and safe once sanitised.
 */
const rendered = computed(() => {
  const raw = props.source?.trim()
  if (!raw) return ''
  const html = marked.parse(raw, { async: false, gfm: true, breaks: true }) as string
  return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
})
</script>

<template>
  <div v-if="rendered" class="md" v-html="rendered" />
</template>

<style scoped>
.md {
  color: var(--on-surface);
  line-height: 1.6;
}
.md :deep(p) {
  margin: 0 0 var(--space-md);
}
.md :deep(p:last-child) {
  margin-bottom: 0;
}
.md :deep(h1),
.md :deep(h2),
.md :deep(h3),
.md :deep(h4) {
  margin: var(--space-lg) 0 var(--space-sm);
  font-weight: 700;
  color: var(--on-surface);
  line-height: 1.3;
}
.md :deep(h1) { font-size: 1.5rem; }
.md :deep(h2) { font-size: 1.25rem; }
.md :deep(h3) { font-size: 1.1rem; }
.md :deep(h4) { font-size: 1rem; }
.md :deep(ul),
.md :deep(ol) {
  margin: 0 0 var(--space-md);
  padding-left: 1.5rem;
}
.md :deep(li) {
  margin-bottom: var(--space-xs);
}
.md :deep(code) {
  background: var(--surface-container-high);
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  font-family: var(--font-mono, ui-monospace, SFMono-Regular, monospace);
  font-size: 0.9em;
}
.md :deep(pre) {
  background: var(--surface-container-high);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius);
  padding: var(--space-md);
  overflow-x: auto;
  margin: 0 0 var(--space-md);
}
.md :deep(pre code) {
  background: transparent;
  padding: 0;
  border-radius: 0;
  font-size: 0.9rem;
}
.md :deep(blockquote) {
  border-left: 3px solid var(--outline-variant);
  padding: 0 var(--space-md);
  margin: 0 0 var(--space-md);
  color: var(--on-surface-variant);
}
.md :deep(a) {
  color: var(--primary-container);
  text-decoration: underline;
}
.md :deep(a:hover) {
  text-decoration: none;
}
.md :deep(hr) {
  border: 0;
  border-top: 1px solid var(--outline-variant);
  margin: var(--space-lg) 0;
}
.md :deep(table) {
  border-collapse: collapse;
  margin: 0 0 var(--space-md);
}
.md :deep(th),
.md :deep(td) {
  border: 1px solid var(--outline-variant);
  padding: 4px 8px;
  text-align: left;
}
.md :deep(th) {
  background: var(--surface-container-high);
  font-weight: 700;
}
.md :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius);
}
</style>
