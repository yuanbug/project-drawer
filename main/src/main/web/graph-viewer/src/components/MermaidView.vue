<template>
  <div class="mermaid-view">
    <div>
      <div class="mermaid-code">
        <p>图片仅供预览，建议将代码复制到mermaid编辑器使用</p>
        <pre>{{ code }}</pre>
      </div>
      <div id="mermaid-graph" class="mermaid" v-html="svgHtml"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import mermaid from 'mermaid'
import { onMounted, watch, ref, nextTick } from 'vue'
import svgPanZoom from 'svg-pan-zoom'

const props = defineProps({
  code: {
    type: String,
  },
})

const svgHtml = ref('')

const refresh = async () => {
  const { svg } = await mermaid.render('mermaid-svg', props.code || '')
  svgHtml.value = svg
  nextTick(() => {
    const svg = document.getElementById('mermaid-svg')
    if (!svg) {
      return
    }
    svg.removeAttribute('style')
    svgPanZoom('#mermaid-svg')
  })
}

onMounted(() => {
  mermaid.initialize({ startOnLoad: false })
  refresh()
})

watch(() => props.code, refresh)
</script>

<style lang="css">
.mermaid-view {
  height: 80vh;
}

.mermaid-code {
  float: left;
  overflow: auto;
  margin-right: 40px;
  height: 80vh;
}

.mermaid-code pre {
  width: 30vw;
  min-height: 50vh;
}

.mermaid {
  float: left;
  overflow: auto;
  width: calc(50vw - 40px);
  height: 80vh;
}

.mermaid svg {
  width: 45vw;
  height: 75vh;
}
</style>
