<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'
import {
  useGetOwnProfile,
  useGetQuizzesByAuthor,
  updateOwnProfile,
  uploadAvatar,
  deleteAvatar,
  getGetOwnProfileQueryKey,
  getGetProfileByUsernameQueryKey,
} from '@/api/user-profile-controller/user-profile-controller'
import { useAuthStore } from '@/stores/auth'
import { errorMessage } from '@/lib/errors'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import Input from '@/components/Input.vue'
import Avatar from '@/components/Avatar.vue'
import QuizCard from '@/components/QuizCard.vue'

const router = useRouter()
const qc = useQueryClient()
const auth = useAuthStore()
const { data, isPending } = useGetOwnProfile()

// Quizzes I authored, fetched once the profile resolves (we need my username).
const myUsername = computed(() => data.value?.username ?? '')
const authoredQuizzes = useGetQuizzesByAuthor(myUsername, computed(() => ({ page: 0 })), {
  query: { enabled: computed(() => !!myUsername.value) },
})
const authored = computed(() => authoredQuizzes.data.value?._embedded?.quizzes ?? [])
const authoredError = ref<string | null>(null)

const displayName = ref('')
const bio = ref('')

// Seed the form whenever fresh profile data arrives. We don't want to clobber
// in-flight edits, so only run when the form fields are still empty.
watch(
  data,
  (snapshot) => {
    if (!snapshot) return
    if (!displayName.value) displayName.value = snapshot.displayName ?? ''
    if (!bio.value) bio.value = snapshot.bio ?? ''
  },
  { immediate: true },
)

const errorText = ref<string | null>(null)
const savingProfile = ref(false)
async function saveProfile() {
  errorText.value = null
  savingProfile.value = true
  try {
    const updated = await updateOwnProfile({
      displayName: displayName.value.trim(),
      bio: bio.value,
    })
    auth.applyProfileSnapshot({
      username: updated.username,
      displayName: updated.displayName,
      hasAvatar: updated.hasAvatar,
    })
    qc.setQueryData(getGetOwnProfileQueryKey(), updated)
    if (updated.username) {
      qc.invalidateQueries({ queryKey: getGetProfileByUsernameQueryKey(updated.username) })
    }
  } catch (e) {
    errorText.value = errorMessage(e)
  } finally {
    savingProfile.value = false
  }
}

