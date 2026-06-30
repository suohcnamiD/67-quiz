<script setup lang="ts">
import Button from '@/components/Button.vue'
import BrandMark from '@/components/BrandMark.vue'
import ShaderBackground from '@/components/ShaderBackground.vue'
</script>

<template>
  <div class="page">
    <ShaderBackground />
    <main class="center">
      <BrandMark as="h1" size="lg" />
      <p class="tagline body-lg muted">
        Compete against others and prove your knowledge in exciting quizzes.
      </p>
      <div class="ctas">
        <RouterLink to="/register">
          <Button>Get started</Button>
        </RouterLink>
        <RouterLink to="/login">
          <Button variant="ghost">Sign in</Button>
        </RouterLink>
      </div>
    </main>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #050000;
}
.center {
  position: relative;
  z-index: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-xl) var(--margin-mobile);
  max-width: 36rem;
}
.tagline {
  margin: 0;
  color: var(--on-surface-variant);
}
.ctas {
  display: flex;
  gap: var(--space-md);
  flex-wrap: wrap;
  justify-content: center;
  margin-top: var(--space-sm);
}
.ctas :deep(button) {
  min-width: 9rem;
}
</style>
