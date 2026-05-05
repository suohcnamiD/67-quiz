import { defineConfig } from 'orval';

export default defineConfig({
  main: {
    input: './../api/openapi.json',
    output: {
      mode: 'tags-split',
      target: './src/api/openAPIDefinition.ts',
      client: 'vue-query',
      httpClient: 'axios',
      override: {
        mutator: {
          path: './src/lib/axios.ts',
          name: 'customInstance',
        },
      },
    },
  }
})
