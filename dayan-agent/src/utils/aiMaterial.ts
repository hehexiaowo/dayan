import { getContentDetail } from '@/api/content'
import { retrieveKnowledge, type KnowledgeCitation } from '@/api/knowledge'
import { getParkFullDetail } from '@/api/park'
import type { GoodsProduct } from '@/types'
import type { ParkFullDetail } from '@/types/park'
import type {
  AiCodeNameRef,
  AiKbFileRef,
  AiMaterialBlock,
  AiMaterialRefs
} from '@/types/toolAiCreator'
import { htmlToText } from '@/utils/htmlToText'

/** 机构全量详情的宽松别名（字段与 ParkFullDetail 同构，便于本地窄类型书写） */
type ParkFullDetailLike = ParkFullDetail

/** 聚合参数：来自 step-material 的选择状态 */
export interface AssembleMaterialsInput {
  purpose: string
  topic: string
  /** 已选知识文件（含 fileId/fileName/repoId） */
  kbDocs: { fileId: string; fileName: string; repoId?: number }[]
  /** 已选商品（getGoodsList 行，详情已在手） */
  goods: GoodsProduct[]
  /** 已选机构编码 */
  parkCodes: string[]
  /** 参考范文（TPL:模板码 或 内容 code；为空跳过） */
  refContentCode: string
}

/** 聚合结果：素材块 + 引用（含展示名） */
export interface AssembleMaterialsResult {
  materials: AiMaterialBlock[]
  materialRefs: AiMaterialRefs
  warnings: string[]
}

/** 素材总量上限（与后端一致，超限截断） */
const MATERIAL_TOTAL_MAX = 8000
/** 参考范文正文最大截取字符数 */
const REF_CONTENT_MAX = 3000
/** 机构展示板块单块截取 */
const PARK_BLOCK_MAX = 150

const NETWORK_LABELS: Record<string, string> = { vital: '活力长居', care: '照护长居', sojourn: '旅游短居' }
const BILLING_LABELS: Record<number, string> = { 1: '月', 2: '季', 3: '半年', 4: '年', 5: '一次性' }
const CHARGE_LABELS: Record<number, string> = { 1: '房间费', 2: '照护费', 3: '餐费', 4: '押金', 5: '设施费', 6: '服务费', 9: '其他' }

/**
 * 并行聚合四类素材（前端供材，替代后端素材聚合器）：
 * 范文正文 + 知识库检索召回 + 商品详情（已在手）+ 机构结构化摘要。
 * 单项失败降级为 warning，不阻断创建。
 */
export async function assembleMaterials(input: AssembleMaterialsInput): Promise<AssembleMaterialsResult> {
  const blocks: AiMaterialBlock[] = []
  const warnings: string[] = []
  const refs: AiMaterialRefs = {}

  // 1) 范文（TPL: 模板静态文案后端持有，仅内容 code 拉正文）
  if (input.refContentCode) {
    refs.refContentCode = input.refContentCode
    if (!input.refContentCode.startsWith('TPL:')) {
      try {
        const c = await getContentDetail(input.refContentCode)
        const text = htmlToText(c.contentBody || '').slice(0, REF_CONTENT_MAX).trim()
        if (text) {
          blocks.push({ type: 'ref', title: `参考范文 · ${c.title || ''}`, text })
        }
      } catch {
        warnings.push('参考范文加载失败，已跳过')
      }
    }
  }

  // 2) 知识库（按仓库分组检索，勾选文档精准召回）
  if (input.kbDocs.length) {
    refs.kbFiles = input.kbDocs.map<AiKbFileRef>((d) => ({ fileId: d.fileId, fileName: d.fileName }))
    const byRepo = new Map<number, { fileId: string; fileName: string }[]>()
    for (const d of input.kbDocs) {
      if (!d.repoId) continue
      const arr = byRepo.get(d.repoId) || []
      arr.push({ fileId: d.fileId, fileName: d.fileName })
      byRepo.set(d.repoId, arr)
    }
    for (const [repoId, docs] of byRepo) {
      try {
        const cites: KnowledgeCitation[] = await retrieveKnowledge({
          repoId,
          query: input.topic || docs.map((d) => d.fileName).join(' ') || '养老',
          docFileIds: docs.map((d) => d.fileId),
          topK: 8,
        })
        const text = cites
          .map((c, i) => `[${i + 1}] ${(c.text || '').replace(/\s+/g, ' ').trim()}`)
          .filter((l) => l.length > 4)
          .join('\n')
        if (text) {
          blocks.push({ type: 'kb', title: `知识库资料（${docs.length} 篇勾选文档）`, text })
        }
      } catch {
        warnings.push('知识库检索失败，已跳过')
      }
    }
  }

  // 3) 商品（详情已在手，直接成块）
  if (input.goods.length) {
    refs.goods = input.goods.map<AiCodeNameRef>((g) => ({ code: g.goodsCode, name: g.goodsName }))
    for (const g of input.goods) {
      const parts = [
        g.goodsDescription || g.summary || '',
        g.salePrice != null ? `售价 ${g.salePrice} 元/${g.priceUnit || '份'}` : '',
      ].filter(Boolean)
      if (parts.join('')) {
        blocks.push({ type: 'goods', title: `商品 · ${g.goodsName}`, text: parts.join('\n') })
      }
    }
  }

  // 4) 机构（结构化摘要，防机构信息幻觉）
  if (input.parkCodes.length) {
    refs.parks = []
    for (const code of input.parkCodes) {
      try {
        const full = await getParkFullDetail(code)
        const p = full.parkInfo
        if (!p) continue
        refs.parks.push({ code, name: p.fullName || code })
        blocks.push({ type: 'park', title: `机构资料 · ${p.fullName || code}`, text: buildParkSummary(full) })
      } catch {
        warnings.push(`机构 ${code} 资料加载失败，已跳过`)
      }
    }
  }

  // 总量截断
  let used = 0
  const kept: AiMaterialBlock[] = []
  for (const b of blocks) {
    if (used >= MATERIAL_TOTAL_MAX) {
      warnings.push('素材总量超限，已截断')
      break
    }
    kept.push(b)
    used += b.text.length
  }
  return { materials: kept, materialRefs: refs, warnings }
}

