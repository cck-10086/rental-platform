<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup>
// Element Plus 中文语言包（组件由 unplugin-vue-components 按需自动引入）
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>
