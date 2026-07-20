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
      path: '/guidance',
      name: 'guidance',
      component: () => import('@/views/GroupGuidanceView.vue'),
    },
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
    },
  ],
})

/**
 * 認証・グループ所属ガード。初回遷移時のみ `/me` でログイン状態を確認し、以降はストアの状態を使い回す。
 *
 * - 未ログインで非公開ページに来た場合は `/login` へ
 * - ログイン済みだがどのグループにも所属していない場合は `/guidance` へ（GRP-00）
 * - ログイン済みかつグループ所属済みの場合は `/login` `/guidance` から `/` へ流す
 */
router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!authStore.isInitialized) {
    await authStore.fetchMe()
  }

  if (!to.meta.public && !authStore.isAuthenticated) {
    return { name: 'login' }
  }

  if (authStore.isAuthenticated) {
    const hasGroup = (authStore.user?.groups.length ?? 0) > 0
    if (to.name === 'login') {
      return { name: hasGroup ? 'home' : 'guidance' }
    }
    if (to.name === 'guidance' && hasGroup) {
      return { name: 'home' }
    }
    if (to.name === 'home' && !hasGroup) {
      return { name: 'guidance' }
    }
  }

  return true
})

export default router
