import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
// Geist Sans:数字/西文专用字体(SaaS 质感),中文回退系统中文字体
import '@fontsource/geist-sans/400.css'
import '@fontsource/geist-sans/500.css'
import '@fontsource/geist-sans/600.css'
import './assets/global.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
