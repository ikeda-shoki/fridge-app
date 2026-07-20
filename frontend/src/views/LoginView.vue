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
  <v-container class="d-flex flex-column justify-center fill-height text-center" fluid>
    <v-icon icon="mdi-fridge-outline" size="64" color="primary" class="mb-4 mx-auto" />
    <h1 class="font-pop text-h4 font-weight-bold mb-6">冷蔵庫アプリ</h1>
    <div ref="buttonContainer" class="d-flex justify-center"></div>
    <v-alert v-if="errorMessage" type="error" variant="tonal" class="mt-4 mx-4" density="compact">
      {{ errorMessage }}
    </v-alert>
  </v-container>
</template>
