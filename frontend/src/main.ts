import { createApp } from 'vue'
import { createPinia } from 'pinia'
// ポップで今風な丸ゴシック（ヘッダー等の見出し用）。self-hosted のため外部依存なし。
import '@fontsource/m-plus-rounded-1c/500.css'
import '@fontsource/m-plus-rounded-1c/700.css'
import '@fontsource/m-plus-rounded-1c/800.css'
import './style.css'
import App from './App.vue'
import router from './router'
import vuetify from './plugins/vuetify'
import { primeCsrfToken } from './lib/httpClient'

// 最初の状態変更リクエストの前に CSRF トークン Cookie を発行させておく。
// 失敗してもアプリ起動はブロックしない。
primeCsrfToken().finally(() => {
  createApp(App).use(createPinia()).use(router).use(vuetify).mount('#app')
})
