<script setup lang="ts">
import { RouterView } from 'vue-router'
import DefaultLayout from './layouts/DefaultLayout.vue'
import Toast from './components/common/Toast.vue'
</script>

<template>
  <DefaultLayout>
    <RouterView :key="$route.fullPath" />
  </DefaultLayout>
  <Toast />
</template>
