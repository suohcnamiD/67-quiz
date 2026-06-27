<script setup lang="ts">
import Button from '@/components/Button.vue'
import Card from '@/components/Card.vue'
</script>

<template>
  <div class="page">
    <main class="wrap">
      <section class="hero">
        <h1 class="hero__brand">67quiz</h1>
        <p class="hero__tagline body-lg muted">
          Author quizzes, share them, and see how your friends score.
        </p>
        <div class="hero__ctas">
          <RouterLink to="/register" class="cta">
            <Button>Get started</Button>
          </RouterLink>
          <RouterLink to="/login" class="cta">
            <Button variant="ghost">Sign in</Button>
          </RouterLink>
        </div>
      </section>

      <section class="grid">
        <Card class="feature">
          <h2 class="feature__title">Single &amp; multi choice</h2>
          <p class="body-md muted feature__body">
            Mix tighter "pick one" questions with broader "select all that apply"
            ones in the same quiz.
          </p>
        </Card>
        <Card class="feature">
          <h2 class="feature__title">Per-option scoring</h2>
          <p class="body-md muted feature__body">
            Multi-choice questions reward every correctly classified option, so
            partial answers still pick up points.
          </p>
        </Card>
        <Card class="feature">
          <h2 class="feature__title">Profiles &amp; browsing</h2>
          <p class="body-md muted feature__body">
            Every author gets a profile page listing their quizzes. Search for
            people or topics and dive in.
          </p>
        </Card>
      </section>
    </main>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: var(--space-xl) var(--margin-mobile);
  display: flex;
  align-items: center;
  justify-content: center;
}
.wrap {
  width: 100%;
  max-width: 64rem;
  display: flex;
  flex-direction: column;
  gap: var(--space-xxl, var(--space-xl));
}
.hero {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
}
.hero__brand {
  margin: 0;
  font-size: clamp(2.75rem, 8vw, 4.5rem);
  font-weight: 900;
  letter-spacing: -0.03em;
  line-height: 1.05;
  padding-bottom: 0.1em;
  color: var(--on-surface);
}
.hero__tagline {
  margin: 0;
  max-width: 36rem;
}
.hero__ctas {
  display: flex;
  gap: var(--space-md);
  flex-wrap: wrap;
  justify-content: center;
  margin-top: var(--space-sm);
}
.cta :deep(button) {
  min-width: 9rem;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-md);
}
@media (max-width: 720px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
.feature {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.feature__title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--on-surface);
}
.feature__body {
  margin: 0;
}
.muted {
  color: var(--on-surface-variant);
}
</style>
