<template>
  <div class="app">
    <Row :gutter="16">
      <Col :span="4">
        <Card class="method-list-card" title="方法列表">
          <Methodlist :methods="methodList" :onLoadMethodLink="loadMethodLink" />
        </Card>
      </Col>
      <Col :span="20">
        <Card title="方法关系视图">
          <MethodLinkGraph :methodLink="graphMethodLink" />
        </Card>
      </Col>
    </Row>
  </div>
</template>

<script setup lang="ts">
import { Card, Col, message, Row } from 'ant-design-vue'
import Methodlist from './components/MethodList.vue'
import MethodLinkGraph from './components/MethodLinkGraph.vue'
import { onMounted, ref } from 'vue'
import type { MethodLink, MethodListItem } from '@/types'

const methodList = ref<MethodListItem[]>([])
const graphMethodLink = ref<MethodLink | undefined>(void 0)

function getData<T>(url: string, name: string, callback: (data: T) => void, finalAction?: () => void) {
  fetch(url, { method: 'GET' })
    .then(response => {
      if (!response.ok) {
        message.warn(`${name}出错`)
        return undefined
      }
      return response.json() as unknown as T
    })
    .then(data => data && callback(data))
    .finally(() => finalAction?.())
}

onMounted(() => {
  getData<MethodListItem[]>('api/method-info/list', '获取方法列表', data => (methodList.value = data))
})

const loadMethodLink = (method: MethodListItem) => {
  getData<MethodLink>(`api/method-info/method-link?methodId=${encodeURIComponent(method.methodId)}`, '获取方法关系链', data => (graphMethodLink.value = data), message.loading('正在加载', 0))
}
</script>

<style lang="css">
.app {
  width: calc(100vw - 16px);
}

.method-list-card .ant-card-body {
  padding: 2px;
}
</style>
