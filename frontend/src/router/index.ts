import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    /** true の場合、未ログインでもアクセスできる（ログイン画面など）。 */
    public?: boolean
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
    },
  ],
})

/**
 * 認証ガード。初回遷移時のみ `/me` でログイン状態を確認し、以降はストアの状態を使い回す。
 * 未ログインで非公開ページに来た場合は `/login` へ、ログイン済みで `/login` に来た場合は `/` へ流す。
 */
router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!authStore.isInitialized) {
    await authStore.fetchMe()
  }

  if (!to.meta.public && !authStore.isAuthenticated) {
    return { name: 'login' }
  }
  if (to.name === 'login' && authStore.isAuthenticated) {
    return { name: 'home' }
  }
  return true
})

export default router
