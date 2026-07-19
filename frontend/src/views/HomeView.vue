<script setup lang="ts">
// 認証後のプレースホルダー画面（本来の冷蔵庫一覧はステップ15で実装する）。
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

async function handleLogout(): Promise<void> {
  await authStore.logout()
  await router.push({ name: 'login' })
}
</script>

<template>
  <v-container>
    <v-row justify="center">
      <v-col cols="12" sm="8" md="6">
        <v-card class="pa-4" elevation="1" rounded="lg">
          <v-card-item>
            <v-avatar v-if="authStore.user?.avatarUrl" :image="authStore.user.avatarUrl" />
            <v-card-title>{{ authStore.user?.displayName }} さん、ようこそ</v-card-title>
          </v-card-item>
          <v-card-actions>
            <v-spacer />
            <v-btn variant="text" @click="handleLogout">ログアウト</v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
