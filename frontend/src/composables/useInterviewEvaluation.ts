/**
 * 面试评估结果解析
 */

// 单次问答评估
export interface QAEvaluation {
  考查知识点: string
  评估: string
}

// 总评
export interface KnowledgePoint {
  知识点名称: string
  薄弱程度: '高' | '中' | '低'
}

export interface FinalEvaluation {
  待提升知识点列表: KnowledgePoint[]
  总评: string
}

/**
 * 解析单次问答评估 JSON
 */
export function parseQAEvaluation(jsonStr: string): QAEvaluation | null {
  if (!jsonStr) return null
  try {
    // 尝试解析为 JSON
    const parsed = JSON.parse(jsonStr)
    if (parsed.考查知识点 !== undefined && parsed.评估 !== undefined) {
      return parsed as QAEvaluation
    }
  } catch {
    // 不是 JSON 格式，直接返回 null（可能是纯文本）
  }
  return null
}

/**
 * 解析总评 JSON
 */
export function parseFinalEvaluation(jsonStr: string): FinalEvaluation | null {
  if (!jsonStr) return null
  try {
    const parsed = JSON.parse(jsonStr)
    if (parsed.待提升知识点列表 !== undefined && parsed.总评 !== undefined) {
      return parsed as FinalEvaluation
    }
  } catch {
    // 不是 JSON 格式
  }
  return null
}

/**
 * 获取薄弱程度对应的颜色
 */
export function getWeaknessColor(level: string): string {
  switch (level) {
    case '高':
      return 'text-red-600 bg-red-50'
    case '中':
      return 'text-amber-600 bg-amber-50'
    case '低':
      return 'text-green-600 bg-green-50'
    default:
      return 'text-gray-600 bg-gray-50'
  }
}
