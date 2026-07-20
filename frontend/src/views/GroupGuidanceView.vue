<script setup lang="ts">
// 初回ガイダンス画面（GRP-00）。グループ未所属ユーザーに「作成」「参加」の2択を提示する。
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useGroupStore } from '@/stores/groups'
import { extractErrorMessage } from '@/lib/apiError'

type DialogMode = 'create' | 'join' | null

const router = useRouter()
const groupStore = useGroupStore()

const dialogMode = ref<DialogMode>(null)
const inputValue = ref('')
const errorMessage = ref<string | null>(null)
const submitting = ref(false)

function openDialog(mode: DialogMode): void {
  dialogMode.value = mode
  inputValue.value = ''
  errorMessage.value = null
}

function closeDialog(): void {
  dialogMode.value = null
}

async function submit(): Promise<void> {
  if (!inputValue.value.trim()) return

  submitting.value = true
  errorMessage.value = null
  try {
    if (dialogMode.value === 'create') {
      await groupStore.createGroup(inputValue.value.trim())
    } else if (dialogMode.value === 'join') {
      await groupStore.joinGroup(inputValue.value.trim())
    }
    await router.push({ name: 'home' })
  } catch (error) {
    errorMessage.value = extractErrorMessage(
      error,
      '処理に失敗しました。時間をおいて再度お試しください。',
    )
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <v-container class="d-flex flex-column justify-center fill-height text-center">
    <v-icon icon="mdi-account-group-outline" size="64" color="primary" class="mb-4 mx-auto" />
    <h1 class="text-h5 mb-2">ようこそ！</h1>
    <p class="text-body-2 mb-6">
      まだどの家族グループにも参加していません。<br />
      新しく作るか、招待コードで参加しましょう。
    </p>

    <v-btn color="primary" class="mb-4" prepend-icon="mdi-plus" @click="openDialog('create')">
      新しい家族グループを作る
    </v-btn>
    <v-btn
      variant="outlined"
      color="primary"
      prepend-icon="mdi-ticket-confirmation-outline"
      @click="openDialog('join')"
    >
      招待コードで参加する
    </v-btn>

    <v-dialog :model-value="dialogMode !== null" max-width="400" @update:model-value="closeDialog">
      <v-card class="pa-4">
        <v-card-title>
          {{ dialogMode === 'create' ? '家族グループを作る' : '招待コードを入力' }}
        </v-card-title>
        <v-card-text>
          <v-text-field
            v-model="inputValue"
            :label="dialogMode === 'create' ? 'グループ名' : '招待コード（6桁）'"
            :maxlength="dialogMode === 'create' ? 100 : 6"
            autofocus
            @keyup.enter="submit"
          />
          <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact">
            {{ errorMessage }}
          </v-alert>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="closeDialog">キャンセル</v-btn>
          <v-btn color="primary" :loading="submitting" @click="submit">
            {{ dialogMode === 'create' ? '作成する' : '参加する' }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>
