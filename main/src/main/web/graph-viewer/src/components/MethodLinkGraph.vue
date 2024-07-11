<template>
  <div class="graph-wrapper">
    <div class="tool-bar">
      <Tooltip title="重新绘制">
        <Button type="primary" shape="circle" :icon="h(ReloadOutlined)" size="small" @click="refresh" />
      </Tooltip>
      <Tooltip title="导出PNG图片，若结点过多，可尝试SVG模式">
        <Button type="primary" shape="circle" :icon="h(DownloadOutlined)" size="small" @click="download" />
      </Tooltip>
      <Checkbox v-model:checked="showDependencyType">显示依赖类型</Checkbox>
      <RadioGroup v-model:value="rankdir" size="small" @change="updateRankdir">
        <RadioButton v-for="key in Object.keys(DagreLayouRankdir)" :key="key" :value="key">
          {{ DagreLayouRankdir[key as keyof typeof DagreLayouRankdir] }}
        </RadioButton>
      </RadioGroup>
      <RadioGroup v-model:value="renderType" size="small">
        <Tooltip title="推荐使用，设置后请刷新页面">
          <RadioButton value="canvas">Canvas渲染 </RadioButton>
        </Tooltip>
        <Tooltip title="性能较差，设置后请刷新页面">
          <RadioButton value="svg">SVG渲染 </RadioButton>
        </Tooltip>
      </RadioGroup>
    </div>
    <div class="container-block">
      <div ref="container"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { type PropType, ref, watch, h } from 'vue'
import { Arrow, Graph } from '@antv/g6'
import { MethodCallingTypes, type MethodLink, DagreLayouRankdir } from '@/types'
import { storeToRefs } from 'pinia'
import { Tooltip, Checkbox, Button, RadioGroup, RadioButton } from 'ant-design-vue'
import { ReloadOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { useMethodLinkGraphSettingStore } from '@/stores/method-link-graph-setting'

const container = ref<HTMLDivElement | undefined>(undefined)
const graph = ref<Graph | undefined>(undefined)

const { showDependencyType, rankdir, renderType } = storeToRefs(useMethodLinkGraphSettingStore())

const props = defineProps({
  methodLink: {
    type: Object as PropType<MethodLink>,
  },
})

const toGraphData = (methodLink: MethodLink) => {
  const nodes = Object.values(methodLink.methods).map(method => ({
    id: method.id,
    label: method.name,
    type: 'ellipse',
    size: [method.name.length * 6, 30],
  }))

  const edges = [
    ...methodLink.callings.map(calling => ({
      source: calling.from,
      target: calling.to,
      label: showDependencyType.value ? MethodCallingTypes[calling.type] : '',
      style: {
        stroke: '#F6BD16',
        endArrow: {
          path: Arrow.triangle(),
          fill: '#F6BD16',
        },
      },
    })),
    ...Object.entries(methodLink.overrides)
      .map(kv => ({ from: kv[0], toMethods: kv[1] }))
      .flatMap(({ from, toMethods }) =>
        toMethods.map(toMethod => ({
          source: from,
          target: toMethod,
          label: showDependencyType.value ? '实现' : '',
          style: {
            stroke: '#8f8f8f',
            startArrow: {
              path: Arrow.diamond(),
              fill: '#8f8f8f',
            },
          },
        }))
      ),
    ...methodLink.recursions.map(recursion => ({
      source: recursion.from,
      target: recursion.to,
      type: recursion.from === recursion.to ? 'loop' : 'quadratic',
      label: showDependencyType.value ? '递归' : '',
      style: {
        stroke: '#DC143C',
        lineDash: [2, 2],
        endArrow: {
          path: Arrow.triangle(),
          fill: '#DC143C',
        },
      },
    })),
  ]

  return { nodes, edges }
}

// 放到onMounted不一定能拿到ref，干脆每次watch都调用一下
const initGraph = () => {
  if (!container.value || !props.methodLink) {
    return
  }
  if (graph.value) {
    return
  }
  graph.value = new Graph({
    container: container.value,
    width: container.value.clientWidth - 10,
    height: window.innerHeight - 140,
    modes: {
      default: ['drag-canvas', 'zoom-canvas', 'drag-node'],
    },
    renderer: renderType.value,
    layout: {
      type: 'dagre',
      rankdir: rankdir.value,
    },
    fitView: true,
    defaultEdge: {
      type: 'quadratic',
      loopCfg: {
        position: 'top',
        dist: 40,
        pointPadding: 200,
      },
    },
  })
}

const updateRankdir = () => {
  graph.value?.updateLayout({
    type: 'dagre',
    rankdir: rankdir.value,
  })
}

const refresh = () => {
  initGraph()
  graph.value?.data(props.methodLink ? toGraphData(props.methodLink) : { nodes: [], edges: [] })
  graph.value?.render()
}

watch(() => props.methodLink, refresh)

const download = () => {
  if (!props.methodLink) {
    return
  }
  const zoom = graph.value?.getZoom()
  // 调整缩放比，使导出的图片更清晰 TODO 看看设置PixelRatio有没有用
  graph.value?.zoomTo(3)
  graph.value?.downloadFullImage(props.methodLink.rootMethodId, 'image/png', { padding: 5 })
  graph.value?.zoomTo(zoom || 1)
}
</script>

<style lang="css">
.graph-wrapper {
  height: calc(100vh - 120px);
}

.container-block {
  border: 1px dashed #666;
  height: calc(100vh - 140px);
}

.tool-bar {
  margin-bottom: 5px;
  height: 30px;
}

:nth-child(n + 2 of .tool-bar .ant-btn) {
  margin-left: 8px;
}

.tool-bar .ant-checkbox-wrapper {
  margin-left: 8px;
  padding-left: 2px;
}

.tool-bar .ant-checkbox-wrapper:hover span {
  color: #4096ff;
}

.tool-bar .ant-radio-group {
  margin-left: 8px;
}
</style>
