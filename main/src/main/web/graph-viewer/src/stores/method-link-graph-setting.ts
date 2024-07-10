import { defineStore } from 'pinia'

export const useMethodLinkGraphSettingStore = defineStore('method-link-setting', {
  persist: true,
  state: () => ({
    showDependencyType: false,
  }),
})
