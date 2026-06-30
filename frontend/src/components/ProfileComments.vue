<script setup lang="ts">
import { ref, computed, toRef } from 'vue'
import { useQueryClient } from '@tanstack/vue-query'
import {
  useListComments,
  usePostComment,
  useDeleteComment,
  getListCommentsQueryKey,
} from '@/api/profile-comment-controller/profile-comment-controller'
import { errorMessage, firstErrorCode } from '@/lib/errors'
import { useAuthStore } from '@/stores/auth'
import { confirmDialog } from '@/lib/confirmDialog'
import Avatar from '@/components/Avatar.vue'
import Button from '@/components/Button.vue'

const props = defineProps<{ username: string }>()
const usernameRef = toRef(props, 'username')

const auth = useAuthStore()
const qc = useQueryClient()

const page = ref(0)
const listParams = computed(() => ({ page: page.value }))
const list = useListComments(usernameRef, listParams)
const comments = computed(() => list.data.value?._embedded?.comments ?? [])
const totalPages = computed(() => list.data.value?.page?.totalPages ?? 1)
const totalElements = computed(() => list.data.value?.page?.totalElements ?? 0)

const body = ref('')
const bodyError = ref<string | null>(null)
const submitError = ref<string | null>(null)

const charsLeft = computed(() => 1000 - body.value.length)
const tooLong = computed(() => charsLeft.value < 0)

const postMutation = usePostComment()
const deleteMutation = useDeleteComment()

async function post() {
  bodyError.value = null
  submitError.value = null
  const trimmed = body.value.trim()
  if (!trimmed) {
    bodyError.value = 'Write something before posting.'
    return
  }
  if (trimmed.length > 1000) {
    bodyError.value = 'Comment must be at most 1000 characters.'
    return
  }
  try {
    await postMutation.mutateAsync({
      username: props.username,
      data: { body: trimmed },
    })
    body.value = ''
    page.value = 0
    qc.invalidateQueries({ queryKey: getListCommentsQueryKey(props.username) })
  } catch (e) {
    const code = firstErrorCode(e)
    if (code === 'INVALID_COMMENT') {
      bodyError.value = errorMessage(e)
    } else {
      submitError.value = errorMessage(e)
    }
  }
}

async function remove(commentId: string) {
  const ok = await confirmDialog.open({
    title: 'Delete this comment?',
    body: 'This cannot be undone.',
    confirmLabel: 'Delete',
    danger: true,
  })
  if (!ok) return
  try {
    await deleteMutation.mutateAsync({ username: props.username, commentId })
    qc.invalidateQueries({ queryKey: getListCommentsQueryKey(props.username) })
  } catch (e) {
    submitError.value = errorMessage(e)
  }
}

function fmtRelative(iso?: string): string {
  if (!iso) return ''
  const then = new Date(iso).getTime()
  if (Number.isNaN(then)) return ''
  const diffSec = Math.round((Date.now() - then) / 1000)
  if (Math.abs(diffSec) < 45) return 'just now'
  const diffMin = Math.round(diffSec / 60)
  if (Math.abs(diffMin) < 60) return `${diffMin} min ago`
  const diffHr = Math.round(diffSec / 3600)
  if (Math.abs(diffHr) < 24) return `${diffHr} h ago`
  const diffDay = Math.round(diffSec / 86400)
  if (Math.abs(diffDay) < 30) return `${diffDay} d ago`
  return new Date(iso).toLocaleDateString()
}
</script>

