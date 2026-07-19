<script setup lang="ts">
// アプリのルートコンポーネント。モバイルファーストのアプリシェル（ステップ13）を提供する。
</script>

<template>
  <v-app>
    <v-main>
      <div class="app-shell">
        <router-view />
      </div>
    </v-main>
  </v-app>
</template>

<style scoped>
/*
 * モバイル縦画面（最小360px）を基準にし、PC では最大幅を制限して中央寄せにする
 * （docs/screens.md）。広い画面ではシェルの外側に背景色の余白ができ、スマホアプリの
 * ような見た目になる。
 */
.app-shell {
  max-width: 480px;
  margin-inline: auto;
  min-height: 100%;
  background: rgb(var(--v-theme-surface));
}

@media (min-width: 600px) {
  .app-shell {
    box-shadow: 0 0 24px rgba(0, 0, 0, 0.08);
  }
}
</style>
