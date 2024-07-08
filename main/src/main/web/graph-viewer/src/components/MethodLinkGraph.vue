<template>
  <div class="container-block">
    <div ref="container"></div>
  </div>
</template>

<script setup lang="ts">
import {type PropType, ref, watch} from 'vue'
import {Arrow, Graph} from '@antv/g6'
import type {MethodLink} from '@/types'

const container = ref<HTMLDivElement | undefined>(undefined)
const graph = ref<Graph | undefined>(undefined)

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
      style: {
        stroke: '#F6BD16',
        endArrow: {
          path: Arrow.triangle(),
          fill: '#F6BD16',
        },
      },
    })),
    ...Object.entries(methodLink.overrides)
        .map(kv => ({from: kv[0], toMethods: kv[1]}))
        .flatMap(({from, toMethods}) =>
            toMethods.map(toMethod => ({
              source: from,
              target: toMethod,
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

  return {nodes, edges}
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
    height: window.innerHeight - 120,
    modes: {
      default: [
        'drag-canvas',
        'zoom-canvas',
        'drag-node',
        // {
        //   type: 'activate-relations',
        //   trigger: 'click',
        //   resetSelected: true,
        // },
      ],
    },
    layout: {
      type: 'dagre',
    },
    fitView: true,
    defaultEdge: {
      type: 'quadratic',
      loopCfg: {
        position: 'top',
        dist: 40,
        pointPadding: 200
      },
    },
  })
}

watch(
    () => props.methodLink,
    methodLink => {
      initGraph()
      graph.value?.data(methodLink ? toGraphData(methodLink) : {nodes: [], edges: []})
      graph.value?.render()
    }
)
</script>

<style lang="css">
.container-block {
  height: calc(100vh - 120px);
}
</style>