<template>
  <section class="comments" :aria-label="`Comments on ${username}'s profile`">
    <header class="comments__head">
      <h2 class="headline-md">
        Comments
        <span v-if="totalElements > 0" class="muted label-md">· {{ totalElements }}</span>
      </h2>
    </header>

    <form v-if="auth.isAuthenticated()" class="composer" @submit.prevent="post">
      <textarea
        v-model="body"
        class="composer__input"
        :class="{ 'composer__input--error': !!bodyError }"
        :disabled="postMutation.isPending.value"
        rows="3"
        maxlength="1100"
        placeholder="Leave a comment…"
        :aria-invalid="!!bodyError"
        aria-label="New comment"
      ></textarea>
      <div class="composer__foot">
        <span v-if="bodyError" class="composer__error body-md" role="alert">{{ bodyError }}</span>
        <span
          v-else
          class="composer__counter body-md"
          :class="{ 'composer__counter--bad': tooLong }"
        >{{ charsLeft }} left</span>
        <Button type="submit" :loading="postMutation.isPending.value" :disabled="tooLong">Post</Button>
      </div>
      <p v-if="submitError" class="banner label-md" role="alert">{{ submitError }}</p>
    </form>

    <p v-if="list.isLoading.value" class="empty body-md">Loading comments…</p>
    <p v-else-if="!comments.length" class="empty body-md">
      No comments yet.
      <span v-if="auth.isAuthenticated()">Be the first.</span>
    </p>

    <ul v-else class="list">
      <li v-for="c in comments" :key="c.id" class="item">
        <div class="item__head">
          <RouterLink
            v-if="c.author?.username"
            :to="{ name: 'user-profile', params: { username: c.author.username } }"
            class="item__user"
          >
            <Avatar
              :username="c.author.username"
              :display-name="c.author.displayName"
              :initials-only="!c.author.hasAvatar"
              :size="32"
            />
            <span class="item__name">{{ c.author.displayName ?? c.author.username }}</span>
          </RouterLink>
          <span class="item__time body-md muted">{{ fmtRelative(c.createdAt) }}</span>
          <button
            v-if="c.canDelete"
            type="button"
            class="item__delete"
            :aria-label="`Delete comment by ${c.author?.displayName ?? c.author?.username ?? 'user'}`"
            @click="c.id && remove(c.id)"
          >✕</button>
        </div>
        <p class="item__body body-md">{{ c.body }}</p>
      </li>
    </ul>

    <div v-if="totalPages > 1" class="pager">
      <Button
        variant="ghost"
        :disabled="page === 0"
        @click="page = Math.max(0, page - 1)"
      >Previous</Button>
      <span class="body-md muted">Page {{ page + 1 }} / {{ totalPages }}</span>
      <Button
        variant="ghost"
        :disabled="page + 1 >= totalPages"
        @click="page = page + 1"
      >Next</Button>
    </div>
  </section>
</template>

<style scoped>
.comments {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.comments__head h2 { margin: 0; }
.muted { color: var(--on-surface-variant); }

.composer {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.composer__input {
  width: 100%;
  background: var(--surface-container-lowest);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius);
  color: var(--on-surface);
  padding: 10px 12px;
  font: inherit;
  resize: vertical;
  min-height: 72px;
}
.composer__input:focus {
  outline: none;
  border-color: var(--primary-container);
}
.composer__input--error { border-color: var(--error-container); }
.composer__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}
.composer__counter { color: var(--on-surface-variant); }
.composer__counter--bad { color: var(--error); }
.composer__error { color: var(--error); }

.empty { color: var(--on-surface-variant); margin: 0; }

.list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.item {
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.item__head {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}
.item__user {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--on-surface);
  text-decoration: none;
}
.item__user:hover { text-decoration: underline; }
.item__name { font-weight: 600; }
.item__time { margin-left: 0; }
.item__delete {
  margin-left: auto;
  appearance: none;
  background: transparent;
  border: 0;
  color: var(--on-surface-variant);
  cursor: pointer;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 0.9rem;
  line-height: 1;
  transition: background-color 120ms ease, color 120ms ease;
}
.item__delete:hover {
  color: var(--on-error-container);
  background: var(--error-container);
}
.item__body {
  margin: 0;
  white-space: pre-wrap;
  color: var(--on-surface);
}

.banner {
  padding: var(--space-sm) var(--space-md);
  background: var(--error-container);
  color: var(--on-error-container);
  border-radius: var(--radius);
  margin: 0;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
}

@media (max-width: 640px) {
  .item__head {
    flex-wrap: wrap;
  }
  .item__delete {
    margin-left: auto;
  }
}
</style>
