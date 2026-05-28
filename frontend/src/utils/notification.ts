/**
 * 统一的通知工具
 * 提供简洁的 API 来显示各种类型的通知
 */

interface NotificationOptions {
  title?: string
  duration?: number
}

type NotificationType = 'success' | 'error' | 'warning' | 'info'

class NotificationService {
  /**
   * 显示通知
   */
  show(message: string, type: NotificationType = 'info', options?: NotificationOptions) {
    // 尝试使用全局方法
    if (typeof window !== 'undefined' && (window as any).$notification) {
      const method = (window as any).$notification[type]
      if (method) {
        method(message, options?.title)
        return
      }
    }
    
    // 降级方案：使用 console
    console.log(`[${type.toUpperCase()}] ${options?.title ? options.title + ': ' : ''}${message}`)
  }

  success(message: string, options?: NotificationOptions) {
    this.show(message, 'success', options)
  }

  error(message: string, options?: NotificationOptions) {
    this.show(message, 'error', options)
  }

  warning(message: string, options?: NotificationOptions) {
    this.show(message, 'warning', options)
  }

  info(message: string, options?: NotificationOptions) {
    this.show(message, 'info', options)
  }
}

export const notification = new NotificationService()
export default notification
