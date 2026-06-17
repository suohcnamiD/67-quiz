<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/authentication-controller/authentication-controller'
import { useAuthStore } from '@/stores/auth'
import { firstErrorCode } from '@/lib/axios'
import Button from '@/components/Button.vue'
import Input from '@/components/Input.vue'
import Card from '@/components/Card.vue'

const username = ref('')
const password = ref('')
const errorMessage = ref<string | null>(null)
const submitting = ref(false)
const auth = useAuthStore()
const router = useRouter()

async function submit() {
  errorMessage.value = null
  submitting.value = true
  try {
    const res = await login({ username: username.value, password: password.value })
    auth.markAuthenticated(res.roles ?? [])
    router.push('/app')
  } catch (e) {
    const code = firstErrorCode(e)
    errorMessage.value = code === 'UNAUTHORIZED' ? 'Wrong username or password.' : 'Could not sign in.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <Card>
      <h1 class="headline-lg">Sign in</h1>
      <p class="subtitle body-md">Resume your run.</p>
      <form class="form" @submit.prevent="submit">
        <Input v-model="username" label="Username" autocomplete="username" />
        <Input v-model="password" label="Password" type="password" autocomplete="current-password" />
        <p v-if="errorMessage" class="form__error label-md">{{ errorMessage }}</p>
        <Button type="submit" :loading="submitting" full-width>Sign in</Button>
      </form>
      <p class="footnote body-md">
        No account? <RouterLink to="/register">Create one</RouterLink>
      </p>
    </Card>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: var(--space-xl) var(--margin-mobile);
}
.page > :deep(.card) {
  width: 100%;
  max-width: 420px;
}
.subtitle {
  color: var(--on-surface-variant);
  margin: var(--space-xs) 0 var(--space-lg);
}
.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.form__error {
  color: var(--error);
}
.footnote {
  margin-top: var(--space-lg);
  color: var(--on-surface-variant);
}
</style>
