<script setup lang="ts">
  import { useLogin } from '@/api/authentication-controller/authentication-controller';
import { onMounted } from 'vue';
  const { data } = useLogin();
  onMounted(() => {
    console.log(data);
  })
</script>

<template>

</template>

<style scoped>

</style>
