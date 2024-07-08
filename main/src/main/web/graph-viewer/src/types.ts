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

export type MethodCallingType = 'SELF' | 'SUPER' | 'BROTHER' | 'OUT' | 'JDK' | 'LIBRARY'

export interface MethodCalling {
    from: string
    to: string
    type: MethodCallingType
}

export interface Method {
    id: string
    name: string
    declaringClass: string
    arguments: { name: string; type: string }[]
}
