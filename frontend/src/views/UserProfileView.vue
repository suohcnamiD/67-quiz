<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGetProfileByUsername } from '@/api/user-profile-controller/user-profile-controller'
import { useAuthStore } from '@/stores/auth'
import Card from '@/components/Card.vue'
import Button from '@/components/Button.vue'
import Avatar from '@/components/Avatar.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const username = computed(() => route.params.username as string)

const { data, isPending, isError } = useGetProfileByUsername(username)
const profile = computed(() => data.value)
</script>

<template>
  <div v-if="isPending" class="empty body-md">Loading…</div>
  <Card v-else-if="isError || !profile" class="notfound">
    <h1 class="headline-md">User not found</h1>
    <p class="body-md muted">No user named @{{ username }} here.</p>
    <Button @click="router.push('/app')">Back to browse</Button>
  </Card>
  <template v-else>
    <header class="head">
      <Avatar
        :username="profile.username"
        :display-name="profile.displayName"
        :version="profile.isYou ? auth.avatarVersion : 0"
        :initials-only="!profile.hasAvatar"
        :size="96"
      />
      <div class="head__main">
        <h1 class="head__name">{{ profile.displayName ?? profile.username }}</h1>
        <p class="head__handle muted">@{{ profile.username }}</p>
        <p v-if="profile.bio" class="head__bio body-md">{{ profile.bio }}</p>
      </div>
      <div v-if="profile.isYou" class="head__you">
        <Button variant="ghost" @click="router.push('/app/profile')">This is you — edit profile</Button>
      </div>
    </header>

    <section class="stats">
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
.notfound {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  align-items: flex-start;
}
.notfound h1 {
  margin: 0;
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
