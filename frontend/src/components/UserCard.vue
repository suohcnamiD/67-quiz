<script setup lang="ts">
import { useRouter } from 'vue-router'
import Avatar from './Avatar.vue'
import type { AuthorSummaryDto } from '@/api/openAPIDefinition.schemas'

const props = defineProps<{ user: AuthorSummaryDto }>()
const router = useRouter()

function open() {
  if (props.user.username) router.push(`/app/users/${props.user.username}`)
}
</script>

<template>
  <button type="button" class="user" @click="open">
    <Avatar
      :username="user.username"
      :display-name="user.displayName"
      :initials-only="!user.hasAvatar"
      :size="36"
    />
    <span class="user__text">
      <span class="user__name">{{ user.displayName ?? user.username }}</span>
      <span class="user__handle label-sm muted">@{{ user.username }}</span>
    </span>
  </button>
</template>

<style scoped>
.user {
  appearance: none;
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-md);
  width: 100%;
  text-align: left;
  cursor: pointer;
  color: var(--on-surface);
  transition: border-color 120ms ease, background-color 120ms ease;
}
.user:hover {
  border-color: var(--outline);
  background: var(--surface-container-high);
}
.user__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.user__name {
  font-weight: 600;
  color: var(--on-surface);
}
.user__handle {
  font-variant-numeric: tabular-nums;
  text-transform: none;
  letter-spacing: normal;
}
.muted {
  color: var(--on-surface-variant);
}
</style>