/**
 * 机构结构化素材摘要（后端 AiMaterialAssembler.buildParkSummary 的 TS 移植；
 * 事实全部来自机构库结构化数据，防机构信息幻觉）。
 */
function buildParkSummary(full: ParkFullDetailLike): string {
  const p = full.parkInfo
  const lines: string[] = []
  const loc = `${p.province || ''}${p.city || ''}${p.district || ''}${p.address || ''}`
  if (loc) lines.push(`位置：${loc}`)
  const tags = (p.networkTags || []).map((t) => NETWORK_LABELS[t] || t).filter(Boolean)
  let head = ''
  if (tags.length) head += `业态：${tags.join('、')}`
  if (p.dayanLevel != null) head += `${head ? '｜' : ''}平台评级：${p.dayanLevel} 级`
  if (p.specialtyTag) head += `${head ? '｜' : ''}特色：${p.specialtyTag}`
  if (head) lines.push(head)
  if (p.baseDescription) lines.push(`简介：${p.baseDescription.slice(0, 400)}`)
  lines.push(
    `床位：总 ${p.totalBeds ?? '-'} 张，可入住 ${p.availableBeds ?? '-'} 张` +
      (p.checkInAgeMin != null && p.checkInAgeMax != null ? `；入住年龄 ${p.checkInAgeMin}-${p.checkInAgeMax} 岁` : '')
  )
  if (p.minPriceDisplay != null && p.minPriceDisplay > 0) {
    const max = p.maxPriceDisplay != null && p.maxPriceDisplay > p.minPriceDisplay ? `-${p.maxPriceDisplay}` : ''
    lines.push(`价格参考：${p.minPriceDisplay}${max} 元/${p.priceUnit || '月'}`)
  }
  const roomNames = (full.roomTypes || []).map((r) => r.roomTypeName).filter(Boolean)
  if (roomNames.length) lines.push(`房型：${roomNames.join('、')}`)
  const currentPricing = (full.pricingList || []).filter((pr) => pr.isCurrent === 1).slice(0, 8)
  if (currentPricing.length) {
    const parts = currentPricing.map((pr) => {
      const charge = CHARGE_LABELS[pr.chargeType ?? -1] || '费用'
      const refName = pr.refName || pr.planName || '费用项'
      const cycle = BILLING_LABELS[pr.billingCycle ?? -1] || '期'
      const price = pr.salePrice != null ? `${pr.salePrice}元/` : '面议'
      return `${charge} ${refName} ${price}${cycle}`
    })
    lines.push(`费用明细（当前价）：${parts.join('；')}`)
  }
  const pickNames = <T extends object>(arr: T[] | undefined, key: keyof T): string[] =>
    (arr || []).map((x) => String(x[key] ?? '')).filter(Boolean)
  const services = pickNames(full.serviceTypes, 'serviceTypeName')
  if (services.length) lines.push(`服务：${services.join('、')}`)
  const cares = pickNames(full.careTypes, 'careTypeName')
  if (cares.length) lines.push(`照护等级：${cares.join('、')}`)
  const foods = pickNames(full.foodTypes, 'foodTypeName')
  if (foods.length) lines.push(`餐饮：${foods.join('、')}`)
  const facilities = pickNames(full.facilityTypes, 'facilityTypeName')
  if (facilities.length) lines.push(`设施：${facilities.join('、')}`)
  for (const b of (full.displayBlocks || []).slice(0, 4)) {
    if (b.blockTitle) {
      lines.push(`亮点：${b.blockTitle}——${htmlToText(b.content || '').slice(0, PARK_BLOCK_MAX)}`)
    }
  }
  return lines.join('\n')
}
