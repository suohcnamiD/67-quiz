<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/authentication-controller/authentication-controller'
import { useAuthStore } from '@/stores/auth'
import { errorMessage, firstErrorCode, validationFieldErrors } from '@/lib/errors'
import Button from '@/components/Button.vue'
import Input from '@/components/Input.vue'
import Card from '@/components/Card.vue'
import BrandMark from '@/components/BrandMark.vue'

const username = ref('')
const password = ref('')
const errorText = ref<string | null>(null)
const usernameError = ref<string | null>(null)
const passwordError = ref<string | null>(null)
const submitting = ref(false)
const auth = useAuthStore()
const router = useRouter()

function clearErrors() {
  errorText.value = null
  usernameError.value = null
  passwordError.value = null
}

async function submit() {
  clearErrors()
  submitting.value = true
  try {
    const res = await login({ username: username.value, password: password.value })
    auth.markAuthenticated(res.roles ?? [])
    router.push('/app')
  } catch (e) {
    const code = firstErrorCode(e)
    if (code === 'UNAUTHORIZED') {
      errorText.value = errorMessage(e)
    } else {
      const fieldErrors = validationFieldErrors(e)
      if (fieldErrors.username) usernameError.value = fieldErrors.username
      if (fieldErrors.password) passwordError.value = fieldErrors.password
      if (!fieldErrors.username && !fieldErrors.password) errorText.value = errorMessage(e)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <Card>
      <RouterLink to="/" class="brand-link" aria-label="67quiz home"><BrandMark size="md" /></RouterLink>
      <h1 class="headline-lg">Sign in</h1>
      <form class="form" @submit.prevent="submit">
        <Input
          v-model="username"
          label="Username"
          autocomplete="username"
          :error="usernameError ?? undefined"
        />
        <Input
          v-model="password"
          label="Password"
          type="password"
          autocomplete="current-password"
          :error="passwordError ?? undefined"
        />
        <p v-if="errorText" class="form__error label-md" role="alert">{{ errorText }}</p>
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
  margin-top: var(--space-lg);
}
.form__error {
  color: var(--error);
}
.brand-link {
  display: inline-block;
  margin-bottom: var(--space-md);
  text-decoration: none;
  opacity: 0.85;
  transition: opacity 120ms ease;
}
.brand-link:hover { opacity: 1; }
.footnote {
  margin-top: var(--space-lg);
  color: var(--on-surface-variant);
}
</style>
