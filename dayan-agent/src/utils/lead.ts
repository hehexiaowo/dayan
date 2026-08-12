/**
 * 线索域公共展示函数（列表页/详情页共用，消除重复定义）。
 */
import { LeadStatus } from '@/types';

export function statusText(s?: LeadStatus | number): string {
  switch (s) {
    case LeadStatus.NEW: case 1: return '新线索';
    case LeadStatus.FOLLOWING: case 2: return '跟进中';
    case LeadStatus.INTENDED: case 3: return '意向';
    case LeadStatus.CONVERTED: case 4: return '已转化';
    case LeadStatus.LOST: case 5: return '已流失';
    default: return '未知';
  }
}

export function statusClass(s?: LeadStatus | number): string {
  switch (s) {
    case LeadStatus.NEW: case 1: return 'st-new';
    case LeadStatus.FOLLOWING: case 2: return 'st-following';
    case LeadStatus.INTENDED: case 3: return 'st-intended';
    case LeadStatus.CONVERTED: case 4: return 'st-converted';
    case LeadStatus.LOST: case 5: return 'st-lost';
    default: return '';
  }
}

export function avatarColor(s?: LeadStatus | number): 'blue' | 'green' | 'orange' | 'red' | 'gray' {
  switch (s) {
    case LeadStatus.NEW: case 1: return 'blue';
    case LeadStatus.FOLLOWING: case 2: return 'orange';
    case LeadStatus.INTENDED: case 3: return 'red';
    case LeadStatus.CONVERTED: case 4: return 'green';
    case LeadStatus.LOST: case 5: return 'gray';
    default: return 'blue';
  }
}

export function genderText(g?: number): string {
  switch (g) { case 1: return '男'; case 2: return '女'; default: return '未知'; }
}

export function intentionText(level?: number): string {
  switch (level) { case 3: return '高'; case 2: return '中'; case 1: return '低'; default: return '-'; }
}

export function intentionClass(level?: number): string {
  switch (level) { case 3: return 'it-high'; case 2: return 'it-mid'; case 1: return 'it-low'; default: return ''; }
}

export function sourceText(s?: number): string {
  switch (s) {
    case 1: return '手工录入';
    case 2: return '分享扫码';
    case 3: return '活动接触';
    case 4: return '转介绍';
    case 5: return '内容引流';
    default: return '未知';
  }
}

/**
 * ISO 时间格式化。
 * @param short true=列表用 `MM-DD HH:mm`，false=详情用 `YYYY-MM-DD HH:mm`
 */
export function formatTime(t?: string, short = false): string {
  if (!t) return '-';
  const norm = t.replace('T', ' ');
  return short ? norm.slice(5, 16) : norm.slice(0, 16);
}

/**
 * 互动类型文案（与 TraceType 枚举对齐）。
 * 1=浏览内容 2=使用工具 3=查看海报
 */
export function traceTypeText(type?: number): string {
  switch (type) {
    case 1: return '浏览内容';
    case 2: return '使用工具';
    case 3: return '查看海报';
    default: return '访问';
  }
}

/**
 * 互动类型单字图标（列表卡片紧凑展示用）。
 */
export function traceTypeIcon(type?: number): string {
  switch (type) {
    case 1: return '文';
    case 2: return '具';
    case 3: return '海';
    default: return '访';
  }
}
