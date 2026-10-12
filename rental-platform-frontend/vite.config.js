import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需自动引入（组件与样式）
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] })
  ],
  build: {
    // 保留标准 backdrop-filter（毛玻璃）：默认 target 下 esbuild 会删标准属性只留 -webkit- 前缀
    cssTarget: 'chrome90'
  },
  resolve: {
    alias: {
      '@': `${import.meta.dirname}/src`
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true
      }
    }
  },
  preview: {
    // 局域网演示：npm run preview -- --host 0.0.0.0 后，同网段设备访问 http://<本机IP>:4173
    host: true,
    port: 4173,
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true
      }
    }
  },
  test: {
    environment: 'jsdom',
    css: true,
    server: {
      deps: {
        // element-plus 由按需插件注入样式 import，需经 Vite 转换而非 Node 原生加载
        inline: ['element-plus']
      }
    }
  }
})
