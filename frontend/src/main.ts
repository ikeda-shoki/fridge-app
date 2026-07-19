import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import { createVuetify } from 'vuetify'
import './style.css'
import App from './App.vue'
import router from './router'

const vuetify = createVuetify({
  icons: {
    defaultSet: 'mdi',
  },
})

createApp(App).use(createPinia()).use(router).use(vuetify).mount('#app')
