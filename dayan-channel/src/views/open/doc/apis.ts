/**
 * 开放平台接口文档 - 接口元数据。
 *
 * 9 个 /open-api/v1/* 接口的结构化定义，供 ApiCatalog / ApiDetail / ApiTester 渲染。
 * 接口当前建设中（无真实后端），测试面板仅展示请求构造。
 */

/** 单个接口的元数据 */
export interface ApiItem {
  id: string
  method: 'GET' | 'POST' | 'PUT' | 'DELETE'
  path: string
  title: string
  summary: string
  description?: string
  headers?: ApiParam[]
  params?: ApiParam[]
  requestExample?: string
  responseExample: string
}

/** 参数定义 */
export interface ApiParam {
  name: string
  location: 'path' | 'query' | 'header' | 'body'
  type: 'string' | 'number' | 'boolean' | 'object'
  required: boolean
  description: string
  default?: string
}

/** 目录分组 */
export interface ApiGroup {
  id: string
  title: string
  apis: ApiItem[]
}

/** 环境预设 */
export interface EnvOption {
  value: string
  label: string
  baseUrl: string
}

export const ENV_OPTIONS: EnvOption[] = [
  { value: 'test', label: '测试环境', baseUrl: 'https://test.dayan.com' },
  { value: 'staging', label: '预发环境', baseUrl: 'https://staging.dayan.com' },
  { value: 'prod', label: '生产环境', baseUrl: 'https://api.dayan.com' }
]

/** 统一响应外壳（示例用） */
const R = (data: object) =>
  JSON.stringify(
    {
      code: 0,
      message: 'success',
      data,
      timestamp: 1786149639969,
      traceId: '917a1dd4b06d48f4b3d97f75ae6b8a48'
    },
    null,
    2
  )

