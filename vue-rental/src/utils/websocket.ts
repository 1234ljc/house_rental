import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useUserStore } from '@/stores/user'

type MessageCallback = (message: any) => void

class WebSocketService {
  private client: Client | null = null
  private messageCallbacks: MessageCallback[] = []
  private customerServiceCallbacks: MessageCallback[] = []
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private isConnecting = false

  /**
   * 连接WebSocket
   */
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.client?.connected) {
        resolve()
        return
      }

      if (this.isConnecting) {
        resolve()
        return
      }

      this.isConnecting = true
      const userStore = useUserStore()

      this.client = new Client({
        webSocketFactory: () => new SockJS('http://localhost:8080/ws/chat'),
        connectHeaders: {
          Authorization: `Bearer ${userStore.token}`
        },
        debug: (str) => {
          console.log('[WebSocket]', str)
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('[WebSocket] 连接成功')
          this.isConnecting = false
          this.reconnectAttempts = 0
          this.subscribeToMessages()
          resolve()
        },
        onStompError: (frame) => {
          console.error('[WebSocket] STOMP错误:', frame.headers['message'])
          this.isConnecting = false
          reject(new Error(frame.headers['message']))
        },
        onDisconnect: () => {
          console.log('[WebSocket] 断开连接')
          this.isConnecting = false
        },
        onWebSocketClose: () => {
          console.log('[WebSocket] WebSocket关闭')
          this.isConnecting = false
          this.handleReconnect()
        }
      })

      this.client.activate()
    })
  }

  /**
   * 订阅消息
   */
  private subscribeToMessages() {
    if (!this.client?.connected) return

    // 订阅个人消息队列 - 普通聊天消息
    this.client.subscribe('/user/queue/messages', (message: IMessage) => {
      try {
        const data = JSON.parse(message.body)
        console.log('[WebSocket] 收到普通消息:', data)
        this.messageCallbacks.forEach(callback => callback(data))
      } catch (e) {
        console.error('[WebSocket] 消息解析失败:', e)
      }
    })

    // 订阅客服消息队列
    this.client.subscribe('/user/queue/customer-service', (message: IMessage) => {
      try {
        const data = JSON.parse(message.body)
        console.log('[WebSocket] 收到客服消息:', data)
        this.customerServiceCallbacks.forEach(callback => callback(data))
      } catch (e) {
        console.error('[WebSocket] 客服消息解析失败:', e)
      }
    })
    
    console.log('[WebSocket] 已订阅消息队列')
  }

  /**
   * 处理重连
   */
  private handleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`[WebSocket] 尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
      setTimeout(() => {
        this.connect().catch(console.error)
      }, 3000 * this.reconnectAttempts)
    }
  }

  /**
   * 发送消息
   */
  sendMessage(sessionId: number, content: string, messageType = 0, fileUrl?: string) {
    if (!this.client?.connected) {
      console.error('[WebSocket] 未连接，无法发送消息')
      return false
    }

    this.client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({
        sessionId,
        content,
        messageType,
        fileUrl
      })
    })
    return true
  }

  /**
   * 注册普通消息回调
   */
  onMessage(callback: MessageCallback) {
    this.messageCallbacks.push(callback)
    return () => {
      const index = this.messageCallbacks.indexOf(callback)
      if (index > -1) {
        this.messageCallbacks.splice(index, 1)
      }
    }
  }

  /**
   * 注册客服消息回调
   */
  onCustomerServiceMessage(callback: MessageCallback) {
    this.customerServiceCallbacks.push(callback)
    return () => {
      const index = this.customerServiceCallbacks.indexOf(callback)
      if (index > -1) {
        this.customerServiceCallbacks.splice(index, 1)
      }
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.client) {
      this.client.deactivate()
      this.client = null
    }
    this.messageCallbacks = []
    this.customerServiceCallbacks = []
    this.reconnectAttempts = 0
    this.isConnecting = false
  }

  /**
   * 检查是否已连接
   */
  isConnected(): boolean {
    return this.client?.connected ?? false
  }
}

export const wsService = new WebSocketService()
