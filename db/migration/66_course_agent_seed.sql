-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 66_course_agent_seed.sql  课程域 seed（admin 课程管理 + agent 端课程浏览）
--
-- 内容：
--   1. system_dict 补 course_category 分类字典（admin 课程页分类下拉现为空）
--   2. course_lecturer 4 名讲师（与 learning_content 作者人设一致）
--   3. course_info 8 门课程（类型/状态/免费付费全覆盖，验证 agent 端仅显上架）
--
-- 编码说明：CR/LT 用 9 万段（CR9000x/LT9000x），避开 Redis INCR 正常序列
-- （code:seq:CR:0 从 1 起），杜绝与后续 admin 新建编码撞 uk_course_code。
-- 幂等：全部 INSERT 带 NOT EXISTS 守卫，重复执行不翻倍。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 课程分类字典（course_category，风格对齐 content_category 的 CAT00x）
-- ---------------------------------------------------------------------
INSERT INTO `system_dict`
  (`dict_type`, `dict_code`, `dict_name`, `dict_value`, `parent_code`, `level`, `domain`,
   `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
SELECT 'course_category', t.dict_code, t.dict_name, t.dict_value, NULL, 1, 'course',
       t.sort_order, 1, '课程分类（course_info.category_code）', NOW(), NOW(), 'system', 'system', 0
FROM (
  SELECT 'COU001' AS dict_code, '养老规划' AS dict_name, 'COU001' AS dict_value, 1 AS sort_order
  UNION ALL SELECT 'COU002', '销售技能', 'COU002', 2
  UNION ALL SELECT 'COU003', '产品解析', 'COU003', 3
  UNION ALL SELECT 'COU004', '机构运营', 'COU004', 4
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict` d
  WHERE d.dict_type = 'course_category' AND d.dict_code = t.dict_code AND d.deleted = 0
);

-- ---------------------------------------------------------------------
-- 2. 讲师 4 名（LT9000x）
-- ---------------------------------------------------------------------
INSERT INTO `course_lecturer`
  (`lecturer_code`, `lecturer_name`, `gender`, `title`, `organization`, `specialty`,
   `introduction`, `course_count`, `student_count`, `is_certified`, `sort_order`, `status`)
SELECT t.lecturer_code, t.lecturer_name, t.gender, t.title, t.organization, t.specialty,
       t.introduction, 0, 0, 1, t.sort_order, 1
FROM (
  SELECT 'LT90001' AS lecturer_code, '王芳' AS lecturer_name, 2 AS gender,
         '资深养老规划讲师' AS title, '大雁养老研究院' AS organization,
         '["年金险","养老规划"]' AS specialty,
         '15 年保险从业经验，专注年金产品与养老规划授课，累计培训代理人超 2 万人次。' AS introduction, 1 AS sort_order
  UNION ALL SELECT 'LT90002', '李军', 1, '销售总监', '大雁养老年销售中心',
         '["销售管理","合规展业"]',
         '历任区域销售总监，擅长大单策略与团队训练，主导新人营课程体系。', 2
  UNION ALL SELECT 'LT90003', '陈伟', 1, '财富顾问', '大雁养老财富规划部',
         '["资产配置","高净值客户"]',
         'CFP 持证人，服务高净值家庭 300+，专注养老资金规划与税优政策实操。', 3
  UNION ALL SELECT 'LT90004', '张敏', 2, '金牌代理 · 养老社区专家', '大雁养老代理人',
         '["养老社区","体验式营销"]',
         '连续 3 年 MDRT 会员，单月社区参观数 40+ 场，擅长把参观转化为成交。', 4
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `course_lecturer` l WHERE l.lecturer_code = t.lecturer_code AND l.deleted = 0
);

-- ---------------------------------------------------------------------
-- 3. 课程 8 门（CR9000x；状态 2上架x5 / 1待上架x1 / 3已下架x1 / 0草稿x1）
-- ---------------------------------------------------------------------
INSERT INTO `course_info`
  (`course_code`, `course_name`, `course_type`, `category_code`, `course_description`,
   `course_outline`, `target_audience`, `learning_objectives`, `lecturer_code`,
   `total_class`, `total_duration`, `valid_days`, `original_price`, `sale_price`,
   `view_count`, `sales_count`, `is_free`, `is_recommend`, `course_start_date`,
   `course_end_date`, `sort_order`, `course_status`)
SELECT t.course_code, t.course_name, t.course_type, t.category_code, t.course_description,
       t.course_outline, t.target_audience, t.learning_objectives, t.lecturer_code,
       t.total_class, t.total_duration, t.valid_days, t.original_price, t.sale_price,
       t.view_count, t.sales_count, t.is_free, t.is_recommend, t.course_start_date,
       t.course_end_date, t.sort_order, t.course_status
