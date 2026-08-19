/**
 * 部门相关类型。
 *
 * 字段对齐后端 com.dayan.organ.entity.OrganDepartment。
 */

/** 部门状态：1启用 0禁用 */
export enum DepartmentStatus {
  ENABLED = 1,
  DISABLED = 0
}

/** 部门状态选项 */
export const DEPARTMENT_STATUS_OPTIONS = [
  { label: '启用', value: DepartmentStatus.ENABLED },
  { label: '禁用', value: DepartmentStatus.DISABLED }
] as const

/** 部门类型 */
export enum DeptType {
  /** 公司 */
  COMPANY = 1,
  /** 部门 */
  DEPARTMENT = 2,
  /** 小组 */
  GROUP = 3,
}

/** 部门类型选项（对齐后端 dept_type 取值：1=公司, 2=部门, 3=小组） */
export const DEPT_TYPE_OPTIONS = [
  { label: '公司', value: DeptType.COMPANY },
  { label: '部门', value: DeptType.DEPARTMENT },
  { label: '小组', value: DeptType.GROUP }
] as const

/**
 * 部门实体（后端 OrganDepartment）。
 *
 * 后端 list 接口返回平铺列表，由前端 buildDepartmentTree 组树。
 */
export interface Department {
  id?: number
  /** 机构编码 */
  organCode: string
  /** 部门编码（主键业务码） */
  deptCode: string
  /** 部门名称 */
  deptName: string
  /** 父部门编码（顶级为 null/空） */
  parentCode: string | null
  /** 祖级列表（逗号分隔编码链，后端维护） */
  ancestors?: string
  /** 部门类型 */
  deptType?: DeptType
  /** 负责人姓名 */
  leaderName?: string
  /** 负责人电话 */
  leaderPhone?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 0禁用 */
  status: DepartmentStatus
  /** 备注 */
  remark?: string
  /** 子部门（前端组树时填充） */
  children?: Department[]
}

/**
 * 将平铺部门列表构建为树形结构。
 *
 * 后端 /departments 接口返回平铺列表，前端调用此方法组树展示。
 */
export function buildDepartmentTree(list: Department[], parentCode: string | null = null): Department[] {
  return list
    .filter((d) => (d.parentCode ?? null) === parentCode)
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
    .map((d) => {
      const children = buildDepartmentTree(list, d.deptCode)
      return children.length > 0 ? { ...d, children } : { ...d, children: undefined }
    })
}
