import { defineConfig } from 'orval';

export default defineConfig({
  main: {
    input: './../api/openapi.json',
    output: './src/api/openAPIDefinition.ts'
  }
})
