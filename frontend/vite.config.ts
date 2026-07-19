/// <reference types="vitest/config" />
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vuetify from 'vite-plugin-vuetify'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vuetify({ autoImport: true })],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      // バックエンド（Spring Boot, :8080）へプロキシする。
      // ブラウザからは常に同一オリジン（localhost:5173）に見えるため、
      // Cookie ベース認証で SameSite=Lax の制約に引っかからない。
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    server: {
      // Vuetify コンポーネントが内部で import する .css を Vite の変換経由で読ませる。
      // 外部化（Node の素の ESM ローダーで直接 require/import）されると
      // 「Unknown file extension ".css"」で失敗するため。
      deps: {
        inline: ['vuetify'],
      },
    },
  },
})
