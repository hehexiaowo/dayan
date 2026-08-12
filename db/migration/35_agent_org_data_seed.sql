-- 35_agent_org_data_seed.sql
-- 补全演示代理人 AG91925238 的组织信息（公司/分公司/部门/职位/工号/执业证号）
-- 渠道 CH00002 = 平安养老保险，组织信息应与渠道一致

UPDATE agent_info SET
  company_name  = '平安养老保险股份有限公司',
  branch_name   = '天津分公司',
  department    = '银保业务部',
  position      = '高级保险顾问',
  employee_no   = 'PA2026088',
  license_no    = '002026120000000088',
  agent_level   = 3,
  is_certified  = 1
WHERE agent_code = 'AG91925238';
