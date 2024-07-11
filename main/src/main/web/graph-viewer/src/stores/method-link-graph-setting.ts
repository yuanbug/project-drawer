import { defineStore } from 'pinia'
import { DagreLayouRankdir } from '@/types'

export const useMethodLinkGraphSettingStore = defineStore('method-link-setting', {
  persist: true,
  state: () => ({
    showDependencyType: false,
    rankdir: 'dagre' as keyof typeof DagreLayouRankdir,
    renderType: 'canvas' as 'canvas' | 'svg',
  }),
})