const fileInput = ref<HTMLInputElement | null>(null)
const uploadingAvatar = ref(false)
async function pickAvatar() {
  fileInput.value?.click()
}
async function onFilePicked(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  errorText.value = null
  uploadingAvatar.value = true
  try {
    const updated = await uploadAvatar({ file })
    auth.applyProfileSnapshot({
      hasAvatar: updated.hasAvatar,
      displayName: updated.displayName,
      username: updated.username,
    })
    auth.bumpAvatarVersion()
    qc.setQueryData(getGetOwnProfileQueryKey(), updated)
    if (updated.username) {
      qc.invalidateQueries({ queryKey: getGetProfileByUsernameQueryKey(updated.username) })
    }
  } catch (e) {
    errorText.value = errorMessage(e)
  } finally {
    uploadingAvatar.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

const removingAvatar = ref(false)
async function removeAvatar() {
  if (!confirm('Remove your avatar?')) return
  errorText.value = null
  removingAvatar.value = true
  try {
    const updated = await deleteAvatar()
    auth.applyProfileSnapshot({ hasAvatar: updated.hasAvatar })
    auth.bumpAvatarVersion()
    qc.setQueryData(getGetOwnProfileQueryKey(), updated)
    if (updated.username) {
      qc.invalidateQueries({ queryKey: getGetProfileByUsernameQueryKey(updated.username) })
    }
  } catch (e) {
    errorText.value = errorMessage(e)
  } finally {
    removingAvatar.value = false
  }
}

const profile = computed(() => data.value)
</script>

<template>
  <div v-if="isPending" class="empty body-md">Loading…</div>
  <template v-else-if="profile">
    <header class="head">
      <Avatar
        :username="profile.username"
        :display-name="profile.displayName"
        :version="auth.avatarVersion"
        :initials-only="!profile.hasAvatar"
        :size="96"
      />
      <div class="head__main">
        <h1 class="head__name">{{ profile.displayName ?? profile.username }}</h1>
        <p class="head__handle muted">@{{ profile.username }}</p>
        <p v-if="profile.bio" class="head__bio body-md">{{ profile.bio }}</p>
      </div>
    </header>

    <section class="stats" aria-label="Profile statistics">
      <div class="stat">
        <span class="stat__value">{{ profile.quizzesAuthored ?? 0 }}</span>
        <span class="stat__label label-sm">Quizzes authored</span>
      </div>
      <div class="stat">
        <span class="stat__value">{{ profile.attemptsTaken ?? 0 }}</span>
        <span class="stat__label label-sm">Attempts taken</span>
      </div>
      <div class="stat">
        <span class="stat__value">
          {{ profile.averageScorePercent != null ? `${profile.averageScorePercent}%` : '—' }}
        </span>
        <span class="stat__label label-sm">Average score</span>
      </div>
    </section>

    <section class="authored" aria-labelledby="my-quizzes-heading">
      <header class="authored__head">
        <h2 id="my-quizzes-heading" class="headline-md">Your quizzes</h2>
        <Button variant="ghost" @click="router.push('/app/quiz/new')">+ New quiz</Button>
      </header>
      <p v-if="authoredError" class="banner label-md" role="alert">{{ authoredError }}</p>
      <p v-if="authoredQuizzes.isLoading.value" class="empty body-md">Loading…</p>
      <p v-else-if="!authored.length" class="empty body-md">
        You haven't authored any quizzes yet — start one.
      </p>
      <div v-else class="grid">
        <QuizCard
          v-for="q in authored"
          :key="q.id"
          :quiz="q"
          :show-author="false"
          @error="authoredError = $event"
        />
      </div>
    </section>

    <p v-if="errorText" class="banner label-md" role="alert">{{ errorText }}</p>

    <Card class="edit">
      <h2 class="edit__title">Edit profile</h2>
      <form class="form" @submit.prevent="saveProfile">
        <Input v-model="displayName" label="Display name" />
        <Input v-model="bio" label="Bio" placeholder="Short blurb about you" />
        <div class="form__actions">
          <Button type="submit" :loading="savingProfile">Save</Button>
        </div>
      </form>
    </Card>

    <Card class="edit">
      <h2 class="edit__title">Avatar</h2>
      <p class="muted body-md">
        Pick a PNG, JPEG or WebP up to 2 MB. We'll crop it to a square and store
        a 256×256 copy.
      </p>
      <div class="avatar-actions">
        <Button :loading="uploadingAvatar" @click="pickAvatar">
          {{ profile.hasAvatar ? 'Replace avatar' : 'Upload avatar' }}
        </Button>
        <Button
          v-if="profile.hasAvatar"
          variant="ghost"
          :loading="removingAvatar"
          @click="removeAvatar"
        >Remove</Button>
      </div>
      <input
        ref="fileInput"
        type="file"
        accept="image/png,image/jpeg,image/webp"
        class="file-input"
        @change="onFilePicked"
      />
    </Card>

    <div class="bottom-actions">
      <Button variant="ghost" @click="router.push('/app')">Back to browse</Button>
    </div>
  </template>
</template>

<style scoped>
.empty {
  color: var(--on-surface-variant);
}
.muted {
  color: var(--on-surface-variant);
}
.head {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  margin-bottom: var(--space-xl);
}
.head__main {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.head__name {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 800;
  letter-spacing: -0.01em;
  color: var(--on-surface);
}
.head__handle {
  margin: 0;
  font-variant-numeric: tabular-nums;
}
.head__bio {
  margin: var(--space-xs) 0 0;
  color: var(--on-surface-variant);
  max-width: 36rem;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-md);
  margin-bottom: var(--space-xl);
}
.stat {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  padding: var(--space-md) var(--space-lg);
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
}
.stat__value {
  font-size: 2rem;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--on-surface);
  line-height: 1;
}
.stat__label {
  color: var(--on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.banner {
  margin: 0 0 var(--space-md);
  padding: var(--space-sm) var(--space-md);
  background: var(--error-container);
  color: var(--on-error-container);
  border-radius: var(--radius);
}

.authored {
  margin-bottom: var(--space-xl);
}
.authored__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-md);
}

.edit {
  margin-bottom: var(--space-md);
}
.edit__title {
  margin: 0 0 var(--space-md);
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--on-surface);
}
.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.form__actions {
  display: flex;
  justify-content: flex-end;
}
.avatar-actions {
  display: flex;
  gap: var(--space-sm);
  margin-top: var(--space-md);
}
.file-input {
  display: none;
}
.bottom-actions {
  margin-top: var(--space-lg);
}

@media (max-width: 640px) {
  .stats {
    grid-template-columns: 1fr;
  }
  .head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
