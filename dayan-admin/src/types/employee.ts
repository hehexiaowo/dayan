/**
 * 员工相关类型。
 *
 * 字段对齐后端 com.dayan.organ.entity.OrganEmployee。
 */

/** 员工状态：1在职 0离职 */
export enum EmployeeStatus {
  /** 在职 */
  ACTIVE = 1,
  /** 离职 */
  RESIGNED = 0
}

/** 员工状态选项 */
export const EMPLOYEE_STATUS_OPTIONS = [
  { label: '在职', value: EmployeeStatus.ACTIVE },
  { label: '离职', value: EmployeeStatus.RESIGNED }
] as const

/**
 * 员工实体（后端 OrganEmployee）。
 */
export interface Employee {
  id?: number
  /** 机构编码 */
  organCode: string
  /** 员工编码（主键业务码） */
  employeeCode?: string
  /** 关联账号编码 */
  accountCode?: string
  /** 所属部门编码 */
  deptCode?: string
  /** 真实姓名 */
  realName: string
  /** 性别：1男 2女 0未知 */
  gender?: number
  /** 手机号 */
  phone?: string
  /** 邮箱 */
  email?: string
  /** 身份证号（敏感） */
  idCard?: string
  /** 职位 */
  position?: string
  /** 入职日期（yyyy-MM-dd） */
  entryDate?: string
  /** 离职日期（yyyy-MM-dd） */
  leaveDate?: string
  /** 头像地址 */
  avatar?: string
  /** 员工状态：1在职 0离职 */
  employeeStatus: EmployeeStatus
  /** 备注 */
  remark?: string
}

/** 员工分页查询参数 */
export interface EmployeeQuery {
  /** 机构编码（可选） */
  organCode?: string
  /** 所属部门（可选） */
  deptCode?: string
  /** 真实姓名（模糊匹配，可选） */
  realName?: string
  /** 手机号（模糊匹配，可选） */
  phone?: string
  /** 员工状态（可选） */
  employeeStatus?: EmployeeStatus
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
}
