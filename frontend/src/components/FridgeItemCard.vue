<script setup lang="ts">
// 冷蔵庫一覧の 1 行（FRG-04）。サムネイル・名前・数量・カテゴリと期限ハイライト（FRG-05）を表示する。
import { computed } from 'vue'
import { expiryStatusOf, expiryLabelOf, expiryColorOf } from '@/lib/expiry'
import { fridgeItemImageUrl } from '@/lib/fridgeItemImage'
import type { FridgeItem } from '@/types/fridgeItem'

const props = defineProps<{ item: FridgeItem }>()

const imageUrl = computed(() => fridgeItemImageUrl(props.item))
const expiryStatus = computed(() => expiryStatusOf(props.item.expiresAt))
const expiryLabel = computed(() => expiryLabelOf(props.item.expiresAt))
const expiryColor = computed(() => expiryColorOf(expiryStatus.value))
</script>

<template>
  <v-card class="d-flex align-center pa-3 mb-3" :elevation="1">
    <v-avatar v-if="imageUrl" size="56" rounded="lg" class="me-3">
      <v-img :src="imageUrl" :alt="item.displayName" cover />
    </v-avatar>
    <v-avatar v-else size="56" rounded="lg" color="grey-lighten-3" class="me-3">
      <v-icon icon="mdi-food-apple-outline" color="grey" />
    </v-avatar>

    <div class="flex-grow-1 min-width-0">
      <div class="text-subtitle-1 font-weight-medium text-truncate">
        {{ item.displayName }}
      </div>
      <div class="text-body-2 text-medium-emphasis">
        {{ item.quantity }}{{ item.unit ?? '個' }}
        <span v-if="item.category"> ・{{ item.category }}</span>
      </div>
    </div>

    <v-chip v-if="expiryLabel" :color="expiryColor" size="small" class="ms-2 flex-shrink-0">
      {{ expiryLabel }}
    </v-chip>
  </v-card>
</template>

<style scoped>
/* text-truncate を効かせるため、flex アイテムの最小幅の既定値（auto）を潰す */
.min-width-0 {
  min-width: 0;
}
</style>
