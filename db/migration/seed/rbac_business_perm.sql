SET NAMES utf8mb4;
-- =====================================================================
-- rbac_business_perm.sql  订单/权益/管家/财务 四域权限码补齐
--
-- rbac_permission_seed.sql 仅覆盖 park/scene/service/channel/goods 五域，
-- order/equity/finance/butler 四域此前零播种，非超管角色全部 403。
-- 本文件补齐四域全部按钮级权限码（含已有前端页面 + 后端有但前端暂缺的 controller）。
-- 幂等：ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`。超管（is_admin=1）不受影响。
-- =====================================================================

-- ==================== order 域 ====================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`, `path`, `method`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('order:equity:list',         '权益订单列表',   'order:equity', 3, '/admin-api/order/equity',            'GET',    100, 1, '权益订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:equity:query',        '权益订单详情',   'order:equity', 3, '/admin-api/order/equity/*',           'GET',    101, 1, '权益订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:equity:create',       '新增权益订单',   'order:equity', 3, '/admin-api/order/equity',             'POST',   102, 1, '权益订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:equity:pay-callback', '权益订单支付回调','order:equity', 3, '/admin-api/order/equity/*/pay-callback','POST', 103, 1, '权益订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:equity:deliver',      '权益订单发货',   'order:equity', 3, '/admin-api/order/equity/*/deliver',   'POST',   104, 1, '权益订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:equity:complete',     '权益订单完成',   'order:equity', 3, '/admin-api/order/equity/*/complete',  'POST',   105, 1, '权益订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:equity:apply-refund', '权益订单退款',   'order:equity', 3, '/admin-api/order/equity/*/apply-refund','POST',  106, 1, '权益订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:equity:cancel',       '权益订单取消',   'order:equity', 3, '/admin-api/order/equity/*/cancel',    'POST',   107, 1, '权益订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:scene:list',          '场景订单列表',   'order:scene',  3, '/admin-api/order/scene',             'GET',    110, 1, '场景订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:scene:query',         '场景订单详情',   'order:scene',  3, '/admin-api/order/scene/*',            'GET',    111, 1, '场景订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:scene:create',        '新增场景订单',   'order:scene',  3, '/admin-api/order/scene',              'POST',   112, 1, '场景订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:scene:pay-callback',  '场景订单支付回调','order:scene',  3, '/admin-api/order/scene/*/pay-callback','POST', 113, 1, '场景订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:scene:deliver',       '场景订单发货',   'order:scene',  3, '/admin-api/order/scene/*/deliver',    'POST',   114, 1, '场景订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:scene:complete',      '场景订单完成',   'order:scene',  3, '/admin-api/order/scene/*/complete',   'POST',   115, 1, '场景订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:scene:apply-refund',  '场景订单退款',   'order:scene',  3, '/admin-api/order/scene/*/apply-refund','POST',  116, 1, '场景订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:scene:cancel',        '场景订单取消',   'order:scene',  3, '/admin-api/order/scene/*/cancel',     'POST',   117, 1, '场景订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:course:list',         '课程订单列表',   'order:course', 3, '/admin-api/order/course',            'GET',    120, 1, '课程订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:course:query',        '课程订单详情',   'order:course', 3, '/admin-api/order/course/*',           'GET',    121, 1, '课程订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:course:create',       '新增课程订单',   'order:course', 3, '/admin-api/order/course',             'POST',   122, 1, '课程订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:course:pay-callback', '课程订单支付回调','order:course', 3, '/admin-api/order/course/*/pay-callback','POST',123, 1, '课程订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:course:complete',     '课程订单完成',   'order:course', 3, '/admin-api/order/course/*/complete',  'POST',   124, 1, '课程订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:course:apply-refund', '课程订单退款',   'order:course', 3, '/admin-api/order/course/*/apply-refund','POST', 125, 1, '课程订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:course:cancel',       '课程订单取消',   'order:course', 3, '/admin-api/order/course/*/cancel',    'POST',   126, 1, '课程订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:sojourn:list',        '旅游短居订单列表',   'order:sojourn',3, '/admin-api/order/sojourn',           'GET',    130, 1, '旅游短居订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:sojourn:query',       '旅游短居订单详情',   'order:sojourn',3, '/admin-api/order/sojourn/*',          'GET',    131, 1, '旅游短居订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:sojourn:create',      '新增旅游短居订单',   'order:sojourn',3, '/admin-api/order/sojourn',            'POST',   132, 1, '旅游短居订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:sojourn:pay-callback','旅游短居订单支付回调','order:sojourn',3, '/admin-api/order/sojourn/*/pay-callback','POST',133, 1, '旅游短居订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:sojourn:complete',    '旅游短居订单完成',   'order:sojourn',3, '/admin-api/order/sojourn/*/complete', 'POST',   134, 1, '旅游短居订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:sojourn:apply-refund','旅游短居订单退款',   'order:sojourn',3, '/admin-api/order/sojourn/*/apply-refund','POST',135, 1, '旅游短居订单', NOW(), NOW(), 'system', 'system', 0),
  ('order:sojourn:cancel',      '旅游短居订单取消',   'order:sojourn',3, '/admin-api/order/sojourn/*/cancel',   'POST',   136, 1, '旅游短居订单', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ==================== equity 域 ====================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`, `path`, `method`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('equity:batch:list',         '权益批次列表',     'equity:batch', 3, '/admin-api/equity/batch',            'GET',    200, 1, '权益批次', NOW(), NOW(), 'system', 'system', 0),
  ('equity:batch:query',        '权益批次详情',     'equity:batch', 3, '/admin-api/equity/batch/*',          'GET',    201, 1, '权益批次', NOW(), NOW(), 'system', 'system', 0),
  ('equity:batch:stats',        '权益批次统计',     'equity:batch', 3, '/admin-api/equity/batch/*/stats',    'GET',    202, 1, '权益批次', NOW(), NOW(), 'system', 'system', 0),
  ('equity:batch:create',       '新增权益批次',     'equity:batch', 3, '/admin-api/equity/batch',            'POST',   203, 1, '权益批次', NOW(), NOW(), 'system', 'system', 0),
  ('equity:batch:update',       '修改权益批次',     'equity:batch', 3, '/admin-api/equity/batch/*',          'PUT',    204, 1, '权益批次', NOW(), NOW(), 'system', 'system', 0),
  ('equity:batch:delete',       '删除权益批次',     'equity:batch', 3, '/admin-api/equity/batch/*',          'DELETE', 205, 1, '权益批次', NOW(), NOW(), 'system', 'system', 0),
  ('equity:depot:list',         '权益仓库列表',     'equity:depot', 3, '/admin-api/equity/depot',            'GET',    210, 1, '权益仓库', NOW(), NOW(), 'system', 'system', 0),
  ('equity:depot:query',        '权益仓库详情',     'equity:depot', 3, '/admin-api/equity/depot/*',          'GET',    211, 1, '权益仓库', NOW(), NOW(), 'system', 'system', 0),
  ('equity:depot:stock-in',     '权益入库',         'equity:depot', 3, '/admin-api/equity/depot/stock-in',   'POST',   212, 1, '权益仓库', NOW(), NOW(), 'system', 'system', 0),
  ('equity:depot:outbound',     '权益出库',         'equity:depot', 3, '/admin-api/equity/depot/outbound',   'POST',   213, 1, '权益仓库', NOW(), NOW(), 'system', 'system', 0),
  ('equity:depot:activate',     '权益激活',         'equity:depot', 3, '/admin-api/equity/depot/activate',   'POST',   214, 1, '权益仓库', NOW(), NOW(), 'system', 'system', 0),
  ('equity:depot:void',         '权益作废',         'equity:depot', 3, '/admin-api/equity/depot/void',       'POST',   215, 1, '权益仓库', NOW(), NOW(), 'system', 'system', 0),
  ('equity:depot:change-holder','权益换持有人',     'equity:depot', 3, '/admin-api/equity/depot/change-holder','POST', 216, 1, '权益仓库', NOW(), NOW(), 'system', 'system', 0),
  ('equity:depot:change-done',  '换持有人完成',     'equity:depot', 3, '/admin-api/equity/depot/change-done','POST',   217, 1, '权益仓库', NOW(), NOW(), 'system', 'system', 0),
  ('equity:depot:change-rollback','换持有人回滚',   'equity:depot', 3, '/admin-api/equity/depot/change-rollback','POST',218, 1, '权益仓库', NOW(), NOW(), 'system', 'system', 0),
  ('equity:depot:transition',   '权益状态流转',     'equity:depot', 3, '/admin-api/equity/depot/transition', 'POST',   219, 1, '权益仓库', NOW(), NOW(), 'system', 'system', 0),
  ('equity:activate:list',      '权益激活记录列表', 'equity:activate', 3, '/admin-api/equity/activate',      'GET',    220, 1, '权益激活', NOW(), NOW(), 'system', 'system', 0),
  ('equity:activate:query',     '权益激活记录详情', 'equity:activate', 3, '/admin-api/equity/activate/*',     'GET',    221, 1, '权益激活', NOW(), NOW(), 'system', 'system', 0),
  ('equity:change-holder:list', '换持有人记录列表', 'equity:change-holder', 3, '/admin-api/equity/change-holder','GET',225, 1, '换持有人', NOW(), NOW(), 'system', 'system', 0),
  ('equity:change-holder:query','换持有人记录详情', 'equity:change-holder', 3, '/admin-api/equity/change-holder/*','GET',226, 1, '换持有人', NOW(), NOW(), 'system', 'system', 0),
  ('equity:use-person:list',    '权益人员列表',     'equity:use-person', 3, '/admin-api/equity/use-person',  'GET',    230, 1, '权益人员', NOW(), NOW(), 'system', 'system', 0),
  ('equity:use-person:query',   '权益人员详情',     'equity:use-person', 3, '/admin-api/equity/use-person/*', 'GET',   231, 1, '权益人员', NOW(), NOW(), 'system', 'system', 0),
  ('equity:use-person:set-default','设为默认权益人员','equity:use-person', 3, '/admin-api/equity/use-person/set-default','PUT',232, 1, '权益人员', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ==================== butler 域 ====================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`, `path`, `method`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('butler:info:list',     '管家列表',     'butler:info', 3, '/admin-api/butler/info',           'GET',    300, 1, '管家信息', NOW(), NOW(), 'system', 'system', 0),
  ('butler:info:query',    '管家详情',     'butler:info', 3, '/admin-api/butler/info/*',         'GET',    301, 1, '管家信息', NOW(), NOW(), 'system', 'system', 0),
  ('butler:info:create',   '新增管家',     'butler:info', 3, '/admin-api/butler/info',           'POST',   302, 1, '管家信息', NOW(), NOW(), 'system', 'system', 0),
  ('butler:info:update',   '修改管家',     'butler:info', 3, '/admin-api/butler/info/*',         'PUT',    303, 1, '管家信息', NOW(), NOW(), 'system', 'system', 0),
  ('butler:info:delete',   '删除管家',     'butler:info', 3, '/admin-api/butler/info/*',         'DELETE', 304, 1, '管家信息', NOW(), NOW(), 'system', 'system', 0),
  ('butler:account:list',  '管家账户列表', 'butler:account', 3, '/admin-api/butler/account',    'GET',    310, 1, '管家账户', NOW(), NOW(), 'system', 'system', 0),
  ('butler:account:create','新增管家账户', 'butler:account', 3, '/admin-api/butler/account',     'POST',   311, 1, '管家账户', NOW(), NOW(), 'system', 'system', 0),
  ('butler:account:update','修改管家账户', 'butler:account', 3, '/admin-api/butler/account/*',   'PUT',    312, 1, '管家账户', NOW(), NOW(), 'system', 'system', 0),
  ('butler:account:delete','删除管家账户', 'butler:account', 3, '/admin-api/butler/account/*',   'DELETE', 313, 1, '管家账户', NOW(), NOW(), 'system', 'system', 0),
  ('butler:account:reset', '重置管家密码', 'butler:account', 3, '/admin-api/butler/account/*/reset-password','PUT',314, 1, '管家账户', NOW(), NOW(), 'system', 'system', 0),
  ('butler:skill:list',    '管家技能列表', 'butler:skill', 3, '/admin-api/butler/skill',          'GET',    320, 1, '管家技能', NOW(), NOW(), 'system', 'system', 0),
  ('butler:skill:create',  '新增管家技能', 'butler:skill', 3, '/admin-api/butler/skill',          'POST',   321, 1, '管家技能', NOW(), NOW(), 'system', 'system', 0),
  ('butler:skill:update',  '修改管家技能', 'butler:skill', 3, '/admin-api/butler/skill/*',        'PUT',    322, 1, '管家技能', NOW(), NOW(), 'system', 'system', 0),
  ('butler:skill:delete',  '删除管家技能', 'butler:skill', 3, '/admin-api/butler/skill/*',        'DELETE', 323, 1, '管家技能', NOW(), NOW(), 'system', 'system', 0),
  ('butler:rating:list',   '管家评分列表', 'butler:rating', 3, '/admin-api/butler/rating',        'GET',    330, 1, '管家评分', NOW(), NOW(), 'system', 'system', 0),
  ('butler:rating:create', '新增管家评分', 'butler:rating', 3, '/admin-api/butler/rating',        'POST',   331, 1, '管家评分', NOW(), NOW(), 'system', 'system', 0),
  ('butler:rating:update', '修改管家评分', 'butler:rating', 3, '/admin-api/butler/rating/*',      'PUT',    332, 1, '管家评分', NOW(), NOW(), 'system', 'system', 0),
  ('butler:rating:delete', '删除管家评分', 'butler:rating', 3, '/admin-api/butler/rating/*',      'DELETE', 333, 1, '管家评分', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ==================== finance 域 ====================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`, `path`, `method`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('finance:flow:list',    '财务流水列表', 'finance:flow', 3, '/admin-api/finance/flow',          'GET',    400, 1, '财务流水', NOW(), NOW(), 'system', 'system', 0),
  ('finance:flow:query',   '财务流水详情', 'finance:flow', 3, '/admin-api/finance/flow/*',         'GET',    401, 1, '财务流水', NOW(), NOW(), 'system', 'system', 0),
  ('finance:flow:record',  '记录财务流水', 'finance:flow', 3, '/admin-api/finance/flow/record',    'POST',   402, 1, '财务流水', NOW(), NOW(), 'system', 'system', 0),
  ('finance:bill:list',    '结算单列表',   'finance:bill', 3, '/admin-api/finance/bill',          'GET',    410, 1, '结算单', NOW(), NOW(), 'system', 'system', 0),
  ('finance:bill:query',   '结算单详情',   'finance:bill', 3, '/admin-api/finance/bill/*',         'GET',    411, 1, '结算单', NOW(), NOW(), 'system', 'system', 0),
  ('finance:bill:generate','生成结算单',   'finance:bill', 3, '/admin-api/finance/bill/generate',  'POST',   412, 1, '结算单', NOW(), NOW(), 'system', 'system', 0),
  ('finance:bill:audit',   '审核结算单',   'finance:bill', 3, '/admin-api/finance/bill/audit',     'POST',   413, 1, '结算单', NOW(), NOW(), 'system', 'system', 0),
  ('finance:bill:start-settle','开始结算', 'finance:bill', 3, '/admin-api/finance/bill/start-settle','POST', 414, 1, '结算单', NOW(), NOW(), 'system', 'system', 0),
  ('finance:bill:finish-settle','完成结算','finance:bill', 3, '/admin-api/finance/bill/finish-settle','POST',415, 1, '结算单', NOW(), NOW(), 'system', 'system', 0),
  ('finance:invoice:list', '发票列表',     'finance:invoice', 3, '/admin-api/finance/invoice',    'GET',    420, 1, '发票', NOW(), NOW(), 'system', 'system', 0),
  ('finance:invoice:query','发票详情',     'finance:invoice', 3, '/admin-api/finance/invoice/*',  'GET',    421, 1, '发票', NOW(), NOW(), 'system', 'system', 0),
  ('finance:invoice:apply','申请发票',     'finance:invoice', 3, '/admin-api/finance/invoice/apply','POST', 422, 1, '发票', NOW(), NOW(), 'system', 'system', 0),
  ('finance:invoice:audit','审核发票',     'finance:invoice', 3, '/admin-api/finance/invoice/audit','POST', 423, 1, '发票', NOW(), NOW(), 'system', 'system', 0),
  ('finance:invoice:issue','开票',         'finance:invoice', 3, '/admin-api/finance/invoice/issue','POST', 424, 1, '发票', NOW(), NOW(), 'system', 'system', 0),
  ('finance:invoice:send', '寄出发票',     'finance:invoice', 3, '/admin-api/finance/invoice/send','POST',  425, 1, '发票', NOW(), NOW(), 'system', 'system', 0),
  ('finance:invoice:finish','完成发票',    'finance:invoice', 3, '/admin-api/finance/invoice/finish','POST',426, 1, '发票', NOW(), NOW(), 'system', 'system', 0),
  ('finance:invoice:void', '作废发票',     'finance:invoice', 3, '/admin-api/finance/invoice/void','POST',  427, 1, '发票', NOW(), NOW(), 'system', 'system', 0),
  ('finance:invoice:red-flush','红冲发票', 'finance:invoice', 3, '/admin-api/finance/invoice/red-flush','POST',428, 1, '发票', NOW(), NOW(), 'system', 'system', 0),
  ('finance:account:list', '财务账户列表', 'finance:account', 3, '/admin-api/finance/account',    'GET',    430, 1, '财务账户', NOW(), NOW(), 'system', 'system', 0),
  ('finance:account:query','财务账户详情', 'finance:account', 3, '/admin-api/finance/account/*',  'GET',    431, 1, '财务账户', NOW(), NOW(), 'system', 'system', 0),
  ('finance:account:create','新增财务账户', 'finance:account', 3, '/admin-api/finance/account',   'POST',   432, 1, '财务账户', NOW(), NOW(), 'system', 'system', 0),
  ('finance:account:update','修改财务账户', 'finance:account', 3, '/admin-api/finance/account/*', 'PUT',    433, 1, '财务账户', NOW(), NOW(), 'system', 'system', 0),
  ('finance:account:delete','删除财务账户', 'finance:account', 3, '/admin-api/finance/account/*', 'DELETE', 434, 1, '财务账户', NOW(), NOW(), 'system', 'system', 0),
  ('finance:account:receive','账户收款',   'finance:account', 3, '/admin-api/finance/account/receive','POST',435, 1, '财务账户', NOW(), NOW(), 'system', 'system', 0),
  ('finance:refund:list',  '退款列表',     'finance:refund', 3, '/admin-api/finance/refund',       'GET',    440, 1, '退款', NOW(), NOW(), 'system', 'system', 0),
  ('finance:refund:query', '退款详情',     'finance:refund', 3, '/admin-api/finance/refund/*',     'GET',    441, 1, '退款', NOW(), NOW(), 'system', 'system', 0),
  ('finance:refund:apply', '申请退款',     'finance:refund', 3, '/admin-api/finance/refund/apply', 'POST',   442, 1, '退款', NOW(), NOW(), 'system', 'system', 0),
  ('finance:refund:audit', '审核退款',     'finance:refund', 3, '/admin-api/finance/refund/audit', 'POST',   443, 1, '退款', NOW(), NOW(), 'system', 'system', 0),
  ('finance:refund:mark-refunding','标记退款中','finance:refund', 3, '/admin-api/finance/refund/mark-refunding','POST',444, 1, '退款', NOW(), NOW(), 'system', 'system', 0),
  ('finance:refund:mark-success','标记退款成功','finance:refund', 3, '/admin-api/finance/refund/mark-success','POST',445, 1, '退款', NOW(), NOW(), 'system', 'system', 0),
  ('finance:refund:mark-failed','标记退款失败','finance:refund', 3, '/admin-api/finance/refund/mark-failed','POST',446, 1, '退款', NOW(), NOW(), 'system', 'system', 0),
  ('finance:reconciliation:list','对账列表','finance:reconciliation', 3, '/admin-api/finance/reconciliation','GET',450, 1, '对账', NOW(), NOW(), 'system', 'system', 0),
  ('finance:reconciliation:query','对账详情','finance:reconciliation', 3, '/admin-api/finance/reconciliation/*','GET',451, 1, '对账', NOW(), NOW(), 'system', 'system', 0),
  ('finance:payment:list', '支付记录列表', 'finance:payment', 3, '/admin-api/finance/payment',     'GET',    460, 1, '支付记录', NOW(), NOW(), 'system', 'system', 0),
  ('finance:payment:query','支付记录详情', 'finance:payment', 3, '/admin-api/finance/payment/*',   'GET',    461, 1, '支付记录', NOW(), NOW(), 'system', 'system', 0),
  ('finance:payment:create','新增支付记录', 'finance:payment', 3, '/admin-api/finance/payment',    'POST',   462, 1, '支付记录', NOW(), NOW(), 'system', 'system', 0),
  ('finance:payment:mark-success','标记支付成功','finance:payment', 3, '/admin-api/finance/payment/mark-success','POST',463, 1, '支付记录', NOW(), NOW(), 'system', 'system', 0),
  ('finance:payment:mark-failed','标记支付失败','finance:payment', 3, '/admin-api/finance/payment/mark-failed','POST',464, 1, '支付记录', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;
