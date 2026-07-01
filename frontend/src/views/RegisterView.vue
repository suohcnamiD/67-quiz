<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { register } from '@/api/authentication-controller/authentication-controller'
import { useAuthStore } from '@/stores/auth'
import { errorMessage, firstError, validationFieldErrors } from '@/lib/errors'
import Button from '@/components/Button.vue'
import Input from '@/components/Input.vue'
import Card from '@/components/Card.vue'
import BrandMark from '@/components/BrandMark.vue'
import ShaderBackground from '@/components/ShaderBackground.vue'

const username = ref('')
const password = ref('')
const errorText = ref<string | null>(null)
const usernameError = ref<string | null>(null)
const passwordError = ref<string | null>(null)
const submitting = ref(false)
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

// Same-origin next-URL guard as LoginView; keeps the "start a quiz while
// logged out" flow intact when the user registers a new account instead
// of signing in.
function safeNext(): string {
  const raw = route.query.next
  if (typeof raw !== 'string') return '/app'
  if (!raw.startsWith('/') || raw.startsWith('//')) return '/app'
  return raw
}

// Mirrors the server-side rules in RegistrationService — the FE shows green
// checks live so the user sees the requirements fulfilled as they type.
// Source of truth is still the backend; if these drift the server will reject
// the registration and the existing error path will surface the message.
const usernameRules = computed(() => [
  { ok: username.value.length >= 5, label: 'At least 5 characters' },
  { ok: username.value.length <= 16, label: 'At most 16 characters' },
  { ok: /^[a-zA-Z]/.test(username.value), label: 'Starts with a letter' },
  { ok: /^[a-zA-Z0-9_]*$/.test(username.value) && username.value.length > 0, label: 'Only letters, numbers, underscore' },
])
const passwordRules = computed(() => [
  { ok: password.value.length >= 8, label: 'At least 8 characters' },
])

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
    router.push(safeNext())
  } catch (e) {
    const first = firstError(e)
    const code = first?.code
    const msg = errorMessage(e)
    if (code === 'USERNAME_ALREADY_TAKEN' || code === 'INVALID_USERNAME') {
      usernameError.value = msg
    } else if (code === 'INVALID_PASSWORD') {
      passwordError.value = msg
    } else {
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
    <ShaderBackground />
    <Card>
      <header class="auth-head">
        <RouterLink to="/" class="brand-link" aria-label="67quiz home"><BrandMark size="md" /></RouterLink>
        <h1 class="headline-lg auth-head__title">Create account</h1>
      </header>
      <form class="form" @submit.prevent="submit">
        <div class="field-block">
          <Input
            v-model="username"
            label="Username"
            autocomplete="username"
            :error="usernameError ?? undefined"
          />
          <ul class="rules" aria-label="Username requirements">
            <li v-for="(rule, i) in usernameRules" :key="`u${i}`" :class="['rule', { 'rule--ok': rule.ok }]">
              <span aria-hidden="true" class="rule__mark">{{ rule.ok ? '✓' : '✕' }}</span>
              <span class="rule__label body-md">{{ rule.label }}</span>
            </li>
          </ul>
        </div>
        <div class="field-block">
          <Input
            v-model="password"
            label="Password"
            type="password"
            autocomplete="new-password"
            :error="passwordError ?? undefined"
          />
          <ul class="rules" aria-label="Password requirements">
            <li v-for="(rule, i) in passwordRules" :key="`p${i}`" :class="['rule', { 'rule--ok': rule.ok }]">
              <span aria-hidden="true" class="rule__mark">{{ rule.ok ? '✓' : '✕' }}</span>
              <span class="rule__label body-md">{{ rule.label }}</span>
            </li>
          </ul>
        </div>
        <p v-if="errorText" class="form__error label-md" role="alert">{{ errorText }}</p>
        <Button type="submit" :loading="submitting" full-width>Create account</Button>
      </form>
      <p class="footnote body-md">
        Already have one? <RouterLink :to="{ path: '/login', query: route.query.next ? { next: route.query.next } : {} }">Sign in</RouterLink>
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
  background: #050000;
}
.page > :deep(.card) {
  width: 100%;
  max-width: 420px;
  position: relative;
  z-index: 1;
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
.field-block {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.rules {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.rule {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--on-error-container);
  font-size: 0.875rem;
  transition: color 120ms ease;
}
.rule__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  font-weight: 700;
  font-size: 0.75rem;
  background: var(--on-error-container);
  color: #000;
  border: 0;
  transition: color 120ms ease, background-color 120ms ease;
}
.rule--ok {
  color: var(--on-secondary-container);
}
.rule--ok .rule__mark {
  background: var(--on-secondary-container);
  color: #000;
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
