import { createApp } from 'vue'
import App from './App.vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import 'ant-design-vue/dist/reset.css'

createApp(App).use(createPinia().use(piniaPluginPersistedstate)).mount('#app')
