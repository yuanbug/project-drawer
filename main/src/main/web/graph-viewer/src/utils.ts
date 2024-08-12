import { message } from 'ant-design-vue'

export function getData<T>(url: string, name: string, callback: (data: T) => void, finalAction?: () => void) {
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
