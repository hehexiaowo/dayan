import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { Search, Plus, Edit, Delete, Refresh, ArrowDown, Close, View, Hide } from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './permission'
import { permission } from '@/directives/permission'
import '@/styles/index.scss'

const app = createApp(App)

// 仅注册常用图标（按需引入，减少打包体积）
const icons = { Search, Plus, Edit, Delete, Refresh, ArrowDown, Close, View, Hide }
for (const [key, component] of Object.entries(icons)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.directive('permission', permission)

app.mount('#app')
