<template>
  <div class="method-list-block">
    <Collapse v-if="Object.keys(data).length">
      <CollapsePanel v-for="(group, groupName) in data" :key="groupName" :header="groupName">
        <div v-for="(subGroup, subGroupName) in group" :key="subGroupName" class="sub-group">
          <span class="sub-group-name">{{ subGroupName }}</span>
          <ul>
            <li v-for="(method, methodId) in subGroup" :key="methodId">
              <a class="method-name" @click="() => onLoadMethodLink(method)"> {{ method.name }}</a>
            </li>
          </ul>
        </div>
      </CollapsePanel>
    </Collapse>
  </div>
</template>

<script setup lang="ts">
import {Collapse, CollapsePanel} from 'ant-design-vue'
import {computed, type PropType} from 'vue'
import type {MethodListItem} from '@/types'

const props = defineProps({
  methods: {
    type: Array as PropType<MethodListItem[]>,
    default: () => [],
  },
  onLoadMethodLink: {
    type: Function as PropType<(method: MethodListItem) => void>,
    default: () => () => {
    },
  },
})

const groupMethods = (methods: MethodListItem[]) => {
  return methods.reduce((acc: Record<string, Record<string, MethodListItem[]>>, method) => {
    const groupName = method.groupName
    const subGroupName = method.subGroupName
    if (!acc[groupName]) {
      acc[groupName] = {}
    }
    if (!acc[groupName][subGroupName]) {
      acc[groupName][subGroupName] = []
    }
    acc[groupName][subGroupName].push(method)
    return acc
  }, {})
}

const data = computed(() => groupMethods(props.methods))
</script>

<style lang="css">
.method-list-block {
  height: calc(100vh - 120px);
  overflow-y: auto;
  overflow-x: hidden;
}

.sub-group {
  line-break: anywhere;
}

.sub-group-name {
  font-weight: bold;
  font-size: 10px;
}

.method-name {
  font-size: 10px;
}
</style>
