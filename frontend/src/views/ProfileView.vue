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
import { scrollAndFlash } from '@/lib/scrollAndFlash'
import { confirmDialog } from '@/lib/confirmDialog'
import Button from '@/components/Button.vue'
import Input from '@/components/Input.vue'
import Avatar from '@/components/Avatar.vue'
import QuizCard from '@/components/QuizCard.vue'
import Modal from '@/components/Modal.vue'

const router = useRouter()
const qc = useQueryClient()
const auth = useAuthStore()
const { data, isPending } = useGetOwnProfile()
const profile = computed(() => data.value)

// Quizzes I authored, fetched once the profile resolves (we need my username).
const myUsername = computed(() => data.value?.username ?? '')
const authoredQuizzes = useGetQuizzesByAuthor(myUsername, computed(() => ({ page: 0 })), {
  query: { enabled: computed(() => !!myUsername.value) },
})
const authored = computed(() => authoredQuizzes.data.value?._embedded?.quizzes ?? [])
const authoredError = ref<string | null>(null)
const errorText = ref<string | null>(null)

// --- Details modal (display name + bio) ---------------------------------
const detailsOpen = ref(false)
const detailsName = ref('')
const detailsBio = ref('')
const savingDetails = ref(false)
const detailsError = ref<string | null>(null)

function openDetailsModal() {
  detailsName.value = profile.value?.displayName ?? profile.value?.username ?? ''
  detailsBio.value = profile.value?.bio ?? ''
  detailsError.value = null
  detailsOpen.value = true
}

