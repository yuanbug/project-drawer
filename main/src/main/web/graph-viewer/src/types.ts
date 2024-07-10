export interface MethodListItem {
  methodId: string
  groupName: string
  subGroupName: string
  name: string
}

export interface MethodLink {
  rootMethodId: string
  methods: Record<string, Method>
  callings: MethodCalling[]
  recursions: MethodCalling[]
  overrides: Record<string, string[]>
}

export const MethodCallingTypes = {
  SELF: '类内',
  SUPER: '父类',
  BROTHER: '同模块',
  OUT: '跨模块',
  JDK: 'JDK',
  LIBRARY: '库',
}

export interface MethodCalling {
  from: string
  to: string
  type: keyof typeof MethodCallingTypes
}

export interface Method {
  id: string
  name: string
  declaringClass: string
  arguments: { name: string; type: string }[]
}
