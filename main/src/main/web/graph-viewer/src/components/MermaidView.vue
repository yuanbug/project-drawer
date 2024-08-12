<template>
  <div class="mermaid-view">
    <p>图片仅供预览，建议将代码复制到mermaid编辑器使用</p>
    <div>
      <div class="mermaid-code">
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
  nextTick(() => svgPanZoom('#mermaid-svg'))
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
}

.mermaid-code pre {
  max-width: 30vw;
  height: 80vh;
}

.mermaid {
  float: left;
  overflow: auto;
}

.mermaid svg {
  width: 50vw;
  height: 80vh;
}
</style>