FROM (
  SELECT 'CR90001' AS course_code, '年金险销售全流程实战' AS course_name, 1 AS course_type,
         'COU001' AS category_code,
         '从需求挖掘、方案设计到促成与递归转介绍，12 课时打通年金险销售全链路，配套话术与案例复盘。' AS course_description,
         '[{"title":"第一章 需求挖掘","lessons":[{"title":"养老缺口测算对话","duration":25},{"title":"三代人保单结构盘点","duration":30}]},{"title":"第二章 方案设计","lessons":[{"title":"年金+万能组合逻辑","duration":35},{"title":"收益演示三张表","duration":28}]},{"title":"第三章 促成与异议","lessons":[{"title":"「再考虑一下」应对","duration":22},{"title":"递归转介绍话术","duration":20}]}]' AS course_outline,
         '入行 1-3 年的保险代理人' AS target_audience,
         '独立完成年金险需求分析与方案呈现；掌握 5 类高频异议处理' AS learning_objectives,
         'LT90001' AS lecturer_code, 12 AS total_class, 660 AS total_duration, 365 AS valid_days,
         399.00 AS original_price, 299.00 AS sale_price, 1280 AS view_count, 86 AS sales_count,
         0 AS is_free, 1 AS is_recommend, '2026-08-01' AS course_start_date, NULL AS course_end_date,
         40 AS sort_order, 2 AS course_status
  UNION ALL SELECT 'CR90002', '高净值客户养老规划顾问课', 1, 'COU001',
         '以家庭资产负债表为起点，构建「养老资金三支柱」配置框架，覆盖保单、信托与社区入住权组合。',
         '[{"title":"第一章 客户画像","lessons":[{"title":"高净值家庭五类画像","duration":30}]},{"title":"第二章 配置框架","lessons":[{"title":"三支柱资金结构","duration":40},{"title":"保单与信托衔接","duration":35}]},{"title":"第三章 案例工作坊","lessons":[{"title":"2000 万家庭方案复盘","duration":45}]}]',
         '服务高净值客户的理财顾问与资深代理人',
         '完成一份高净值家庭养老资金规划建议书',
         'LT90003', 16, 900, 365, 699.00, 499.00, 860, 42, 0, 1, '2026-08-05', NULL, 30, 2
  UNION ALL SELECT 'CR90003', '养老社区体验式营销训练营', 4, 'COU002',
         '线上学方法论 + 线下跟访实操：参观前铺垫、现场动线讲解、餐叙促成三段式训练。',
         '[{"title":"第一模块 线上方法论","lessons":[{"title":"参观邀约三板斧","duration":24},{"title":"动线讲解脚本","duration":30}]},{"title":"第二模块 线下实操","lessons":[{"title":"陪访两次+复盘","duration":120}]}]',
         '主推养老社区权益的代理人',
         '独立设计一场完整社区参观动线并完成餐叙促成',
         'LT90004', 10, 480, 180, 499.00, 399.00, 645, 31, 0, 0, '2026-08-10', '2026-09-10', 20, 2
  UNION ALL SELECT 'CR90004', '保险顾问合规展业必修课', 1, 'COU002',
         '销售行为红线、宣传物料合规与客户信息保护三大板块，年度必修。',
         '[{"title":"第一章 销售红线","lessons":[{"title":"十类禁止话术","duration":20}]},{"title":"第二章 物料合规","lessons":[{"title":"朋友圈宣传边界","duration":18}]},{"title":"第三章 信息保护","lessons":[{"title":"个险客户数据管理","duration":22}]}]',
         '全体保险代理人（年度必修）',
         '识别并规避常见合规风险；通过课后合规自测',
         'LT90002', 6, 300, 0, 0.00, 0.00, 2140, 318, 1, 0, '2026-07-15', NULL, 15, 2
  UNION ALL SELECT 'CR90005', '长者沟通与家庭会议引导', 1, 'COU001',
         '面向三代同堂家庭的养老决策沟通课：会议准备、角色识别、冲突化解与共识达成。',
         '[{"title":"第一章 家庭会议准备","lessons":[{"title":"会前资料清单","duration":15},{"title":"关键角色识别","duration":20}]},{"title":"第二章 现场引导","lessons":[{"title":"代际冲突调解","duration":26},{"title":"共识落点设计","duration":22}]}]',
         '需要与客户家庭多轮沟通的顾问',
         '主持一场家庭养老决策会议并形成书面共识',
         'LT90001', 8, 420, 365, 199.00, 0.00, 905, 127, 1, 0, '2026-08-12', NULL, 10, 2
  UNION ALL SELECT 'CR90006', '2026 养老金政策与税优实操', 2, 'COU003',
         '直播课：个人养老金账户抵扣、递延纳税与领取测算的最新政策解读。',
         '[{"title":"直播模块","lessons":[{"title":"政策要点直播","duration":90},{"title":"答疑与案例","duration":60}]}]',
         '所有需要讲解税优政策的代理人',
         '掌握个人养老金税优测算方法',
         'LT90003', 4, 150, 90, 299.00, 199.00, 0, 0, 0, 0, '2026-08-20', '2026-08-20', 8, 1
  UNION ALL SELECT 'CR90007', 'CCRC 项目实地踩点方法论', 3, 'COU004',
         '线下课：一天走访 3 家 CCRC 社区，学习踩点评估表使用与供应商洽谈要点。',
         '[{"title":"实地踩点日","lessons":[{"title":"三社区动线走访","duration":180},{"title":"评估表复盘","duration":60}]}]',
         '机构合作与供应商管理岗位',
         '独立完成一份 CCRC 社区评估报告',
         'LT90004', 2, 240, 30, 699.00, 599.00, 320, 12, 0, 0, '2026-07-01', '2026-07-31', 5, 3
  UNION ALL SELECT 'CR90008', '银发客群数字化获客', 4, 'COU002',
         '短视频、直播与私域社群三条线的银发客群获客打法（内容制作中）。',
         NULL,
         '希望拓展线上获客的代理人',
         '搭建一条可持续的内容获客管线',
         'LT90002', 9, 510, 365, 399.00, 299.00, 0, 0, 0, 0, NULL, NULL, 3, 0
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `course_info` c WHERE c.course_code = t.course_code AND c.deleted = 0
);