/** 4 个分组，9 个接口 */
export const groups: ApiGroup[] = [
  {
    id: 'auth',
    title: '认证',
    apis: [
      {
        id: 'token',
        method: 'POST',
        path: '/open-api/v1/token',
        title: '获取 Token',
        summary: '使用 AppKey + 签名换取访问 Token',
        description: '传入 appKey + 签名，返回访问 token（有效期 2 小时）。后续接口携带 Authorization: Bearer {token}。',
        params: [
          { name: 'appKey', location: 'body', type: 'string', required: true, description: '平台分配的应用标识', default: 'your_app_key' },
          { name: 'timestamp', location: 'body', type: 'number', required: true, description: '请求时间戳（毫秒）', default: '1786149639969' },
          { name: 'nonce', location: 'body', type: 'string', required: true, description: '随机字符串（防重放）', default: 'a3f8e2c1' },
          { name: 'sign', location: 'body', type: 'string', required: true, description: 'HMAC-SHA256(appSecret, appKey+timestamp+nonce)', default: 'b7c9d1e3f4a5...' }
        ],
        responseExample: R({ token: 'eyJhbGciOiJIUzI1NiJ9...', expireIn: 7200 })
      }
    ]
  },
  {
    id: 'content',
    title: '内容营销',
    apis: [
      {
        id: 'content-list',
        method: 'GET',
        path: '/open-api/v1/contents',
        title: '内容查询',
        summary: '查询本渠道已配置的内容列表',
        params: [
          { name: 'current', location: 'query', type: 'number', required: false, description: '页码，默认 1', default: '1' },
          { name: 'size', location: 'query', type: 'number', required: false, description: '每页条数，默认 10', default: '10' }
        ],
        responseExample: R({
          records: [{ contentCode: 'CT0000000001', title: '养老政策解读', contentType: 1 }],
          total: 1,
          current: 1,
          size: 10
        })
      },
      {
        id: 'scene-list',
        method: 'GET',
        path: '/open-api/v1/scenes',
        title: '场景查询',
        summary: '查询本渠道已配置的场景列表',
        params: [
          { name: 'current', location: 'query', type: 'number', required: false, description: '页码，默认 1', default: '1' },
          { name: 'size', location: 'query', type: 'number', required: false, description: '每页条数，默认 10', default: '10' }
        ],
        responseExample: R({
          records: [{ sceneCode: 'SC0001', sceneName: '春节关怀', sceneType: 1 }],
          total: 1,
          current: 1,
          size: 10
        })
      },
      {
        id: 'park-list',
        method: 'GET',
        path: '/open-api/v1/parks',
        title: '机构查询',
        summary: '查询养老机构信息',
        params: [
          { name: 'current', location: 'query', type: 'number', required: false, description: '页码，默认 1', default: '1' },
          { name: 'size', location: 'query', type: 'number', required: false, description: '每页条数，默认 10', default: '10' },
          { name: 'cityCode', location: 'query', type: 'string', required: false, description: '城市编码（可选筛选）' }
        ],
        responseExample: R({
          records: [{ parkCode: 'PK0001', parkName: '阳光颐养中心', cityCode: '310100', bedCount: 200 }],
          total: 1,
          current: 1,
          size: 10
        })
      }
    ]
  },
  {
    id: 'equity',
    title: '权益订单',
    apis: [
      {
        id: 'equity-detail',
        method: 'GET',
        path: '/open-api/v1/equities/{equityCode}',
        title: '权益查询',
        summary: '查询指定权益的详情',
        params: [
          { name: 'equityCode', location: 'path', type: 'string', required: true, description: '权益编码', default: 'EQ0000000001' }
        ],
        responseExample: R({
          equityCode: 'EQ0000000001',
          equityName: '长者体检套餐',
          faceValue: 59900,
          validFrom: '2026-01-01',
          validTo: '2026-12-31'
        })
      },
      {
        id: 'activate-callback',
        method: 'POST',
        path: '/open-api/v1/equities/activate-callback',
        title: '权益激活回调',
        summary: '权益激活后由平台回调通知接入方',
        description: '平台在权益激活后向接入方配置的回调地址 POST 此请求。接入方需返回 code:0 确认接收。',
        params: [
          { name: 'equityCode', location: 'body', type: 'string', required: true, description: '权益编码', default: 'EQ0000000001' },
          { name: 'activateCode', location: 'body', type: 'string', required: true, description: '激活码', default: 'AC20260808001' },
          { name: 'clientCode', location: 'body', type: 'string', required: true, description: '客户编码', default: 'CL0001' },
          { name: 'activateTime', location: 'body', type: 'string', required: true, description: '激活时间', default: '2026-08-08 10:30:00' }
        ],
        responseExample: R({ received: true })
      },
      {
        id: 'order-detail',
        method: 'GET',
        path: '/open-api/v1/orders/{orderCode}',
        title: '订单查询',
        summary: '查询订单状态',
        params: [
          { name: 'orderCode', location: 'path', type: 'string', required: true, description: '订单编码', default: 'OD20260808001' }
        ],
        responseExample: R({
          orderCode: 'OD20260808001',
          orderStatus: 2,
          orderAmount: 29900,
          createTime: '2026-08-08 09:00:00',
          payTime: '2026-08-08 09:05:00'
        })
      }
    ]
  },
  {
    id: 'webhook',
    title: 'Webhook',
    apis: [
      {
        id: 'webhook-register',
        method: 'POST',
        path: '/open-api/v1/webhooks/register',
        title: 'Webhook 注册',
        summary: '注册 webhook 回调地址',
        params: [
          { name: 'callbackUrl', location: 'body', type: 'string', required: true, description: '回调地址（HTTPS）', default: 'https://your-server.com/callback' },
          { name: 'events', location: 'body', type: 'string', required: true, description: '订阅事件，逗号分隔（如 equity.activated,order.paid）', default: 'equity.activated,order.paid' }
        ],
        responseExample: R({ webhookId: 'WH0001', registered: true })
      },
      {
        id: 'webhook-logs',
        method: 'GET',
        path: '/open-api/v1/webhooks/logs',
        title: 'Webhook 日志',
        summary: '查询 webhook 推送日志',
        params: [
          { name: 'current', location: 'query', type: 'number', required: false, description: '页码，默认 1', default: '1' },
          { name: 'size', location: 'query', type: 'number', required: false, description: '每页条数，默认 10', default: '10' },
          { name: 'event', location: 'query', type: 'string', required: false, description: '事件类型筛选（可选）', default: 'equity.activated' }
        ],
        responseExample: R({
          records: [
            { logId: 'WL0001', event: 'equity.activated', pushStatus: 1, pushTime: '2026-08-08 10:31:00', responseCode: 200 }
          ],
          total: 1,
          current: 1,
          size: 10
        })
      }
    ]
  }
]

/** 根据 id 查找接口 */
export function findApi(id: string): ApiItem {
  for (const g of groups) {
    const found = g.apis.find((a) => a.id === id)
    if (found) return found
  }
  return groups[0].apis[0]
}