async function saveDetails() {
  detailsError.value = null
  savingDetails.value = true
  try {
    const updated = await updateOwnProfile({
      displayName: detailsName.value.trim(),
      bio: detailsBio.value,
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
    detailsOpen.value = false
  } catch (e) {
    detailsError.value = errorMessage(e)
  } finally {
    savingDetails.value = false
  }
}

// --- Avatar modal --------------------------------------------------------
const avatarOpen = ref(false)
const avatarError = ref<string | null>(null)
const uploadingAvatar = ref(false)
const removingAvatar = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

function openAvatarModal() {
  avatarError.value = null
  avatarOpen.value = true
}

function pickAvatar() {
  fileInput.value?.click()
}

async function onFilePicked(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  avatarError.value = null
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
    // Close the modal on success so the user sees their new avatar on the hero.
    avatarOpen.value = false
  } catch (e) {
    avatarError.value = errorMessage(e)
  } finally {
    uploadingAvatar.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

async function removeAvatar() {
  const ok = await confirmDialog.open({
    title: 'Remove your avatar?',
    body: 'You can upload a new one any time.',
    confirmLabel: 'Remove',
    danger: true,
  })
  if (!ok) return
  avatarError.value = null
  removingAvatar.value = true
  try {
    const updated = await deleteAvatar()
    auth.applyProfileSnapshot({ hasAvatar: updated.hasAvatar })
    auth.bumpAvatarVersion()
    qc.setQueryData(getGetOwnProfileQueryKey(), updated)
    if (updated.username) {
      qc.invalidateQueries({ queryKey: getGetProfileByUsernameQueryKey(updated.username) })
    }
    avatarOpen.value = false
  } catch (e) {
    avatarError.value = errorMessage(e)
  } finally {
    removingAvatar.value = false
  }
}

// When the user opens the avatar modal via keyboard (Enter on the avatar
// button), focus should land on the upload action; the dialog itself takes
// focus first, that's fine.
watch(detailsOpen, (open) => {
  if (!open) detailsError.value = null
})
watch(avatarOpen, (open) => {
  if (!open) avatarError.value = null
})

function scrollToAuthored() {
  scrollAndFlash('authored-section')
}
</script>

<template>
  <div v-if="isPending" class="empty body-md">Loading…</div>
  <template v-else-if="profile">
    <header class="head">
      <button
        type="button"
        class="avatar-button"
        :aria-label="profile.hasAvatar ? 'Change avatar' : 'Upload avatar'"
        @click="openAvatarModal"
      >
        <Avatar
          :username="profile.username"
          :display-name="profile.displayName"
          :version="auth.avatarVersion"
          :initials-only="!profile.hasAvatar"
          :size="96"
        />
        <span class="avatar-button__overlay" aria-hidden="true">Edit</span>
      </button>
      <div class="head__main">
        <h1 class="head__name">{{ profile.displayName ?? profile.username }}</h1>
        <p class="head__handle muted">@{{ profile.username }}</p>
        <p v-if="profile.bio" class="head__bio body-md">{{ profile.bio }}</p>
      </div>
      <div class="head__actions">
        <Button variant="ghost" @click="openDetailsModal">Edit profile</Button>
      </div>
    </header>

    <section class="stats" aria-label="Profile statistics">
      <button
        type="button"
        class="stat stat--link"
        :disabled="(profile.quizzesAuthored ?? 0) === 0"
        :aria-label="`${profile.quizzesAuthored ?? 0} quizzes authored — jump to list`"
        @click="scrollToAuthored"
      >
        <span class="stat__value">{{ profile.quizzesAuthored ?? 0 }}</span>
        <span class="stat__label label-sm">Quizzes authored</span>
      </button>
      <button
        type="button"
        class="stat stat--link"
        :disabled="(profile.attemptsTaken ?? 0) === 0"
        :aria-label="`${profile.attemptsTaken ?? 0} attempts taken — jump to past results`"
        @click="router.push({ path: '/app', hash: '#past-results' })"
      >
        <span class="stat__value">{{ profile.attemptsTaken ?? 0 }}</span>
        <span class="stat__label label-sm">Attempts taken</span>
      </button>
      <div class="stat">
        <span class="stat__value">
          {{ profile.averageScorePercent != null ? `${profile.averageScorePercent}%` : '—' }}
        </span>
        <span class="stat__label label-sm">Average score</span>
      </div>
    </section>

    <section id="authored-section" class="authored" aria-labelledby="my-quizzes-heading">
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

    <div class="bottom-actions">
      <Button variant="ghost" @click="router.push('/app')">Back to browse</Button>
    </div>

    <!-- Modals — only one open at a time. -->
    <Modal :open="detailsOpen" title="Edit profile" @close="detailsOpen = false">
      <form id="details-form" class="form" @submit.prevent="saveDetails">
        <Input v-model="detailsName" label="Display name" />
        <Input v-model="detailsBio" label="Bio" placeholder="Short blurb about you" />
        <p v-if="detailsError" class="banner label-md" role="alert">{{ detailsError }}</p>
      </form>
      <template #footer>
        <Button variant="ghost" @click="detailsOpen = false">Cancel</Button>
        <Button type="submit" form="details-form" :loading="savingDetails">Save</Button>
      </template>
    </Modal>

    <Modal :open="avatarOpen" title="Change avatar" @close="avatarOpen = false">
      <div class="avatar-modal">
        <Avatar
          :username="profile.username"
          :display-name="profile.displayName"
          :version="auth.avatarVersion"
          :initials-only="!profile.hasAvatar"
          :size="120"
        />
        <p class="muted body-md">
          Pick a PNG, JPEG or WebP up to 2 MB. We'll crop it to a square and
          store a 256×256 copy.
        </p>
        <p v-if="avatarError" class="banner label-md" role="alert">{{ avatarError }}</p>
        <input
          ref="fileInput"
          type="file"
          accept="image/png,image/jpeg,image/webp"
          class="file-input"
          @change="onFilePicked"
        />
      </div>
      <template #footer>
        <Button
          v-if="profile.hasAvatar"
          variant="danger"
          :loading="removingAvatar"
          @click="removeAvatar"
        >Remove</Button>
        <Button variant="ghost" @click="avatarOpen = false">Cancel</Button>
        <Button :loading="uploadingAvatar" @click="pickAvatar">
          {{ profile.hasAvatar ? 'Replace…' : 'Upload…' }}
        </Button>
      </template>
    </Modal>
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
  flex: 1;
  min-width: 0;
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
.head__actions {
  display: flex;
  gap: var(--space-sm);
}

/* Clickable avatar with a hover-revealed "Edit" overlay. */
.avatar-button {
  position: relative;
  appearance: none;
  background: transparent;
  border: 0;
  padding: 0;
  border-radius: 50%;
  cursor: pointer;
  line-height: 0;
}
.avatar-button:hover .avatar-button__overlay,
.avatar-button:focus-visible .avatar-button__overlay {
  opacity: 1;
}
.avatar-button__overlay {
  position: absolute;
  inset: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-weight: 700;
  font-size: 0.85rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  opacity: 0;
  transition: opacity 120ms ease;
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
  text-align: left;
}
.stat--link {
  appearance: none;
  font: inherit;
  color: inherit;
  cursor: pointer;
  transition: border-color 120ms ease, background-color 120ms ease, transform 120ms ease;
}
.stat--link:not(:disabled):hover {
  border-color: var(--outline);
  background: var(--surface-container-high);
}
.stat--link:not(:disabled):active {
  transform: translateY(1px);
}
.stat--link:disabled {
  cursor: default;
  opacity: 0.85;
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

.bottom-actions {
  margin-top: var(--space-lg);
}

/* Modal form layout */
.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.avatar-modal {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
  text-align: center;
}
.avatar-modal .muted {
  max-width: 28rem;
}
.file-input {
  display: none;
}

@media (max-width: 640px) {
  .stats {
    grid-template-columns: 1fr;
  }
  .head {
    flex-direction: column;
    align-items: flex-start;
  }
  .head__actions {
    width: 100%;
  }
}
</style>
