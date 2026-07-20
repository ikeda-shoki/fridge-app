<script setup lang="ts">
// 冷蔵庫一覧（ホーム）。一覧・期限ハイライト・名前検索・カテゴリフィルタ・追加 FAB（FRG-04〜06・09）。
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useFridgeStore } from '@/stores/fridge'
import FridgeItemCard from '@/components/FridgeItemCard.vue'
import { FRIDGE_ITEM_CATEGORIES } from '@/types/fridgeItem'

/** 入力のたびに API を叩かないための待ち時間。 */
const SEARCH_DEBOUNCE_MS = 300

const router = useRouter()
const authStore = useAuthStore()
const fridgeStore = useFridgeStore()

const searchQuery = ref('')
const selectedCategory = ref<string | null>(null)

const groupId = computed(() => authStore.currentGroup?.id ?? null)
const isEmpty = computed(() => !fridgeStore.loading && fridgeStore.items.length === 0)
const hasFilter = computed(() => searchQuery.value !== '' || selectedCategory.value !== null)

let debounceTimer: ReturnType<typeof setTimeout> | undefined

function reload(): void {
  if (!groupId.value) return
  fridgeStore.fetchItems(groupId.value, {
    category: selectedCategory.value,
    q: searchQuery.value,
  })
}

onMounted(reload)
onUnmounted(() => clearTimeout(debounceTimer))

// カテゴリは即時、検索文字列は入力が落ち着いてから反映する
watch(selectedCategory, reload)
watch(searchQuery, () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(reload, SEARCH_DEBOUNCE_MS)
})

async function handleLogout(): Promise<void> {
  try {
    await authStore.logout()
  } finally {
    // ログアウト API が失敗してもローカル状態は破棄済みなのでログイン画面へ遷移する
    await router.push({ name: 'login' })
  }
}

function handleAddItem(): void {
  // TODO(step-16): アイテム編集ボトムシートを開く
}
</script>

<template>
  <v-app-bar color="primary" density="comfortable" flat>
    <v-app-bar-title>
      <span class="font-pop font-weight-bold d-inline-flex align-center ga-2">
        <v-icon icon="mdi-fridge-outline" size="24" />
        {{ authStore.currentGroup?.name ?? '冷蔵庫' }}
      </span>
    </v-app-bar-title>
    <template #append>
      <v-btn icon="mdi-logout" aria-label="ログアウト" @click="handleLogout" />
    </template>
  </v-app-bar>

  <v-container class="pb-16">
    <v-text-field
      v-model="searchQuery"
      placeholder="アイテム名で検索"
      prepend-inner-icon="mdi-magnify"
      clearable
      hide-details
      class="mb-3"
    />

    <div class="d-flex flex-wrap ga-2 mb-3">
      <v-chip
        size="small"
        :variant="selectedCategory === null ? 'flat' : 'outlined'"
        :color="selectedCategory === null ? 'primary' : undefined"
        @click="selectedCategory = null"
      >
        すべて
      </v-chip>
      <v-chip
        v-for="category in FRIDGE_ITEM_CATEGORIES"
        :key="category"
        size="small"
        :variant="selectedCategory === category ? 'flat' : 'outlined'"
        :color="selectedCategory === category ? 'primary' : undefined"
        @click="selectedCategory = category"
      >
        {{ category }}
      </v-chip>
    </div>

    <v-alert
      v-if="fridgeStore.errorMessage"
      type="error"
      variant="tonal"
      density="compact"
      class="mb-3"
    >
      {{ fridgeStore.errorMessage }}
    </v-alert>

    <div v-if="fridgeStore.loading" class="d-flex justify-center py-8">
      <v-progress-circular indeterminate color="primary" />
    </div>

    <div v-else-if="isEmpty" class="text-center text-medium-emphasis py-10">
      <v-icon icon="mdi-fridge-off-outline" size="48" class="mb-3" />
      <p v-if="hasFilter">条件に一致するアイテムはありません。</p>
      <p v-else>冷蔵庫にアイテムがありません。<br />右下のボタンから追加しましょう。</p>
    </div>

    <template v-else>
      <FridgeItemCard v-for="item in fridgeStore.items" :key="item.id" :item="item" />
    </template>
  </v-container>

  <v-fab
    icon="mdi-plus"
    color="primary"
    size="large"
    location="bottom end"
    app
    aria-label="アイテムを追加"
    @click="handleAddItem"
  />
</template>
