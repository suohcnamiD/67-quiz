<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/authentication-controller/authentication-controller'
import { useAuthStore } from '@/stores/auth'
import { errorMessage } from '@/lib/errors'
import Button from '@/components/Button.vue'
import Input from '@/components/Input.vue'
import Card from '@/components/Card.vue'

const username = ref('')
const password = ref('')
const errorText = ref<string | null>(null)
const submitting = ref(false)
const auth = useAuthStore()
const router = useRouter()

async function submit() {
  errorText.value = null
  submitting.value = true
  try {
    const res = await register({ username: username.value, password: password.value })
    auth.markAuthenticated(res.roles ?? [])
    router.push('/app')
  } catch (e) {
    errorText.value = errorMessage(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <Card>
      <h1 class="headline-lg">Create account</h1>
      <form class="form" @submit.prevent="submit">
        <Input v-model="username" label="Username" autocomplete="username" />
        <Input v-model="password" label="Password" type="password" autocomplete="new-password" />
        <p v-if="errorText" class="form__error label-md">{{ errorText }}</p>
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
.footnote {
  margin-top: var(--space-lg);
  color: var(--on-surface-variant);
}
</style>
