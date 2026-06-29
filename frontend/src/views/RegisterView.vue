<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/authentication-controller/authentication-controller'
import { useAuthStore } from '@/stores/auth'
import { errorMessage, firstError, validationFieldErrors } from '@/lib/errors'
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
    const res = await register({ username: username.value, password: password.value })
    auth.markAuthenticated(res.roles ?? [])
    router.push('/app')
  } catch (e) {
    const first = firstError(e)
    const code = first?.code
    const msg = errorMessage(e)
    if (code === 'USERNAME_ALREADY_TAKEN' || code === 'INVALID_USERNAME') {
      usernameError.value = msg
    } else if (code === 'INVALID_PASSWORD') {
      passwordError.value = msg
    } else {
      // Bean-validation responses (VALIDATION_ERROR) carry field names.
      const fieldErrors = validationFieldErrors(e)
      if (fieldErrors.username) usernameError.value = fieldErrors.username
      if (fieldErrors.password) passwordError.value = fieldErrors.password
      if (!fieldErrors.username && !fieldErrors.password) errorText.value = msg
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <Card>
      <header class="auth-head">
        <RouterLink to="/" class="brand-link" aria-label="67quiz home"><BrandMark size="md" /></RouterLink>
        <h1 class="headline-lg auth-head__title">Create account</h1>
      </header>
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
          autocomplete="new-password"
          :error="passwordError ?? undefined"
        />
        <p v-if="errorText" class="form__error label-md" role="alert">{{ errorText }}</p>
        <Button type="submit" :loading="submitting" full-width>Create account</Button>
      </form>
      <p class="footnote body-md">
        Already have one? <RouterLink to="/login">Sign in</RouterLink>
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
.auth-head {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}
.auth-head .brand-link {
  margin-bottom: 0;
  align-self: center;
}
.auth-head__title {
  margin: 0;
  text-align: left;
}
.footnote {
  margin-top: var(--space-lg);
  color: var(--on-surface-variant);
}
</style>
