<script setup lang="ts">
// ログイン画面（screens.md）。Google ログインボタンのみを表示する。
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useGoogleIdentity } from '@/composables/useGoogleIdentity'

const router = useRouter()
const authStore = useAuthStore()
const { renderButton } = useGoogleIdentity()

const buttonContainer = ref<HTMLElement | null>(null)
const errorMessage = ref<string | null>(null)

onMounted(async () => {
  if (!buttonContainer.value) return
  try {
    await renderButton(buttonContainer.value, handleCredential)
  } catch {
    errorMessage.value = 'ログインボタンの表示に失敗しました。時間をおいて再度お試しください。'
  }
})

async function handleCredential(idToken: string): Promise<void> {
  errorMessage.value = null
  try {
    await authStore.loginWithGoogle(idToken)
    await router.push({ name: 'home' })
  } catch {
    errorMessage.value = 'ログインに失敗しました。時間をおいて再度お試しください。'
  }
}
</script>

<template>
  <v-container class="fill-height" fluid>
    <v-row align="center" justify="center">
      <v-col cols="12" sm="8" md="4">
        <v-card class="pa-6 text-center" elevation="2" rounded="lg">
          <v-card-title class="justify-center text-h5 mb-4">冷蔵庫アプリ</v-card-title>
          <div ref="buttonContainer" class="d-flex justify-center"></div>
          <v-alert v-if="errorMessage" type="error" variant="tonal" class="mt-4" density="compact">
            {{ errorMessage }}
          </v-alert>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
