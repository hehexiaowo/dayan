// Main entry: invoke skeleton builder + generate all 127 tables' entity/mapper
const path = require('path');
const {
  MODULES_ROOT, MODULES, buildModuleSkeleton, writeEntity, writeMapper, mapType,
} = require('./gen-modules.js');

// Audit fields in BaseEntity -> skip. 'id' is generated as PK in entity template -> skip.
const SKIP_FIELDS = new Set([
  'id', 'created_at', 'updated_at', 'creator', 'updater', 'deleted_at', 'deleted',
]);
function f(rows) {
  return rows
    .filter(([name]) => !SKIP_FIELDS.has(name))
    .map(([name, type, comment]) => ({ name, type: mapType(type), comment }));
}

// ======== ALL 127 TABLES — fields preserved per docs/02 ========
// Format: '<table>' : f([ [col,type,cmt], ... ])

const T = {};

// ----- system_ (18) -----
T['system_dict_common'] = f([
  ['dict_type','varchar(50)','字典类型'],['dict_code','varchar(50)','字典编码'],
  ['dict_name','varchar(100)','字典显示名称'],['dict_value','varchar(100)','字典存储值'],
  ['parent_code','varchar(50)','父级编码'],['level','tinyint(2)','层级'],
  ['sort_order','int(11)','排序号'],['icon','varchar(100)','图标'],
  ['css_class','varchar(100)','样式类名'],['status','tinyint(1)','状态'],
  ['is_default','tinyint(1)','是否默认项'],['remark','varchar(500)','备注'],
]);
T['system_dict_region'] = f([
  ['region_code','varchar(20)','行政区划代码'],['region_name','varchar(100)','区划名称'],
  ['parent_code','varchar(20)','父级区划代码'],['level','tinyint(2)','层级'],
  ['pinyin','varchar(200)','拼音'],['first_letter','char(1)','首字母'],
  ['lng','decimal(10,6)','经度'],['lat','decimal(10,6)','纬度'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
]);
T['system_dict_iplocation'] = f([
  ['ip_start','varchar(50)','IP起始地址'],['ip_end','varchar(50)','IP结束地址'],
  ['ip_start_num','bigint(20)','IP起始数值'],['ip_end_num','bigint(20)','IP结束数值'],
  ['country','varchar(50)','国家'],['province','varchar(50)','省份'],
  ['city','varchar(50)','城市'],['district','varchar(50)','区/县'],
  ['isp','varchar(100)','运营商'],['region_code','varchar(20)','关联区划代码'],
]);
T['system_state_machine'] = f([
  ['machine_code','varchar(50)','状态机编码'],['machine_name','varchar(100)','状态机名称'],
  ['biz_type','varchar(50)','业务类型'],['from_state','tinyint(4)','源主状态值'],
  ['from_state_name','varchar(50)','源主状态名称'],['from_sub_state','varchar(20)','源子状态值'],
  ['to_state','tinyint(4)','目标主状态值'],['to_state_name','varchar(50)','目标主状态名称'],
  ['to_sub_state','varchar(20)','目标子状态值'],['event_code','varchar(50)','触发事件编码'],
  ['event_name','varchar(100)','触发事件名称'],['condition_expr','varchar(500)','流转条件表达式'],
  ['action_bean','varchar(200)','流转执行器'],['sort_order','int(11)','排序号'],
  ['status','tinyint(1)','状态'],['remark','varchar(500)','备注'],
]);
T['system_log_organ'] = f([
  ['organ_code','varchar(50)','组织编码'],['account_code','varchar(50)','操作账号编码'],
  ['account_name','varchar(100)','操作人姓名'],['module','varchar(50)','操作模块'],
  ['action','varchar(50)','操作动作'],['target_type','varchar(50)','操作对象类型'],
  ['target_code','varchar(50)','操作对象编码'],['content','text','操作内容描述'],
  ['before_data','text','变更前数据'],['after_data','text','变更后数据'],
  ['ip_address','varchar(50)','操作IP地址'],['user_agent','varchar(500)','浏览器UA'],
  ['request_url','varchar(500)','请求URL'],['request_method','varchar(10)','请求方法'],
  ['result_status','tinyint(1)','结果状态'],['error_msg','text','错误信息'],
  ['duration','int(11)','执行耗时(毫秒)'],
]);
T['system_log_supplier'] = f([
  ['supplier_code','varchar(50)','供应商编码'],['account_code','varchar(50)','操作账号编码'],
  ['account_name','varchar(100)','操作人姓名'],['module','varchar(50)','操作模块'],
  ['action','varchar(50)','操作动作'],['target_type','varchar(50)','操作对象类型'],
  ['target_code','varchar(50)','操作对象编码'],['content','text','操作内容描述'],
  ['before_data','text','变更前数据'],['after_data','text','变更后数据'],
  ['ip_address','varchar(50)','操作IP地址'],['user_agent','varchar(500)','浏览器UA'],
  ['result_status','tinyint(1)','结果状态'],['error_msg','text','错误信息'],
]);
T['system_log_channel'] = f([
  ['channel_code','varchar(50)','渠道编码'],['account_code','varchar(50)','操作账号编码'],
  ['account_name','varchar(100)','操作人姓名'],['module','varchar(50)','操作模块'],
  ['action','varchar(50)','操作动作'],['target_type','varchar(50)','操作对象类型'],
  ['target_code','varchar(50)','操作对象编码'],['content','text','操作内容描述'],
  ['before_data','text','变更前数据'],['after_data','text','变更后数据'],
  ['ip_address','varchar(50)','操作IP地址'],['user_agent','varchar(500)','浏览器UA'],
  ['result_status','tinyint(1)','结果状态'],['error_msg','text','错误信息'],
]);
T['system_message_template'] = f([
  ['template_code','varchar(50)','模板编码'],['template_name','varchar(100)','模板名称'],
  ['biz_type','varchar(50)','业务类型'],['channel_type','tinyint(2)','渠道类型'],
  ['title','varchar(200)','消息标题'],['content','text','模板正文'],
  ['variables','text','变量定义'],['channel_config','text','渠道差异配置'],
  ['fallback_channel_type','tinyint(2)','降级渠道'],['channel_code','varchar(50)','渠道编码'],
  ['status','tinyint(1)','状态'],['sort_order','int(11)','排序号'],
  ['remark','varchar(500)','备注'],
]);
T['system_config'] = f([
  ['config_group','varchar(50)','配置分组'],['config_key','varchar(100)','配置键'],
  ['config_value','text','配置值'],['value_type','varchar(20)','值类型'],
  ['env','varchar(20)','环境'],['scope','varchar(20)','配置作用域'],
  ['organ_code','varchar(50)','组织编码'],['user_code','varchar(50)','用户/账号编码'],
  ['config_name','varchar(100)','配置名称'],['description','varchar(500)','配置说明'],
  ['is_secret','tinyint(1)','是否敏感配置'],['is_runtime','tinyint(1)','是否运行时热更新'],
  ['sort_order','int(11)','排序号'],
]);
T['system_config_log'] = f([
  ['config_id','bigint(20)','关联system_config.id'],['config_group','varchar(50)','配置分组'],
  ['config_key','varchar(100)','配置键'],['env','varchar(20)','环境'],
  ['old_value','text','变更前值'],['new_value','text','变更后值'],
  ['action','varchar(20)','操作类型'],['account_type','varchar(30)','操作账号类型'],
  ['account_code','varchar(50)','操作账号编码'],
]);
T['system_menu'] = f([
  ['menu_code','varchar(50)','菜单编码'],['menu_name','varchar(100)','菜单名称'],
  ['parent_code','varchar(50)','父菜单编码'],['menu_type','tinyint(2)','菜单类型'],
  ['path','varchar(200)','路由路径'],['component','varchar(200)','前端组件路径'],
  ['permission_code','varchar(100)','权限标识'],['icon','varchar(100)','菜单图标'],
  ['sort_order','int(11)','排序号'],['is_visible','tinyint(1)','是否可见'],
  ['is_external','tinyint(1)','是否外链'],['is_cache','tinyint(1)','是否缓存'],
  ['domain_type','varchar(30)','所属域'],['status','tinyint(1)','状态'],
  ['remark','varchar(500)','备注'],
]);
T['system_operation_log'] = f([
  ['trace_id','varchar(64)','链路追踪ID'],['account_type','varchar(30)','账号类型'],
  ['account_code','varchar(50)','操作账号编码'],['account_name','varchar(100)','操作人姓名'],
  ['module','varchar(50)','操作模块'],['action','varchar(50)','操作动作'],
  ['action_description','varchar(200)','操作描述'],['target_type','varchar(50)','操作对象类型'],
  ['target_code','varchar(50)','操作对象编码'],['target_description','varchar(200)','操作对象描述'],
  ['request_url','varchar(500)','请求URL'],['request_method','varchar(10)','请求方法'],
  ['request_params','text','请求参数'],['response_code','int(11)','响应状态码'],
  ['ip_address','varchar(50)','操作IP地址'],['ip_location','varchar(200)','IP归属地'],
  ['user_agent','varchar(500)','浏览器UA'],['device_type','varchar(20)','设备类型'],
  ['os','varchar(50)','操作系统'],['browser','varchar(50)','浏览器'],
  ['result_status','tinyint(1)','结果'],['error_msg','text','错误信息'],
  ['duration','int(11)','执行耗时(毫秒)'],
]);
T['system_login_log'] = f([
  ['client_code','varchar(50)','客户编码'],['login_type','tinyint(2)','登录方式'],
  ['phone','varchar(20)','登录手机号'],['open_id','varchar(100)','第三方OpenID'],
  ['login_ip','varchar(50)','登录IP'],['login_location','varchar(100)','登录地域'],
  ['device_type','tinyint(2)','设备类型'],['device_info','varchar(500)','设备信息'],
  ['result','tinyint(2)','结果'],['fail_reason','varchar(200)','失败原因'],
  ['login_time','datetime','登录时间'],
]);
T['system_service_change_log'] = f([
  ['session_code','varchar(64)','服务会话编码'],['change_type','tinyint(2)','变更类型'],
  ['from_value','varchar(500)','变更前值'],['to_value','varchar(500)','变更后值'],
  ['change_reason','varchar(500)','变更原因'],['operator_code','varchar(64)','操作人编码'],
  ['operator_name','varchar(50)','操作人姓名'],['operator_type','varchar(30)','操作人类型'],
  ['operate_time','datetime','操作时间'],['remark','varchar(500)','备注'],
]);
T['system_order_status_log'] = f([
  ['order_type','tinyint(2)','订单类型'],['order_code','varchar(50)','订单编号'],
  ['from_status','tinyint(4)','原状态'],['to_status','tinyint(4)','新状态'],
  ['change_reason','varchar(500)','变更原因'],['operator_code','varchar(64)','操作人编码'],
  ['operator_name','varchar(50)','操作人姓名'],['operator_type','varchar(30)','操作人类型'],
  ['operate_time','datetime','操作时间'],['remark','varchar(500)','备注'],
]);
T['system_message'] = f([
  ['message_code','varchar(50)','消息实例编码'],['batch_code','varchar(50)','发送批次编码'],
  ['template_code','varchar(50)','模板编码'],['biz_type','varchar(50)','业务类型'],
  ['channel_type','tinyint(2)','实际发送渠道'],['message_type','tinyint(2)','消息类型'],
  ['title','varchar(200)','消息标题'],['content','text','消息正文'],
  ['target_type','varchar(30)','接收者类型'],['target_code','varchar(50)','接收者编码'],
  ['target_name','varchar(100)','接收者名称'],['target_contact','varchar(100)','接收者联系方式'],
  ['sender_type','varchar(30)','发送者类型'],['sender_code','varchar(50)','发送者编码'],
  ['link_url','varchar(500)','跳转链接'],['link_type','tinyint(2)','链接类型'],
  ['send_status','tinyint(2)','发送状态'],['provider_msg_id','varchar(100)','第三方服务商消息ID'],
  ['send_time','datetime','实际发送时间'],['deliver_time','datetime','送达时间'],
  ['read_time','datetime','已读时间'],['expire_time','datetime','过期时间'],
  ['retry_count','int(11)','重试次数'],['error_code','varchar(50)','失败错误码'],
  ['error_msg','varchar(500)','失败原因'],['priority','tinyint(2)','优先级'],
]);
T['system_message_read'] = f([
  ['message_code','varchar(50)','消息编码'],['account_type','varchar(30)','账号类型'],
  ['account_code','varchar(50)','接收者编码'],['is_read','tinyint(1)','是否已读'],
  ['read_time','datetime','阅读时间'],
]);
T['system_dict_business'] = f([
  ['dict_type','varchar(64)','字典类型'],['dict_code','varchar(64)','字典编码'],
  ['dict_name','varchar(128)','字典显示名称'],['dict_value','varchar(128)','字典存储值'],
  ['parent_code','varchar(64)','父级编码'],['domain','varchar(32)','所属业务域'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
  ['remark','varchar(500)','备注'],
]);

// ----- organ_ (9) -----
T['organ_info'] = f([
  ['organ_code','varchar(50)','组织编码'],['full_name','varchar(200)','组织全称'],
  ['short_name','varchar(50)','简称'],['organ_type','tinyint(2)','组织类型'],
  ['unified_credit_code','varchar(50)','统一社会信用代码'],['legal_person','varchar(50)','法定代表人'],
  ['registered_capital','decimal(12,2)','注册资本(万元)'],['establish_date','date','成立日期'],
  ['business_scope','text','经营范围'],['province_code','varchar(20)','省份编码'],
  ['city_code','varchar(20)','城市编码'],['district_code','varchar(20)','区划编码'],
  ['address','varchar(500)','详细地址'],['contact_person','varchar(50)','联系人'],
  ['contact_phone','varchar(20)','联系电话'],['contact_email','varchar(100)','联系邮箱'],
  ['logo_url','varchar(500)','Logo图片URL'],['website','varchar(200)','官网地址'],
  ['description','text','组织介绍'],['license_image','varchar(500)','营业执照图片URL'],
  ['status','tinyint(2)','状态'],['sort_order','int(11)','排序号'],
  ['remark','varchar(500)','备注'],
]);
T['organ_account'] = f([
  ['organ_code','varchar(50)','所属组织编码'],['account_code','varchar(50)','账号编码'],
  ['username','varchar(50)','登录用户名'],['password','varchar(200)','密码'],
  ['salt','varchar(50)','密码盐值'],['real_name','varchar(50)','真实姓名'],
  ['avatar','varchar(500)','头像URL'],['gender','tinyint(1)','性别'],
  ['phone','varchar(20)','手机号'],['open_id','varchar(100)','微信OpenID'],
  ['union_id','varchar(100)','微信UnionID'],['email','varchar(100)','邮箱'],
  ['id_card','varchar(20)','身份证号'],['last_login_time','datetime','最后登录时间'],
  ['last_login_ip','varchar(50)','最后登录IP'],['login_count','int(11)','累计登录次数'],
  ['pwd_update_time','datetime','密码修改时间'],['account_status','tinyint(2)','账号状态'],
  ['is_admin','tinyint(1)','是否超级管理员'],['remark','varchar(500)','备注'],
]);
T['organ_role'] = f([
  ['organ_code','varchar(50)','所属组织编码'],['role_code','varchar(50)','角色编码'],
  ['role_name','varchar(100)','角色名称'],['role_type','tinyint(2)','角色类型'],
  ['description','varchar(500)','角色描述'],['data_scope','tinyint(2)','数据范围'],
  ['status','tinyint(1)','状态'],['sort_order','int(11)','排序号'],
]);
T['organ_permission'] = f([
  ['permission_code','varchar(100)','权限编码'],['permission_name','varchar(100)','权限名称'],
  ['parent_code','varchar(50)','父权限编码'],['permission_type','tinyint(2)','权限类型'],
  ['path','varchar(200)','路由/接口路径'],['method','varchar(20)','请求方法'],
  ['icon','varchar(100)','图标'],['sort_order','int(11)','排序号'],
  ['status','tinyint(1)','状态'],['remark','varchar(500)','备注'],
]);
T['organ_role_permission_ship'] = f([
  ['role_code','varchar(64)','角色编码'],['permission_code','varchar(64)','权限编码'],
]);
T['organ_department'] = f([
  ['organ_code','varchar(50)','所属组织编码'],['dept_code','varchar(50)','部门编码'],
  ['dept_name','varchar(100)','部门名称'],['parent_code','varchar(50)','父部门编码'],
  ['ancestors','varchar(500)','祖级列表'],['dept_type','tinyint(2)','部门类型'],
  ['leader_name','varchar(50)','负责人姓名'],['leader_phone','varchar(20)','负责人电话'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
  ['remark','varchar(500)','备注'],
]);
T['organ_employee'] = f([
  ['organ_code','varchar(50)','所属组织编码'],['employee_code','varchar(50)','员工编码'],
  ['account_code','varchar(64)','关联账号编码'],['dept_code','varchar(64)','所属部门编码'],
  ['real_name','varchar(50)','真实姓名'],['gender','tinyint(1)','性别'],
  ['phone','varchar(20)','手机号'],['email','varchar(100)','邮箱'],
  ['id_card','varchar(20)','身份证号'],['position','varchar(100)','职位'],
  ['entry_date','date','入职日期'],['leave_date','date','离职日期'],
  ['avatar','varchar(500)','头像URL'],['employee_status','tinyint(2)','员工状态'],
  ['remark','varchar(500)','备注'],
]);
T['organ_account_role_rel'] = f([
  ['account_code','varchar(50)','账号编码'],['role_code','varchar(50)','角色编码'],
  ['organ_code','varchar(50)','所属组织编码'],
]);
T['organ_role_menu_rel'] = f([
  ['role_code','varchar(50)','角色编码'],['menu_code','varchar(50)','菜单编码'],
  ['organ_code','varchar(50)','所属组织编码'],
]);

// ----- butler_ (8) -----
T['butler_info'] = f([
  ['butler_code','varchar(50)','管家编码'],['full_name','varchar(50)','管家姓名'],
  ['phone','varchar(32)','手机号'],['avatar','varchar(500)','头像URL'],
  ['organ_code','varchar(50)','所属组织编码'],['butler_level','tinyint(2)','管家等级'],
  ['status','tinyint(2)','状态'],['remark','varchar(500)','备注'],
]);
T['butler_account'] = f([
  ['butler_code','varchar(50)','管家编码'],['username','varchar(50)','登录用户名'],
  ['phone','varchar(20)','手机号'],['password','varchar(200)','密码'],
  ['salt','varchar(50)','密码盐值'],['open_id','varchar(100)','微信OpenID'],
  ['union_id','varchar(100)','微信UnionID'],['last_login_time','datetime','最后登录时间'],
  ['account_status','tinyint(2)','账号状态'],
]);
T['butler_schedule'] = f([
  ['butler_code','varchar(50)','管家编码'],['schedule_date','date','排班日期'],
  ['schedule_type','tinyint(2)','排班类型'],['start_time','time','上班时间'],
  ['end_time','time','下班时间'],['status','tinyint(1)','状态'],
]);
T['butler_client_rel'] = f([
  ['butler_code','varchar(50)','管家编码'],['client_code','varchar(50)','客户编码'],
  ['bind_time','datetime','绑定时间'],['status','tinyint(1)','状态'],
]);
T['butler_service_record'] = f([
  ['butler_code','varchar(50)','管家编码'],['client_code','varchar(50)','客户编码'],
  ['service_type','tinyint(2)','服务类型'],['service_title','varchar(200)','服务标题'],
  ['service_date','date','服务日期'],['status','tinyint(2)','状态'],
  ['communicate_way','tinyint(2)','沟通方式'],['remark','varchar(500)','备注'],
]);
T['butler_rating'] = f([
  ['butler_code','varchar(50)','管家编码'],['client_code','varchar(50)','客户编码'],
  ['service_record_code','varchar(64)','关联服务记录编码'],['rating','tinyint(1)','评分'],
  ['content','text','评价内容'],['status','tinyint(1)','状态'],
]);
T['butler_account_role_rel'] = f([
  ['account_code','varchar(64)','管家账号编码'],['butler_code','varchar(50)','管家编码'],
  ['role_type','tinyint(2)','角色类型'],['description','varchar(200)','角色描述'],
]);
T['butler_skill'] = f([
  ['butler_code','varchar(50)','管家编码'],['skill_code','varchar(50)','技能编码'],
  ['skill_name','varchar(100)','技能名称'],['proficiency','tinyint(2)','熟练度'],
  ['is_certified','tinyint(1)','是否持证'],['certificate_no','varchar(100)','证书编号'],
  ['obtain_date','date','取得日期'],['sort_order','int(11)','排序号'],
]);

// ----- supplier_ (10) -----
T['supplier_info'] = f([
  ['supplier_code','varchar(50)','供应商编码'],['full_name','varchar(200)','供应商全称'],
  ['short_name','varchar(50)','简称'],['supplier_type','tinyint(2)','供应商类型'],
  ['unified_credit_code','varchar(50)','统一社会信用代码'],['legal_person','varchar(50)','法定代表人'],
  ['registered_capital','decimal(12,2)','注册资本(万元)'],['establish_date','date','成立日期'],
  ['business_license_no','varchar(100)','营业执照编号'],['business_scope','text','经营范围'],
  ['province_code','varchar(20)','省份编码'],['city_code','varchar(20)','城市编码'],
  ['district_code','varchar(20)','区划编码'],['address','varchar(500)','详细地址'],
  ['contact_person','varchar(50)','联系人'],['contact_phone','varchar(20)','联系电话'],
  ['contact_email','varchar(100)','联系邮箱'],['logo_url','varchar(500)','Logo图片URL'],
  ['description','text','供应商介绍'],['license_image','varchar(500)','营业执照图片URL'],
  ['qualification_image','varchar(500)','资质证书图片URL'],['bank_name','varchar(100)','开户银行'],
  ['bank_account','varchar(50)','银行账号'],['bank_account_name','varchar(100)','银行户名'],
  ['park_count','int(11)','下属机构数量'],['cooperation_start_date','date','合作开始日期'],
  ['cooperation_end_date','date','合作结束日期'],['commission_rate','decimal(5,4)','默认佣金比例'],
  ['status','tinyint(2)','状态'],['audit_status','tinyint(2)','审核状态'],
  ['audit_remark','varchar(500)','审核备注'],['sort_order','int(11)','排序号'],
  ['remark','varchar(500)','备注'],
]);
T['supplier_open_platform'] = f([
  ['supplier_code','varchar(50)','供应商编码'],['platform_name','varchar(100)','平台名称'],
  ['api_base_url','varchar(500)','API基础地址'],['app_key','varchar(100)','应用Key'],
  ['app_secret','varchar(200)','应用密钥'],['callback_url','varchar(500)','回调地址'],
  ['webhook_secret','varchar(200)','Webhook密钥'],['protocol_type','tinyint(2)','协议类型'],
  ['auth_type','tinyint(2)','认证方式'],['data_format','tinyint(2)','数据格式'],
  ['api_version','varchar(20)','API版本'],['rate_limit','int(11)','调用频率限制'],
  ['timeout','int(11)','超时时间(秒)'],['retry_count','int(11)','重试次数'],
  ['extra_config','text','扩展配置'],['status','tinyint(1)','状态'],
]);
T['supplier_account'] = f([
  ['supplier_code','varchar(50)','供应商编码'],['account_code','varchar(50)','账号编码'],
  ['username','varchar(50)','登录用户名'],['password','varchar(200)','密码'],
  ['salt','varchar(50)','密码盐值'],['real_name','varchar(50)','真实姓名'],
  ['avatar','varchar(500)','头像URL'],['phone','varchar(20)','手机号'],
  ['open_id','varchar(100)','微信OpenID'],['union_id','varchar(100)','微信UnionID'],
  ['email','varchar(100)','邮箱'],['position','varchar(100)','职位'],
  ['last_login_time','datetime','最后登录时间'],['last_login_ip','varchar(50)','最后登录IP'],
  ['login_count','int(11)','累计登录次数'],['pwd_update_time','datetime','密码修改时间'],
  ['account_status','tinyint(2)','账号状态'],['is_admin','tinyint(1)','是否管理员账号'],
]);
T['supplier_role'] = f([
  ['supplier_code','varchar(50)','所属供应商编码'],['role_code','varchar(50)','角色编码'],
  ['role_name','varchar(100)','角色名称'],['role_type','tinyint(2)','角色类型'],
  ['description','varchar(500)','角色描述'],['status','tinyint(1)','状态'],
  ['sort_order','int(11)','排序号'],
]);
T['supplier_permission'] = f([
  ['permission_code','varchar(100)','权限编码'],['permission_name','varchar(100)','权限名称'],
  ['parent_code','varchar(50)','父权限编码'],['permission_type','tinyint(2)','权限类型'],
  ['path','varchar(200)','路由/接口路径'],['method','varchar(20)','请求方法'],
  ['icon','varchar(100)','图标'],['sort_order','int(11)','排序号'],
  ['status','tinyint(1)','状态'],['remark','varchar(500)','备注'],
]);
T['supplier_role_permission_ship'] = f([
  ['role_code','varchar(64)','角色编码'],['permission_code','varchar(64)','权限编码'],
]);
T['supplier_contract'] = f([
  ['contract_code','varchar(50)','合同编号'],['contract_name','varchar(200)','合同名称'],
  ['supplier_code','varchar(50)','供应商编码'],['organ_code','varchar(50)','签约组织编码'],
  ['contract_type','tinyint(2)','合同类型'],['sign_date','date','签约日期'],
  ['effective_date','date','生效日期'],['expire_date','date','到期日期'],
  ['contract_amount','decimal(12,2)','合同金额'],['commission_rate','decimal(5,4)','佣金比例'],
  ['settlement_cycle','tinyint(2)','结算周期'],['terms','text','合同条款'],
  ['attachment_urls','text','合同附件URL'],['sign_person','varchar(50)','签约人'],
  ['sign_seal_image','varchar(500)','签约盖章图片URL'],['is_auto_renew','tinyint(1)','是否自动续约'],
  ['renew_count','int(11)','续约次数'],['parent_contract_code','varchar(64)','原合同编码'],
  ['status','tinyint(2)','状态'],['audit_remark','varchar(500)','审核备注'],
  ['remark','varchar(500)','备注'],
]);
T['supplier_evaluation'] = f([
  ['supplier_code','varchar(50)','供应商编码'],['eval_period','varchar(20)','评价周期'],
  ['eval_type','tinyint(2)','评价类型'],['service_quality_score','decimal(5,2)','服务质量评分'],
  ['facility_quality_score','decimal(5,2)','设施质量评分'],['cooperation_score','decimal(5,2)','配合度评分'],
  ['complaint_rate','decimal(5,4)','投诉率'],['total_order_count','int(11)','期间订单总量'],
  ['complaint_count','int(11)','期间投诉量'],['total_score','decimal(5,2)','综合评分'],
  ['score_level','tinyint(2)','评分等级'],['eval_content','text','评价内容'],
  ['improvement_suggestions','text','改进建议'],['evaluator_code','varchar(64)','评价人编码'],
  ['evaluator_name','varchar(50)','评价人姓名'],['eval_date','date','评价日期'],
  ['status','tinyint(1)','状态'],['remark','varchar(500)','备注'],
]);
T['supplier_account_role_rel'] = f([
  ['account_code','varchar(50)','账号编码'],['role_code','varchar(50)','角色编码'],
  ['supplier_code','varchar(50)','供应商编码'],
]);
T['supplier_contact'] = f([
  ['supplier_code','varchar(50)','供应商编码'],['contact_name','varchar(50)','联系人姓名'],
  ['contact_type','tinyint(2)','联系人类型'],['position','varchar(100)','职位'],
  ['phone','varchar(20)','手机号'],['email','varchar(100)','邮箱'],
  ['wechat','varchar(50)','微信号'],['is_primary','tinyint(1)','是否主联系人'],
  ['remark','varchar(500)','备注'],
]);

// ----- park_ (15) -----
T['park_info'] = f([
  ['park_code','varchar(64)','机构唯一编码'],['full_name','varchar(128)','机构名称'],
  ['short_name','varchar(32)','机构简称'],['supplier_code','varchar(50)','所属供应商编码'],
  ['brand','varchar(64)','品牌名称'],['brand_introduction','text','品牌简介'],
  ['brand_logo','varchar(500)','品牌Logo'],['operation_subject','varchar(128)','运营主体'],
  ['operation_subject_description','text','运营主体介绍'],['important_shareholders','text','重要股东'],
  ['partner_company','varchar(255)','合作公司主体'],['business_license_no','varchar(64)','营业执照号'],
  ['business_bd','varchar(32)','商务BD'],['ability_type','tinyint(4)','机构类型'],
  ['ability_type_description','varchar(32)','机构类型描述'],['nature_type','tinyint(4)','机构性质'],
  ['nature_type_description','varchar(32)','机构性质描述'],['specialty_tag','varchar(256)','特色标签'],
  ['dayan_level','tinyint(2)','评级'],['province','varchar(16)','省'],
  ['province_code','varchar(20)','省编码'],['city','varchar(16)','城市'],
  ['city_code','varchar(20)','城市编码'],['district','varchar(16)','区'],
  ['district_code','varchar(20)','区编码'],['address','varchar(256)','具体地址'],
  ['longitude','varchar(64)','经度'],['latitude','varchar(20)','纬度'],
  ['service_hotline','varchar(32)','客服电话'],['base_description','text','机构介绍'],
  ['specialty_description','varchar(512)','机构特色'],['total_area','varchar(32)','占地面积'],
  ['building_area','varchar(32)','建筑面积'],['green_area_rate','varchar(32)','绿化率'],
  ['total_beds','int(11)','总床位数'],['available_beds','int(11)','可用床位数'],
  ['occupancy_rate','varchar(32)','已入住率'],['staff_count','int(11)','员工总数'],
  ['nurse_count','int(11)','护理人员数'],['nurse_patient_ratio','varchar(20)','护患比'],
  ['min_price_display','int(11)','最低月费'],['max_price_display','int(11)','最高月费'],
  ['price_unit','varchar(32)','价格单位'],['check_in_age_min','int(11)','入住最低年龄'],
  ['check_in_age_max','int(11)','入住最高年龄'],['check_in_description','varchar(512)','入住说明'],
  ['deposit_amount','decimal(12,2)','押金金额'],['deposit_description','varchar(500)','押金说明'],
  ['contract_period','tinyint(2)','合同期限'],['score_total','int(11)','总评分'],
  ['score_environment','int(11)','环境评分'],['score_recreation','int(11)','文娱评分'],
  ['score_nursing','int(11)','医养护理评分'],['score_food','int(11)','餐食精细评分'],
  ['score_service','int(11)','服务品质评分'],['score_price','int(11)','价格评分'],
  ['score_description','varchar(255)','评分描述'],['sort_order','int(11)','排序号'],
  ['is_hot','tinyint(2)','平台内评级'],['sub_script','varchar(11)','首页角标'],
  ['operate_status','tinyint(1)','运营状态'],['opening_time','datetime','开业时间'],
  ['online_time','datetime','上架时间'],['offline_time','datetime','下架时间'],
  ['add_platform_time','datetime','加入平台时间'],['is_published','tinyint(1)','是否已发布'],
  ['view_count','int(11)','浏览次数'],['collect_count','int(11)','收藏次数'],
  ['remark','varchar(500)','备注'],['version','bigint(20)','数据版本'],
]);
T['park_media_image'] = f([
  ['park_code','varchar(64)','机构编码'],['image_url','varchar(500)','图片URL'],
  ['image_name','varchar(200)','图片名称'],['image_type','tinyint(2)','图片类型'],
  ['image_description','varchar(500)','图片描述'],['width','int(11)','图片宽度'],
  ['height','int(11)','图片高度'],['file_size','int(11)','文件大小(KB)'],
  ['sort_order','int(11)','排序号'],['is_cover','tinyint(1)','是否封面'],
  ['status','tinyint(1)','状态'],
]);
T['park_media_video'] = f([
  ['park_code','varchar(64)','机构编码'],['video_url','varchar(500)','视频URL'],
  ['cover_url','varchar(500)','封面图URL'],['video_name','varchar(200)','视频名称'],
  ['video_type','tinyint(2)','视频类型'],['video_description','varchar(500)','视频描述'],
  ['duration','int(11)','时长(秒)'],['file_size','int(11)','文件大小(KB)'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
]);
T['park_media_file'] = f([
  ['park_code','varchar(64)','机构编码'],['file_url','varchar(500)','文件URL'],
  ['file_name','varchar(200)','文件名称'],['file_type','tinyint(2)','文件类型'],
  ['file_format','varchar(20)','文件格式'],['file_size','int(11)','文件大小(KB)'],
  ['file_description','varchar(500)','文件描述'],['sort_order','int(11)','排序号'],
  ['status','tinyint(1)','状态'],
]);
T['park_media_vr'] = f([
  ['park_code','varchar(64)','机构编码'],['vr_url','varchar(500)','VR全景链接'],
  ['vr_provider','varchar(100)','VR服务提供商'],['vr_name','varchar(200)','VR资源名称'],
  ['vr_type','tinyint(2)','VR类型'],['thumbnail_url','varchar(500)','缩略图URL'],
  ['vr_description','varchar(500)','VR描述'],['sort_order','int(11)','排序号'],
  ['status','tinyint(1)','状态'],
]);
T['park_facility'] = f([
  ['park_code','varchar(64)','机构编码'],['facility_code','varchar(50)','设施编码'],
  ['facility_name','varchar(100)','设施名称'],['facility_category','tinyint(2)','设施类别'],
  ['building_name','varchar(100)','所在楼栋'],['floor','varchar(20)','所在楼层'],
  ['area','decimal(8,2)','面积'],['capacity','int(11)','最大容纳人数'],
  ['open_time','varchar(100)','开放时间'],['facility_description','text','设施详细描述'],
  ['cover_image','varchar(500)','封面图URL'],['images','text','设施图片URL列表'],
  ['is_free','tinyint(1)','是否免费使用'],['fee_description','varchar(500)','收费说明'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
]);
T['park_service_item'] = f([
  ['park_code','varchar(64)','机构编码'],['service_code','varchar(50)','服务编码'],
  ['service_name','varchar(100)','服务名称'],['service_category','tinyint(2)','服务类别'],
  ['service_description','text','服务详细描述'],['is_included','tinyint(1)','是否包含在基础费用中'],
  ['fee_standard','varchar(200)','收费标准说明'],['service_frequency','varchar(100)','服务频次'],
  ['service_duration','varchar(50)','服务时长'],['cover_image','varchar(500)','服务图片URL'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
]);
T['park_adviser'] = f([
  ['park_code','varchar(64)','机构编码'],['adviser_name','varchar(100)','顾问姓名'],
  ['adviser_title','varchar(100)','顾问头衔'],['adviser_image','varchar(500)','顾问照片URL'],
  ['adviser_content','text','顾问介绍'],['contact_phone','varchar(32)','顾问联系电话'],
  ['is_primary','tinyint(1)','是否首席顾问'],['sort_order','int(11)','排序号'],
  ['status','tinyint(1)','状态'],
]);
T['park_periphery'] = f([
  ['park_code','varchar(64)','机构编码'],['periphery_type','tinyint(2)','周边类型'],
  ['place_name','varchar(200)','地点名称'],['place_address','varchar(500)','详细地址'],
  ['distance','varchar(50)','距离描述'],['detail_description','text','详细描述'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
]);
T['park_room_type'] = f([
  ['park_code','varchar(64)','机构编码'],['room_type_code','varchar(50)','房间类型编码'],
  ['room_type_name','varchar(100)','房间类型名称'],['stay_type','tinyint(2)','居住类型'],
  ['building_name','varchar(100)','楼栋名称'],['floor','varchar(20)','所在楼层'],
  ['room_category','tinyint(2)','房间类别'],['area','decimal(8,2)','房间面积'],
  ['orientation','varchar(20)','朝向'],['bed_count','int(11)','床位数'],
  ['total_rooms','int(11)','该类型房间总数'],['available_rooms','int(11)','可入住数'],
  ['has_bathroom','tinyint(1)','独立卫生间'],['has_kitchen','tinyint(1)','独立厨房'],
  ['has_balcony','tinyint(1)','有阳台'],['has_tv','tinyint(1)','有电视'],
  ['has_aircon','tinyint(1)','有空调'],['has_fridge','tinyint(1)','有冰箱'],
  ['has_washer','tinyint(1)','有洗衣机'],['has_wifi','tinyint(1)','有WiFi'],
  ['has_emergency','tinyint(1)','有紧急呼叫'],['has_monitor','tinyint(1)','有监控'],
  ['facilities','text','配套设施详情'],['description','text','房间详细说明'],
  ['cover_image','varchar(500)','封面图URL'],['images','text','房间图片URL列表'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
  ['design_description','text','户型设计描述'],['design_image','varchar(500)','户型图URL'],
  ['additional_images','text','其他户型图片'],
]);
T['park_room_price'] = f([
  ['park_code','varchar(64)','机构编码'],['room_type_code','varchar(64)','房间类型编码'],
  ['price_type','tinyint(2)','价格类型'],['original_price','decimal(12,2)','原价'],
  ['sale_price','decimal(12,2)','售价'],['discount_rate','decimal(5,2)','折扣率'],
  ['price_description','varchar(200)','价格说明'],['includes_items','text','包含项目'],
  ['effective_date','date','生效日期'],['expire_date','date','失效日期'],
  ['is_current','tinyint(1)','是否当前生效价格'],['is_promotion','tinyint(1)','是否促销价'],
  ['promotion_description','varchar(200)','促销说明'],['price_change_reason','varchar(500)','价格变更原因'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
]);
T['park_care_type'] = f([
  ['park_code','varchar(64)','机构编码'],['care_type_code','varchar(50)','照护类型编码'],
  ['care_type_name','varchar(100)','照护类型名称'],['care_level','tinyint(2)','照护等级'],
  ['care_target','varchar(500)','适用人群描述'],['care_items','text','护理项目明细'],
  ['care_frequency','varchar(100)','护理频次'],['nurse_patient_ratio','varchar(20)','护患比'],
  ['assessment_criteria','text','评估标准说明'],['description','text','照护类型详细描述'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
]);
T['park_care_price'] = f([
  ['park_code','varchar(64)','机构编码'],['care_type_code','varchar(64)','照护类型编码'],
  ['price_type','tinyint(2)','价格类型'],['original_price','decimal(12,2)','原价'],
  ['sale_price','decimal(12,2)','售价'],['discount_rate','decimal(5,2)','折扣率'],
  ['price_description','varchar(200)','价格说明'],['includes_items','text','包含项目'],
  ['effective_date','date','生效日期'],['expire_date','date','失效日期'],
  ['is_current','tinyint(1)','是否当前生效价格'],['is_promotion','tinyint(1)','是否促销价'],
  ['promotion_description','varchar(200)','促销说明'],['sort_order','int(11)','排序号'],
  ['status','tinyint(1)','状态'],
]);
T['park_food_type'] = f([
  ['park_code','varchar(64)','机构编码'],['food_type_code','varchar(50)','餐饮类型编码'],
  ['food_type_name','varchar(100)','餐饮类型名称'],['meal_plan','tinyint(2)','餐饮方案'],
  ['diet_features','varchar(500)','饮食特色'],['sample_menu','text','示例菜单'],
  ['special_diet','tinyint(1)','是否支持特殊饮食'],['special_diet_description','varchar(500)','特殊饮食说明'],
  ['description','text','详细描述'],['cover_image','varchar(500)','封面图URL'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
]);
T['park_food_price'] = f([
  ['park_code','varchar(64)','机构编码'],['food_type_code','varchar(64)','餐饮类型编码'],
  ['price_type','tinyint(2)','价格类型'],['original_price','decimal(12,2)','原价'],
  ['sale_price','decimal(12,2)','售价'],['discount_rate','decimal(5,2)','折扣率'],
  ['price_description','varchar(200)','价格说明'],['effective_date','date','生效日期'],
  ['expire_date','date','失效日期'],['is_current','tinyint(1)','是否当前生效价格'],
  ['is_promotion','tinyint(1)','是否促销价'],['promotion_description','varchar(200)','促销说明'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
]);

// ----- scene_ (5) -----
T['scene_info'] = f([
  ['scene_code','varchar(50)','场景编码'],['scene_name','varchar(200)','场景名称'],
  ['scene_type','tinyint(2)','场景类型'],['park_code','varchar(50)','关联养老机构编码'],
  ['province_code','varchar(20)','省份编码'],['city_code','varchar(20)','城市编码'],
  ['district_code','varchar(20)','区划编码'],['address','varchar(500)','活动地址'],
  ['scene_description','text','场景详细描述'],['cover_image','varchar(500)','封面图URL'],
  ['image_urls','text','场景图片URL列表'],['video_url','varchar(500)','宣传视频URL'],
  ['capacity','int(11)','最大容纳人数'],['duration_hours','decimal(4,1)','预计时长(小时)'],
  ['target_audience','varchar(500)','目标人群描述'],['highlight','text','场景亮点'],
  ['notice','text','注意事项'],['min_person','int(11)','最低成团人数'],
  ['max_person','int(11)','最大参与人数'],['original_price','decimal(12,2)','原价'],
  ['sale_price','decimal(12,2)','售价'],['price_unit','varchar(20)','价格单位'],
  ['is_free','tinyint(1)','是否免费'],['sort_order','int(11)','排序号'],
  ['view_count','int(11)','浏览次数'],['book_count','int(11)','预约次数'],
  ['scene_status','tinyint(2)','场景状态'],['audit_status','tinyint(2)','审核状态'],
  ['remark','varchar(500)','备注'],
]);
T['scene_item'] = f([
  ['scene_code','varchar(64)','场景编码'],['item_code','varchar(50)','项目编码'],
  ['item_name','varchar(100)','项目名称'],['item_type','tinyint(2)','项目类型'],
  ['item_description','varchar(500)','项目描述'],['duration_minutes','int(11)','预计时长(分钟)'],
  ['sort_order','int(11)','排序号'],['is_required','tinyint(1)','是否必选参与'],
  ['status','tinyint(1)','状态'],
]);
T['scene_item_price'] = f([
  ['scene_code','varchar(64)','场景编码'],['scene_item_code','varchar(64)','场景项目编码'],
  ['price_type','tinyint(2)','定价类型'],['original_price','decimal(12,2)','原价'],
  ['sale_price','decimal(12,2)','售价'],['channel_price','decimal(12,2)','渠道专属价'],
  ['price_description','varchar(200)','价格说明'],['effective_date','date','生效日期'],
  ['expire_date','date','失效日期'],['status','tinyint(1)','状态'],
]);
T['scene_schedule'] = f([
  ['scene_code','varchar(64)','场景编码'],['schedule_date','date','活动日期'],
  ['start_time','time','开始时间'],['end_time','time','结束时间'],
  ['max_person','int(11)','最大参与人数'],['current_person','int(11)','已报名人数'],
  ['price_override','decimal(12,2)','当日特殊价格'],['remark','varchar(500)','备注'],
  ['status','tinyint(2)','状态'],
]);
T['scene_resource'] = f([
  ['scene_code','varchar(64)','场景编码'],['resource_type','tinyint(2)','资源类型'],
  ['resource_name','varchar(100)','资源名称'],['resource_description','varchar(500)','资源描述'],
  ['quantity','int(11)','数量'],['unit','varchar(20)','单位'],
  ['unit_cost','decimal(12,2)','单位成本'],['is_provided','tinyint(1)','是否由机构提供'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
]);

// ----- channel_ (11) -----
T['channel_info'] = f([
  ['channel_code','varchar(50)','渠道编码'],['full_name','varchar(200)','渠道名称'],
  ['short_name','varchar(50)','简称'],['channel_type','tinyint(2)','渠道类型'],
  ['parent_code','varchar(50)','上级渠道编码'],['ancestors','varchar(500)','祖级列表'],
  ['level','tinyint(2)','层级'],['unified_credit_code','varchar(50)','统一社会信用代码'],
  ['legal_person','varchar(50)','法定代表人'],['province_code','varchar(20)','省份编码'],
  ['city_code','varchar(20)','城市编码'],['district_code','varchar(20)','区划编码'],
  ['address','varchar(500)','详细地址'],['contact_person','varchar(50)','联系人'],
  ['contact_phone','varchar(20)','联系电话'],['contact_email','varchar(100)','联系邮箱'],
  ['logo_url','varchar(500)','Logo URL'],['description','text','渠道介绍'],
  ['agent_count','int(11)','旗下代理人数量'],['total_order_amount','decimal(14,2)','累计订单金额'],
  ['cooperation_start_date','date','合作开始日期'],['distributor_code','varchar(50)','分销商编码'],
  ['settlement_cycle','tinyint(2)','结算周期'],['feature_config','text','渠道功能开关配置'],
  ['sort_order','int(11)','排序号'],['status','tinyint(2)','状态'],
  ['audit_status','tinyint(2)','审核状态'],['remark','varchar(500)','备注'],
]);
T['channel_open_platform'] = f([
  ['channel_code','varchar(50)','渠道编码'],['platform_name','varchar(100)','平台名称'],
  ['dock_type','tinyint(2)','对接类型'],['api_base_url','varchar(500)','API基础地址'],
  ['app_key','varchar(100)','应用Key'],['app_secret','varchar(200)','应用密钥'],
  ['callback_url','varchar(500)','回调地址'],['h5_domain','varchar(200)','H5域名配置'],
  ['h5_theme','varchar(50)','H5主题配置'],['auth_type','tinyint(2)','认证方式'],
  ['ip_whitelist','text','IP白名单'],['rate_limit','int(11)','调用频率限制'],
  ['timeout','int(11)','超时时间(秒)'],['extra_config','text','扩展配置'],
  ['status','tinyint(1)','状态'],
]);
T['channel_account'] = f([
  ['channel_code','varchar(50)','渠道编码'],['account_code','varchar(50)','账号编码'],
  ['username','varchar(50)','登录用户名'],['password','varchar(200)','密码'],
  ['salt','varchar(50)','密码盐值'],['real_name','varchar(50)','真实姓名'],
  ['avatar','varchar(500)','头像URL'],['phone','varchar(20)','手机号'],
  ['open_id','varchar(100)','微信OpenID'],['union_id','varchar(100)','微信UnionID'],
  ['email','varchar(100)','邮箱'],['position','varchar(100)','职位'],
  ['last_login_time','datetime','最后登录时间'],['last_login_ip','varchar(50)','最后登录IP'],
  ['login_count','int(11)','累计登录次数'],['account_status','tinyint(2)','账号状态'],
  ['is_admin','tinyint(1)','是否管理员'],
]);
T['channel_role'] = f([
  ['channel_code','varchar(50)','所属渠道编码'],['role_code','varchar(50)','角色编码'],
  ['role_name','varchar(100)','角色名称'],['role_type','tinyint(2)','角色类型'],
  ['description','varchar(500)','角色描述'],['status','tinyint(1)','状态'],
  ['sort_order','int(11)','排序号'],
]);
T['channel_permission'] = f([
  ['permission_code','varchar(100)','权限编码'],['permission_name','varchar(100)','权限名称'],
  ['parent_code','varchar(50)','父权限编码'],['permission_type','tinyint(2)','权限类型'],
  ['path','varchar(200)','路由/接口路径'],['method','varchar(20)','请求方法'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
]);
T['channel_role_permission_ship'] = f([
  ['role_code','varchar(64)','角色编码'],['permission_code','varchar(64)','权限编码'],
]);
T['channel_config_content'] = f([
  ['channel_code','varchar(50)','渠道编码'],['content_code','varchar(64)','内容编码'],
  ['content_type','tinyint(2)','内容类型'],['app_type','varchar(10)','展示端类型'],
  ['position','varchar(50)','展示位置'],['sort_order','int(11)','排序号'],
  ['is_top','tinyint(1)','是否置顶'],['effective_time','datetime','生效时间'],
  ['expire_time','datetime','失效时间'],['status','tinyint(1)','状态'],
]);
T['channel_config_scene'] = f([
  ['channel_code','varchar(50)','渠道编码'],['scene_code','varchar(64)','场景编码'],
  ['is_exclusive','tinyint(1)','是否渠道专属'],['custom_name','varchar(200)','自定义场景名称'],
  ['custom_price','decimal(12,2)','自定义价格'],['sort_order','int(11)','排序号'],
  ['effective_time','datetime','生效时间'],['expire_time','datetime','失效时间'],
  ['status','tinyint(1)','状态'],
]);
T['channel_config_goods'] = f([
  ['channel_code','varchar(50)','渠道编码'],['goods_code','varchar(64)','商品编码'],
  ['goods_type','tinyint(2)','商品类型'],['custom_name','varchar(200)','自定义商品名称'],
  ['custom_price','decimal(12,2)','自定义价格'],['custom_description','text','自定义描述'],
  ['is_exclusive','tinyint(1)','是否渠道专属'],['purchase_limit','int(11)','采购限制数量'],
  ['sort_order','int(11)','排序号'],['effective_time','datetime','生效时间'],
  ['expire_time','datetime','失效时间'],['status','tinyint(1)','状态'],
]);
T['channel_account_role_rel'] = f([
  ['account_code','varchar(50)','账号编码'],['role_code','varchar(50)','角色编码'],
  ['channel_code','varchar(50)','渠道编码'],
]);
T['channel_data_sync_log'] = f([
  ['sync_code','varchar(64)','同步记录编码'],['channel_code','varchar(50)','渠道编码'],
  ['sync_type','tinyint(2)','同步类型'],['biz_code','varchar(64)','业务编码'],
  ['direction','tinyint(1)','方向'],['request_data','text','请求报文'],
  ['response_data','text','响应报文'],['http_status','int(11)','HTTP状态码'],
  ['result','tinyint(2)','结果'],['error_msg','varchar(500)','错误信息'],
  ['retry_count','int(11)','重试次数'],['sync_time','datetime','同步时间'],
]);

// ----- agent_ (6) -----
T['agent_info'] = f([
  ['agent_code','varchar(50)','代理人编码'],['full_name','varchar(50)','代理人姓名'],
  ['gender','tinyint(1)','性别'],['avatar','varchar(500)','头像URL'],
  ['phone','varchar(20)','手机号'],['email','varchar(100)','邮箱'],
  ['id_card','varchar(20)','身份证号'],['channel_code','varchar(50)','所属渠道编码'],
  ['company_name','varchar(200)','保险公司名称'],['branch_name','varchar(200)','分支机构'],
  ['department','varchar(100)','部门'],['position','varchar(100)','职位'],
  ['employee_no','varchar(50)','保险公司工号'],['license_no','varchar(50)','从业资格证号'],
  ['province_code','varchar(20)','省份编码'],['city_code','varchar(20)','城市编码'],
  ['district_code','varchar(20)','区划编码'],['address','varchar(500)','详细地址'],
  ['service_intro','text','服务介绍'],['client_count','int(11)','服务客户数'],
  ['total_order_count','int(11)','累计订单数'],['total_order_amount','decimal(14,2)','累计订单金额'],
  ['agent_level','tinyint(2)','等级'],['is_certified','tinyint(1)','是否认证'],
  ['status','tinyint(2)','状态'],['remark','varchar(500)','备注'],
]);
T['agent_account'] = f([
  ['agent_code','varchar(50)','代理人编码'],['channel_code','varchar(50)','所属渠道编码'],
  ['username','varchar(50)','登录用户名'],['phone','varchar(20)','登录手机号'],
  ['password','varchar(200)','密码'],['salt','varchar(50)','密码盐值'],
  ['open_id','varchar(100)','微信OpenID'],['union_id','varchar(100)','微信UnionID'],
  ['ext_account_no','varchar(100)','渠道账号系统唯一编码'],['account_status','tinyint(2)','账号状态'],
  ['last_login_time','datetime','最后登录时间'],['last_login_ip','varchar(50)','最后登录IP'],
]);
T['agent_favorite'] = f([
  ['agent_code','varchar(50)','代理人编码'],['target_type','tinyint(2)','收藏对象类型'],
  ['target_code','varchar(50)','收藏对象编码'],
]);
T['agent_client_rel'] = f([
  ['agent_code','varchar(50)','代理人编码'],['client_code','varchar(50)','客户编码'],
  ['bind_type','tinyint(2)','绑定类型'],['bind_time','datetime','绑定时间'],
  ['status','tinyint(1)','状态'],
]);
T['agent_performance'] = f([
  ['agent_code','varchar(50)','代理人编码'],['channel_code','varchar(50)','所属渠道编码'],
  ['period_type','tinyint(2)','统计周期'],['period_value','varchar(20)','周期值'],
  ['equity_grant_count','int(11)','权益赠送次数'],['equity_grant_amount','decimal(14,2)','权益赠送金额'],
  ['scene_order_count','int(11)','场景订单数'],['scene_order_amount','decimal(14,2)','场景订单金额'],
  ['course_order_count','int(11)','课程订单数'],['course_order_amount','decimal(14,2)','课程订单金额'],
]);
T['agent_share_record'] = f([
  ['share_code','varchar(64)','分享编码'],['agent_code','varchar(50)','代理人编码'],
  ['share_type','tinyint(2)','分享类型'],['biz_code','varchar(64)','分享对象编码'],
  ['share_channel','tinyint(2)','分享渠道'],['client_code','varchar(50)','接收客户编码'],
  ['view_count','int(11)','浏览次数'],['share_time','datetime','分享时间'],
]);

// ----- client_ (7) -----
T['client_info'] = f([
  ['client_code','varchar(50)','客户编码'],['channel_code','varchar(50)','所属渠道编码'],
  ['full_name','varchar(50)','客户姓名'],['gender','tinyint(1)','性别'],
  ['avatar','varchar(500)','头像URL'],['birthday','date','出生日期'],
  ['age','tinyint(3)','年龄'],['id_card','varchar(20)','身份证号'],
  ['phone','varchar(20)','手机号'],['email','varchar(100)','邮箱'],
  ['province_code','varchar(20)','省份编码'],['city_code','varchar(20)','城市编码'],
  ['district_code','varchar(20)','区划编码'],['address','varchar(500)','详细地址'],
  ['nationality','varchar(50)','国籍'],['ethnic','varchar(50)','民族'],
  ['education','tinyint(2)','学历'],['marital_status','tinyint(2)','婚姻状况'],
  ['profession','varchar(100)','职业'],['source_type','tinyint(2)','来源渠道'],
  ['source_agent_code','varchar(64)','来源代理人编码'],['source_channel_code','varchar(50)','来源渠道编码'],
  ['client_level','tinyint(2)','客户等级'],['equity_count','int(11)','持有权益数'],
  ['used_equity_count','int(11)','已使用权益数'],['service_count','int(11)','累计服务次数'],
  ['total_order_amount','decimal(14,2)','累计消费金额'],['last_service_time','datetime','最近服务时间'],
  ['register_time','datetime','注册时间'],['last_login_time','datetime','最后登录时间'],
  ['is_vip','tinyint(1)','是否VIP'],['status','tinyint(2)','状态'],
  ['remark','varchar(500)','备注'],
]);
T['client_account'] = f([
  ['client_code','varchar(50)','客户编码'],['channel_code','varchar(50)','所属渠道编码'],
  ['username','varchar(50)','登录用户名'],['phone','varchar(20)','登录手机号'],
  ['password','varchar(200)','密码'],['salt','varchar(50)','密码盐值'],
  ['open_id','varchar(100)','微信OpenID'],['union_id','varchar(100)','微信UnionID'],
  ['alipay_id','varchar(100)','支付宝账号ID'],['ext_account_no','varchar(100)','渠道账号系统唯一编码'],
  ['last_login_time','datetime','最后登录时间'],['last_login_ip','varchar(50)','最后登录IP'],
  ['login_count','int(11)','累计登录次数'],['account_status','tinyint(2)','账号状态'],
]);
T['client_favorite'] = f([
  ['client_code','varchar(50)','客户编码'],['target_type','tinyint(2)','收藏对象类型'],
  ['target_code','varchar(50)','收藏对象编码'],['target_name','varchar(200)','收藏对象名称'],
  ['remark','varchar(500)','备注'],
]);
T['client_health_profile'] = f([
  ['client_code','varchar(50)','客户编码'],['height','decimal(5,1)','身高(cm)'],
  ['weight','decimal(5,1)','体重(kg)'],['blood_type','tinyint(2)','血型'],
  ['blood_pressure','varchar(50)','血压'],['blood_sugar','decimal(5,2)','血糖(mmol/L)'],
  ['heart_rate','int(11)','心率(次/分)'],['chronic_diseases','text','慢性病列表'],
  ['allergy_history','text','过敏史'],['surgery_history','text','手术史'],
  ['family_history','text','家族病史'],['medication_info','text','当前用药信息'],
  ['mobility_level','tinyint(2)','行动能力'],['cognitive_level','tinyint(2)','认知能力'],
  ['mental_status','tinyint(2)','心理状态'],['diet_preference','text','饮食偏好'],
  ['sleep_quality','tinyint(2)','睡眠质量'],['emergency_contact_name','varchar(50)','紧急联系人姓名'],
  ['emergency_contact_phone','varchar(20)','紧急联系人电话'],['emergency_contact_relation','varchar(20)','紧急联系人关系'],
  ['health_score','decimal(5,2)','健康评分'],['last_assessment_time','datetime','最近评估时间'],
  ['remark','text','备注'],
]);
T['client_care_need'] = f([
  ['client_code','varchar(50)','客户编码'],['butler_code','varchar(64)','评估管家编码'],
  ['butler_full_name','varchar(50)','评估管家姓名(快照)'],['eval_date','date','评估日期'],
  ['care_level','tinyint(2)','建议照护等级'],['care_type_preference','varchar(200)','偏好照护类型'],
  ['living_preference','varchar(200)','居住偏好'],['food_preference','varchar(200)','饮食偏好'],
  ['budget_min','decimal(12,2)','预算下限(元/月)'],['budget_max','decimal(12,2)','预算上限(元/月)'],
  ['area_preference','varchar(200)','区域偏好'],['special_requirements','text','特殊需求说明'],
  ['expected_checkin_date','date','期望入住日期'],['park_recommendations','text','推荐机构列表'],
  ['eval_result','text','评估结论'],['status','tinyint(2)','状态'],
  ['remark','varchar(500)','备注'],
]);
T['client_family_member'] = f([
  ['client_code','varchar(50)','客户编码'],['member_name','varchar(50)','成员姓名'],
  ['relation','varchar(20)','与客户关系'],['gender','tinyint(1)','性别'],
  ['phone','varchar(20)','联系电话'],['email','varchar(100)','邮箱'],
  ['is_emergency_contact','tinyint(1)','是否紧急联系人'],['is_primary_contact','tinyint(1)','是否主要联系人'],
  ['is_decision_maker','tinyint(1)','是否决策人'],['address','varchar(500)','地址'],
  ['remark','varchar(500)','备注'],['status','tinyint(1)','状态'],
  ['sort_order','int(11)','排序号'],
]);
T['client_address'] = f([
  ['client_code','varchar(50)','客户编码'],['receiver_name','varchar(50)','收货人姓名'],
  ['receiver_phone','varchar(20)','收货人电话'],['province_code','varchar(20)','省编码'],
  ['city_code','varchar(20)','城市编码'],['district_code','varchar(20)','区编码'],
  ['detail_address','varchar(256)','详细地址'],['full_address','varchar(500)','完整地址'],
  ['is_default','tinyint(1)','是否默认地址'],['tag','varchar(32)','地址标签'],
]);

// ----- equity_ (6) -----
T['equity_template'] = f([
  ['template_code','varchar(50)','模板编码'],['template_name','varchar(200)','模板名称'],
  ['equity_type','tinyint(2)','权益类型'],['equity_level','tinyint(2)','权益等级'],
  ['equity_value','decimal(12,2)','权益面值'],['cost_price','decimal(12,2)','成本价'],
  ['content_description','text','权益内容描述'],['service_items','text','包含服务项目'],
  ['applicable_parks','text','适用机构范围'],['applicable_cities','text','适用城市范围'],
  ['valid_days','int(11)','激活后有效天数'],['shelf_life_days','int(11)','库存有效期天数'],
  ['is_transferable','tinyint(1)','是否可转让'],['is_stackable','tinyint(1)','是否可叠加使用'],
  ['max_use_count','int(11)','最大使用次数'],['cover_image','varchar(500)','权益封面图'],
  ['card_design_url','varchar(500)','卡面设计图URL'],['terms','text','使用说明/条款'],
  ['sort_order','int(11)','排序号'],['status','tinyint(2)','状态'],
  ['remark','varchar(500)','备注'],
]);
T['equity_batch'] = f([
  ['batch_code','varchar(50)','批次编码'],['batch_name','varchar(200)','批次名称'],
  ['template_code','varchar(50)','权益模板编码'],['channel_code','varchar(50)','分配渠道编码'],
  ['total_quantity','int(11)','总数量'],['produced_count','int(11)','已生成数量'],
  ['allocated_count','int(11)','已分配数量'],['outbound_count','int(11)','已出库数量'],
  ['activated_count','int(11)','已激活数量'],['used_count','int(11)','已使用数量'],
  ['expired_count','int(11)','已过期数量'],['voided_count','int(11)','已作废数量'],
  ['remain_count','int(11)','剩余可用数量'],['unit_cost','decimal(12,2)','单位成本'],
  ['total_cost','decimal(14,2)','批次总成本'],['produce_date','date','生产日期'],
  ['expire_date','date','批次有效期'],['batch_status','tinyint(2)','批次状态'],
  ['remark','varchar(500)','备注'],
]);
T['equity_depot'] = f([
  ['equity_code','varchar(50)','权益编码'],['equity_no','varchar(50)','权益卡号'],
  ['template_code','varchar(50)','权益模板编码'],['batch_code','varchar(50)','批次编码'],
  ['equity_type','tinyint(2)','权益类型'],['equity_value','decimal(12,2)','权益面值'],
  ['cost_price','decimal(12,2)','成本价'],['channel_code','varchar(50)','分配渠道编码'],
  ['agent_code','varchar(50)','分配代理人编码'],['client_code','varchar(50)','领取客户编码'],
  ['produce_time','datetime','入库时间'],['allocate_time','datetime','分配时间'],
  ['outbound_channel_code','varchar(50)','出库寄送渠道编码'],['outbound_agent_code','varchar(50)','出库寄送代理人编码'],
  ['outbound_time','datetime','出库时间'],['logistics_no','varchar(100)','物流单号'],
  ['activate_time','datetime','激活时间'],['first_use_time','datetime','首次使用时间'],
  ['last_use_time','datetime','最近使用时间'],['use_count','int(11)','已使用次数'],
  ['max_use_count','int(11)','最大使用次数'],['expire_time','datetime','过期时间'],
  ['shelf_expire_time','datetime','库存过期时间'],['card_secret','varchar(200)','卡密'],
  ['carrier_type','tinyint(2)','载体类型'],['activate_code','varchar(20)','激活码'],
  ['bind_code','varchar(20)','绑定码'],['qr_code_url','varchar(500)','权益二维码URL'],
  ['order_code','varchar(64)','关联订单编码'],['equity_status','tinyint(2)','权益状态'],
  ['void_reason','varchar(500)','作废原因'],['remark','varchar(500)','备注'],
]);
T['equity_activate'] = f([
  ['activate_code','varchar(50)','激活记录编码'],['equity_code','varchar(50)','权益编码'],
  ['template_code','varchar(64)','权益模板编码'],['client_code','varchar(50)','激活客户编码'],
  ['client_full_name','varchar(50)','激活客户姓名(快照)'],['client_phone','varchar(20)','激活客户手机号'],
  ['activate_channel','tinyint(2)','激活渠道'],['activate_source_code','varchar(64)','激活来源编码'],
  ['activate_time','datetime','激活时间'],['expire_time','datetime','过期时间'],
  ['is_id_card_verified','tinyint(1)','是否实名认证'],['is_agreement_signed','tinyint(1)','是否签署协议'],
  ['ip_address','varchar(50)','激活IP'],['device_info','varchar(500)','设备信息'],
  ['remark','varchar(500)','备注'],
]);
T['equity_use_person'] = f([
  ['equity_code','varchar(50)','权益编码'],['client_code','varchar(50)','权益持有人编码'],
  ['use_person_name','varchar(50)','使用人姓名'],['use_person_gender','tinyint(1)','使用人性别'],
  ['use_person_birthday','date','使用人出生日期'],['use_person_age','tinyint(3)','使用人年龄'],
  ['use_person_phone','varchar(20)','使用人手机号'],['use_person_id_card','varchar(20)','使用人身份证号'],
  ['relation_with_holder','varchar(20)','与持有人关系'],['health_status','text','健康状况简述'],
  ['care_need','text','照护需求简述'],['is_default_holder','tinyint(1)','是否默认权益人'],
  ['remark','varchar(500)','备注'],
]);
T['equity_change_holder'] = f([
  ['equity_code','varchar(50)','权益编码'],['old_use_person_code','varchar(64)','原权益使用人编码'],
  ['old_person_name','varchar(50)','原权益人姓名'],['old_person_id_card','varchar(20)','原权益人身份证号'],
  ['new_use_person_code','varchar(64)','新权益使用人编码'],['new_person_name','varchar(50)','新权益人姓名'],
  ['new_person_id_card','varchar(20)','新权益人身份证号'],['change_reason','varchar(500)','更换原因'],
  ['change_status','tinyint(2)','更换状态'],['operate_time','datetime','操作时间'],
  ['operator_code','varchar(64)','操作人编码'],
]);

// ----- service_ (7) -----
T['service_session'] = f([
  ['session_code','varchar(50)','会话编码'],['equity_code','varchar(50)','关联权益编码'],
  ['client_code','varchar(50)','客户编码'],['butler_code','varchar(50)','服务管家编码'],
  ['butler_full_name','varchar(50)','服务管家姓名(快照)'],['service_type','tinyint(2)','服务类型'],
  ['service_title','varchar(200)','服务标题'],['service_description','text','服务描述'],
  ['priority','tinyint(2)','优先级'],['source_type','tinyint(2)','来源'],
  ['source_code','varchar(64)','来源编码'],['park_code','varchar(64)','关联养老机构编码'],
  ['park_full_name','varchar(200)','关联养老机构名称(快照)'],['agent_code','varchar(64)','关联代理人编码'],
  ['channel_code','varchar(50)','关联渠道编码'],['accept_time','datetime','受理时间'],
  ['complete_time','datetime','完成时间'],['close_time','datetime','关闭时间'],
  ['total_duration','int(11)','总服务时长(小时)'],['touch_count','int(11)','服务接触次数'],
  ['is_satisfied','tinyint(1)','是否满意'],['overall_rating','tinyint(1)','综合评分'],
  ['session_status','tinyint(2)','会话状态'],['sub_status','varchar(20)','子状态'],
  ['close_reason','varchar(500)','关闭原因'],['remark','varchar(500)','备注'],
]);
T['service_equity_demand'] = f([
  ['session_code','varchar(50)','服务会话编码'],['client_code','varchar(64)','客户编码'],
  ['butler_code','varchar(64)','管家编码'],['demand_type','tinyint(2)','需求类型'],
  ['use_person_name','varchar(50)','使用人姓名'],['use_person_age','tinyint(3)','使用人年龄'],
  ['use_person_gender','tinyint(1)','使用人性别'],['health_summary','text','健康状况概述'],
  ['care_level_need','tinyint(2)','所需照护等级'],['city_preference','varchar(200)','城市偏好'],
  ['area_preference','varchar(200)','区域偏好'],['budget_min','decimal(12,2)','预算下限'],
  ['budget_max','decimal(12,2)','预算上限'],['room_preference','varchar(200)','房间偏好'],
  ['food_preference','varchar(200)','饮食偏好'],['special_needs','text','特殊需求'],
  ['expected_time','date','期望服务时间'],['contact_preference','tinyint(2)','联系偏好'],
  ['collect_method','tinyint(2)','收集方式'],['collect_time','datetime','收集时间'],
  ['demand_summary','text','需求总结'],['demand_images','text','需求相关资料图片'],
  ['status','tinyint(2)','状态'],['remark','varchar(500)','备注'],
]);
T['service_equity_solution'] = f([
  ['session_code','varchar(50)','服务会话编码'],['demand_code','varchar(64)','关联需求编码'],
  ['client_code','varchar(64)','客户编码'],['butler_code','varchar(64)','管家编码'],
  ['solution_code','varchar(50)','方案编码'],['solution_name','varchar(200)','方案名称'],
  ['solution_type','tinyint(2)','方案类型'],['recommended_parks','text','推荐机构列表'],
  ['plan_summary','text','方案概述'],['service_items','text','服务项目明细'],
  ['estimated_cost','decimal(12,2)','预估费用'],['cost_breakdown','text','费用明细'],
  ['timeline','text','服务时间安排'],['advantages','text','方案优势'],
  ['risks','text','注意事项/风险提示'],['comparison','text','与备选方案对比说明'],
  ['presentation_time','datetime','方案呈现时间'],['presentation_method','tinyint(2)','呈现方式'],
  ['client_feedback','text','客户反馈'],['is_accepted','tinyint(1)','客户是否接受'],
  ['adjust_count','int(11)','调整次数'],['status','tinyint(2)','状态'],
  ['remark','varchar(500)','备注'],
]);
T['service_equity_arrange'] = f([
  ['session_code','varchar(50)','服务会话编码'],['solution_code','varchar(64)','关联方案编码'],
  ['client_code','varchar(64)','客户编码'],['butler_code','varchar(64)','管家编码'],
  ['arrange_code','varchar(50)','安排编码'],['arrange_type','tinyint(2)','安排类型'],
  ['park_code','varchar(64)','关联养老机构编码'],['park_full_name','varchar(200)','关联养老机构名称(快照)'],
  ['arrange_date','date','安排日期'],['arrange_time_start','time','开始时间'],
  ['arrange_time_end','time','结束时间'],['arrange_address','varchar(500)','安排地址'],
  ['contact_person','varchar(50)','对接联系人'],['contact_phone','varchar(20)','对接联系电话'],
  ['participant_count','int(11)','参与人数'],['prepare_items','text','准备事项'],
  ['progress_notes','text','进展备注'],['confirm_time','datetime','客户确认时间'],
  ['complete_time','datetime','完成时间'],['is_confirmed','tinyint(1)','是否已确认'],
  ['status','tinyint(2)','状态'],['cancel_reason','varchar(500)','取消原因'],
  ['remark','varchar(500)','备注'],
]);
T['service_equity_followup'] = f([
  ['session_code','varchar(50)','服务会话编码'],['arrange_code','varchar(64)','关联安排编码'],
  ['client_code','varchar(64)','客户编码'],['butler_code','varchar(64)','回访管家编码'],
  ['followup_code','varchar(50)','回访编码'],['followup_type','tinyint(2)','回访类型'],
  ['followup_method','tinyint(2)','回访方式'],['followup_date','date','回访日期'],
  ['followup_time','datetime','回访时间'],['service_satisfaction','tinyint(1)','服务满意度'],
  ['park_satisfaction','tinyint(1)','机构满意度'],['butler_satisfaction','tinyint(1)','管家满意度'],
  ['overall_satisfaction','tinyint(1)','综合满意度'],['service_evaluation','text','服务评价内容'],
  ['improvement_suggestions','text','改进建议'],['complaints','text','投诉内容'],
  ['complaint_handle','text','投诉处理情况'],['is_followup_needed','tinyint(1)','是否需要后续跟进'],
  ['followup_plan','text','后续跟进计划'],['next_followup_date','date','下次回访日期'],
  ['is_resolved','tinyint(1)','问题是否已解决'],['status','tinyint(2)','状态'],
  ['remark','varchar(500)','备注'],
]);
T['service_evaluation'] = f([
  ['session_code','varchar(64)','服务会话编码'],['client_code','varchar(64)','客户编码'],
  ['butler_code','varchar(64)','管家编码'],['park_code','varchar(64)','关联机构编码'],
  ['attitude_rating','tinyint(1)','服务态度评分'],['professional_rating','tinyint(1)','专业度评分'],
  ['responsiveness_rating','tinyint(1)','响应速度评分'],['satisfaction_rating','tinyint(1)','满意度评分'],
  ['content','text','评价内容'],['image_urls','text','评价图片'],
  ['is_anonymous','tinyint(1)','是否匿名'],['reply_content','text','回复内容'],
  ['reply_time','datetime','回复时间'],['reply_by_code','varchar(64)','回复人编码'],
  ['status','tinyint(1)','状态'],
]);
T['service_visit_record'] = f([
  ['butler_code','varchar(50)','管家编码'],['park_code','varchar(50)','机构编码'],
  ['visit_date','date','探访日期'],['visit_purpose','tinyint(2)','探访目的'],
  ['facility_check','text','设施检查情况'],['service_check','text','服务检查情况'],
  ['hygiene_check','text','卫生检查情况'],['food_check','text','餐饮检查情况'],
  ['safety_check','text','安全检查情况'],['overall_score','decimal(5,2)','综合评分'],
  ['issues_found','text','发现问题'],['improvement_suggestions','text','改进建议'],
  ['images','text','探访照片'],['status','tinyint(2)','状态'],
  ['remark','varchar(500)','备注'],
]);

// ----- goods_ (5) -----
T['goods_info'] = f([
  ['goods_code','varchar(50)','商品编码'],['goods_name','varchar(200)','商品名称'],
  ['goods_short_name','varchar(50)','商品简称'],['goods_type','tinyint(2)','商品类型'],
  ['category_code','varchar(50)','分类编码'],['brand_name','varchar(100)','品牌名称'],
  ['cover_image','varchar(500)','封面图URL'],['image_urls','text','商品图片'],
  ['video_url','varchar(500)','宣传视频URL'],['goods_description','text','商品详细描述'],
  ['summary','varchar(500)','商品摘要'],['original_price','decimal(12,2)','原价'],
  ['sale_price','decimal(12,2)','售价'],['cost_price','decimal(12,2)','成本价'],
  ['price_unit','varchar(20)','价格单位'],['stock','int(11)','库存'],
  ['sales_count','int(11)','已售数量'],['view_count','int(11)','浏览次数'],
  ['collect_count','int(11)','收藏次数'],['sale_start_time','datetime','开售时间'],
  ['sale_end_time','datetime','停售时间'],['is_hot','tinyint(1)','是否热销'],
  ['is_new','tinyint(1)','是否新品'],['is_recommend','tinyint(1)','是否推荐'],
  ['sort_order','int(11)','排序号'],['goods_status','tinyint(2)','商品状态'],
  ['audit_status','tinyint(2)','审核状态'],['remark','varchar(500)','备注'],
]);
T['goods_sku_equity'] = f([
  ['goods_code','varchar(50)','商品编码'],['sku_code','varchar(50)','SKU编码'],
  ['sku_name','varchar(200)','SKU名称'],['template_code','varchar(50)','权益模板编码'],
  ['equity_type','tinyint(2)','权益类型'],['equity_value','decimal(12,2)','权益面值'],
  ['sku_price','decimal(12,2)','SKU售价'],['stock','int(11)','库存'],
  ['sales_count','int(11)','已售数量'],['spec_description','varchar(500)','规格描述'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
]);
T['goods_sku_scene'] = f([
  ['goods_code','varchar(50)','商品编码'],['sku_code','varchar(50)','SKU编码'],
  ['sku_name','varchar(200)','SKU名称'],['scene_code','varchar(50)','场景编码'],
  ['park_code','varchar(64)','关联机构编码'],['sku_price','decimal(12,2)','SKU售价'],
  ['person_limit','int(11)','人数限制'],['duration_hours','decimal(4,1)','活动时长(小时)'],
  ['schedule_description','varchar(500)','排期说明'],['stock','int(11)','库存'],
  ['sales_count','int(11)','已售数量'],['sort_order','int(11)','排序号'],
  ['status','tinyint(1)','状态'],
]);
T['goods_sku_course'] = f([
  ['goods_code','varchar(50)','商品编码'],['sku_code','varchar(50)','SKU编码'],
  ['sku_name','varchar(200)','SKU名称'],['course_code','varchar(50)','课程编码'],
  ['course_type','tinyint(2)','课程类型'],['sku_price','decimal(12,2)','SKU售价'],
  ['class_count','int(11)','课时数'],['valid_days','int(11)','有效天数'],
  ['stock','int(11)','库存'],['sales_count','int(11)','已售数量'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
]);
T['goods_sku_sojourn'] = f([
  ['goods_code','varchar(50)','商品编码'],['sku_code','varchar(50)','SKU编码'],
  ['sku_name','varchar(200)','SKU名称'],['park_code','varchar(50)','机构编码'],
  ['room_type_code','varchar(64)','关联房间类型编码'],['room_type_name','varchar(100)','房间类型名称'],
  ['care_type_code','varchar(64)','关联照护类型编码'],['food_type_code','varchar(64)','关联餐饮类型编码'],
  ['sku_price','decimal(12,2)','SKU售价'],['price_unit','varchar(20)','价格单位'],
  ['min_days','int(11)','最少天数'],['max_days','int(11)','最多天数'],
  ['stock','int(11)','库存'],['sales_count','int(11)','已售数量'],
  ['effective_date','date','生效日期'],['expire_date','date','失效日期'],
  ['sort_order','int(11)','排序号'],['status','tinyint(1)','状态'],
]);

// ----- content_ (5) -----
T['content_info'] = f([
  ['content_code','varchar(50)','内容编码'],['title','varchar(200)','标题'],
  ['subtitle','varchar(200)','副标题'],['content_type','tinyint(2)','内容类型'],
  ['category_code','varchar(50)','分类编码'],['author_name','varchar(50)','作者姓名'],
  ['author_avatar','varchar(500)','作者头像'],['cover_image','varchar(500)','封面图URL'],
  ['summary','varchar(500)','摘要'],['content_body','longtext','正文内容'],
  ['source_type','tinyint(2)','来源类型'],['source_url','varchar(500)','来源链接'],
  ['tags','varchar(500)','标签'],['is_top','tinyint(1)','是否置顶'],
  ['is_recommend','tinyint(1)','是否推荐'],['is_comment','tinyint(1)','是否允许评论'],
  ['view_count','int(11)','浏览次数'],['like_count','int(11)','点赞次数'],
  ['comment_count','int(11)','评论次数'],['share_count','int(11)','分享次数'],
  ['collect_count','int(11)','收藏次数'],['publish_time','datetime','发布时间'],
  ['sort_order','int(11)','排序号'],['content_status','tinyint(2)','状态'],
  ['audit_status','tinyint(2)','审核状态'],['remark','varchar(500)','备注'],
]);
T['content_category'] = f([
  ['category_code','varchar(50)','分类编码'],['category_name','varchar(100)','分类名称'],
  ['parent_code','varchar(50)','父分类编码'],['category_type','tinyint(2)','分类类型'],
  ['icon','varchar(100)','图标'],['cover_image','varchar(500)','封面图'],
  ['description','varchar(500)','分类描述'],['content_count','int(11)','内容数量'],
  ['sort_order','int(11)','排序号'],['is_visible','tinyint(1)','是否可见'],
  ['status','tinyint(1)','状态'],
]);
T['content_media'] = f([
  ['content_code','varchar(64)','内容编码'],['media_type','tinyint(2)','媒体类型'],
  ['media_url','varchar(500)','资源URL'],['thumbnail_url','varchar(500)','缩略图URL'],
  ['media_name','varchar(200)','资源名称'],['file_format','varchar(20)','文件格式'],
  ['file_size','int(11)','文件大小(KB)'],['width','int(11)','宽度(像素)'],
  ['height','int(11)','高度(像素)'],['duration','int(11)','时长(秒)'],
  ['media_description','varchar(500)','资源描述'],['is_in_body','tinyint(1)','是否在正文中'],
  ['sort_order','int(11)','排序号'],
]);
T['content_record_share'] = f([
  ['content_code','varchar(50)','内容编码'],['sharer_type','varchar(30)','分享者类型'],
  ['sharer_code','varchar(50)','分享者编码'],['share_channel','tinyint(2)','分享渠道'],
  ['share_url','varchar(500)','分享链接'],['share_title','varchar(200)','分享标题'],
  ['share_description','varchar(500)','分享描述'],['share_image','varchar(500)','分享缩略图'],
  ['click_count','int(11)','点击次数'],['convert_count','int(11)','转化次数'],
  ['share_time','datetime','分享时间'],
]);
T['content_record_read'] = f([
  ['content_code','varchar(50)','内容编码'],['reader_type','varchar(30)','阅读者类型'],
  ['reader_code','varchar(50)','阅读者编码'],['read_duration','int(11)','阅读时长(秒)'],
  ['read_progress','decimal(5,2)','阅读进度(%)'],['read_source','tinyint(2)','阅读来源'],
  ['ip_address','varchar(50)','IP地址'],['device_type','varchar(20)','设备类型'],
  ['read_time','datetime','阅读时间'],
]);

// ----- course_ (3) -----
T['course_info'] = f([
  ['course_code','varchar(50)','课程编码'],['course_name','varchar(200)','课程名称'],
  ['course_type','tinyint(2)','课程类型'],['category_code','varchar(50)','分类编码'],
  ['cover_image','varchar(500)','封面图URL'],['video_url','varchar(500)','宣传视频URL'],
  ['course_description','text','课程描述'],['course_outline','text','课程大纲'],
  ['target_audience','varchar(500)','目标人群'],['learning_objectives','text','学习目标'],
  ['lecturer_code','varchar(64)','主讲讲师编码'],['total_class','int(11)','总课时数'],
  ['total_duration','int(11)','总时长(分钟)'],['valid_days','int(11)','有效天数'],
  ['original_price','decimal(12,2)','原价'],['sale_price','decimal(12,2)','售价'],
  ['max_students','int(11)','最大学员数'],['current_students','int(11)','当前学员数'],
  ['view_count','int(11)','浏览次数'],['sales_count','int(11)','已售数量'],
  ['rating_avg','decimal(3,2)','平均评分'],['is_free','tinyint(1)','是否免费'],
  ['is_recommend','tinyint(1)','是否推荐'],['course_start_date','date','开课日期'],
  ['course_end_date','date','结课日期'],['sort_order','int(11)','排序号'],
  ['course_status','tinyint(2)','状态'],['remark','varchar(500)','备注'],
]);
T['course_lecturer'] = f([
  ['lecturer_code','varchar(50)','讲师编码'],['lecturer_name','varchar(50)','讲师姓名'],
  ['gender','tinyint(1)','性别'],['avatar','varchar(500)','头像URL'],
  ['title','varchar(100)','职称/头衔'],['organization','varchar(200)','所属机构'],
  ['specialty','varchar(500)','擅长领域'],['introduction','text','讲师简介'],
  ['certifications','text','资质证书'],['phone','varchar(20)','联系电话'],
  ['email','varchar(100)','联系邮箱'],['course_count','int(11)','开课数量'],
  ['student_count','int(11)','学员总数'],['rating_avg','decimal(3,2)','平均评分'],
  ['is_certified','tinyint(1)','是否平台认证'],['sort_order','int(11)','排序号'],
  ['status','tinyint(2)','状态'],
]);
T['course_record_learn'] = f([
  ['course_code','varchar(50)','课程编码'],['client_code','varchar(64)','学员客户编码'],
  ['agent_code','varchar(64)','学员代理人编码'],['learner_name','varchar(50)','学员姓名'],
  ['learner_phone','varchar(20)','学员手机号'],['enroll_time','datetime','报名时间'],
  ['order_code','varchar(64)','关联订单编码'],['current_lesson','int(11)','当前学到第几课'],
  ['total_lesson','int(11)','总课时'],['learn_progress','decimal(5,2)','学习进度(%)'],
  ['total_learn_time','int(11)','累计学习时长(分钟)'],['last_learn_time','datetime','最近学习时间'],
  ['is_completed','tinyint(1)','是否完成'],['complete_time','datetime','完成时间'],
  ['certificate_url','varchar(500)','结业证书URL'],['rating','tinyint(1)','课程评分'],
  ['rating_content','varchar(500)','评价内容'],['status','tinyint(2)','状态'],
]);

// ----- order_ (4) -----
T['order_equity'] = f([
  ['order_code','varchar(50)','订单编号'],['order_source','tinyint(2)','采购来源'],
  ['channel_code','varchar(50)','渠道编码'],['channel_full_name','varchar(200)','渠道名称(快照)'],
  ['agent_code','varchar(50)','代理人编码'],['agent_full_name','varchar(50)','代理人姓名(快照)'],
  ['distributor_code','varchar(64)','分销商编码'],['distributor_full_name','varchar(200)','分销商名称(快照)'],
  ['goods_code','varchar(50)','商品编码'],['goods_name','varchar(200)','商品名称'],
  ['sku_code','varchar(50)','SKU编码'],['sku_name','varchar(200)','SKU名称'],
  ['quantity','int(11)','购买数量'],['unit_price','decimal(12,2)','单价'],
  ['total_amount','decimal(14,2)','订单总额'],['discount_amount','decimal(12,2)','优惠金额'],
  ['pay_amount','decimal(14,2)','实付金额'],['pay_type','tinyint(2)','支付方式'],
  ['pay_time','datetime','支付时间'],['pay_trade_no','varchar(100)','支付流水号'],
  ['deliver_type','tinyint(2)','权益入库方式'],['deliver_count','int(11)','已入库数量'],
  ['deliver_time','datetime','入库完成时间'],['expire_time','datetime','订单过期时间'],
  ['invoice_status','tinyint(1)','发票状态'],['organ_code','varchar(50)','平台运营方编码'],
  ['order_status','tinyint(2)','订单状态'],['cancel_reason','varchar(500)','取消原因'],
  ['remark','varchar(500)','备注'],
]);
T['order_scene'] = f([
  ['order_code','varchar(50)','订单编号'],['order_type','tinyint(2)','订单类型'],
  ['channel_code','varchar(50)','渠道编码'],['channel_full_name','varchar(200)','渠道名称(快照)'],
  ['agent_code','varchar(64)','代理人编码'],['agent_full_name','varchar(50)','代理人姓名(快照)'],
  ['distributor_code','varchar(64)','分销商编码'],['distributor_full_name','varchar(200)','分销商名称(快照)'],
  ['client_code','varchar(64)','客户编码'],['client_full_name','varchar(50)','客户姓名(快照)'],
  ['goods_code','varchar(50)','商品编码'],['scene_code','varchar(64)','场景编码'],
  ['scene_name','varchar(200)','场景名称'],['sku_code','varchar(64)','SKU编码'],
  ['schedule_code','varchar(64)','排期编码'],['activity_date','date','活动日期'],
  ['participant_count','int(11)','参与人数'],['participant_names','text','参与人姓名'],
  ['unit_price','decimal(12,2)','单价'],['total_amount','decimal(14,2)','订单总额'],
  ['discount_amount','decimal(12,2)','优惠金额'],['pay_amount','decimal(14,2)','实付金额'],
  ['coupon_code','varchar(64)','优惠券编码'],['coupon_amount','decimal(12,2)','优惠券抵扣'],
  ['pay_type','tinyint(2)','支付方式'],['pay_time','datetime','支付时间'],
  ['equity_code','varchar(64)','使用的权益编码'],['contact_name','varchar(50)','联系人姓名'],
  ['contact_phone','varchar(20)','联系人电话'],['remark','varchar(500)','备注'],
  ['order_status','tinyint(2)','状态'],['cancel_reason','varchar(500)','取消原因'],
]);
T['order_course'] = f([
  ['order_code','varchar(50)','订单编号'],['order_type','tinyint(2)','订单类型'],
  ['channel_code','varchar(50)','渠道编码'],['channel_full_name','varchar(200)','渠道名称(快照)'],
  ['agent_code','varchar(64)','代理人编码'],['agent_full_name','varchar(50)','代理人姓名(快照)'],
  ['distributor_code','varchar(64)','分销商编码'],['distributor_full_name','varchar(200)','分销商名称(快照)'],
  ['client_code','varchar(64)','客户编码'],['client_full_name','varchar(50)','客户姓名(快照)'],
  ['goods_code','varchar(50)','商品编码'],['course_code','varchar(64)','课程编码'],
  ['course_name','varchar(200)','课程名称'],['sku_code','varchar(64)','SKU编码'],
  ['quantity','int(11)','购买数量'],['unit_price','decimal(12,2)','单价'],
  ['total_amount','decimal(14,2)','订单总额'],['discount_amount','decimal(12,2)','优惠金额'],
  ['pay_amount','decimal(14,2)','实付金额'],['coupon_code','varchar(64)','优惠券编码'],
  ['pay_type','tinyint(2)','支付方式'],['pay_time','datetime','支付时间'],
  ['equity_code','varchar(64)','使用的权益编码'],['order_status','tinyint(2)','状态'],
  ['cancel_reason','varchar(500)','取消原因'],['remark','varchar(500)','备注'],
]);
T['order_sojourn'] = f([
  ['order_code','varchar(50)','订单编号'],['order_type','tinyint(2)','订单类型'],
  ['channel_code','varchar(50)','渠道编码'],['channel_full_name','varchar(200)','渠道名称(快照)'],
  ['agent_code','varchar(64)','代理人编码'],['agent_full_name','varchar(50)','代理人姓名(快照)'],
  ['distributor_code','varchar(64)','分销商编码'],['distributor_full_name','varchar(200)','分销商名称(快照)'],
  ['client_code','varchar(64)','客户编码'],['client_full_name','varchar(50)','客户姓名(快照)'],
  ['goods_code','varchar(50)','商品编码'],['park_code','varchar(64)','机构编码'],
  ['park_full_name','varchar(200)','机构名称(快照)'],['room_type_code','varchar(64)','房间类型编码'],
  ['sku_code','varchar(64)','SKU编码'],['checkin_date','date','入住日期'],
  ['checkout_date','date','退房日期'],['stay_days','int(11)','入住天数'],
  ['resident_count','int(11)','入住人数'],['resident_names','text','入住人姓名'],
  ['care_type_code','varchar(64)','照护类型编码'],['food_type_code','varchar(64)','餐饮类型编码'],
  ['room_fee','decimal(12,2)','房间费用'],['care_fee','decimal(12,2)','照护费用'],
  ['food_fee','decimal(12,2)','餐饮费用'],['other_fee','decimal(12,2)','其他费用'],
  ['total_amount','decimal(14,2)','订单总额'],['discount_amount','decimal(12,2)','优惠金额'],
  ['pay_amount','decimal(14,2)','实付金额'],['coupon_code','varchar(64)','优惠券编码'],
  ['pay_type','tinyint(2)','支付方式'],['pay_time','datetime','支付时间'],
  ['deposit_amount','decimal(12,2)','押金金额'],['equity_code','varchar(64)','使用的权益编码'],
  ['contact_name','varchar(50)','联系人姓名'],['contact_phone','varchar(20)','联系人电话'],
  ['special_needs','text','特殊需求'],['order_status','tinyint(2)','状态'],
  ['cancel_reason','varchar(500)','取消原因'],['remark','varchar(500)','备注'],
]);

// ----- finance_ (7) -----
T['finance_flow'] = f([
  ['flow_code','varchar(50)','流水编号'],['flow_type','tinyint(2)','流水类型'],
  ['biz_type','varchar(50)','业务类型'],['biz_code','varchar(64)','业务编码'],
  ['account_type','varchar(30)','账号类型'],['account_code','varchar(50)','账号编码'],
  ['flow_amount','decimal(14,2)','流水金额'],['balance_before','decimal(14,2)','变动前余额'],
  ['balance_after','decimal(14,2)','变动后余额'],['pay_type','tinyint(2)','支付方式'],
  ['trade_no','varchar(100)','交易流水号'],['counterparty_type','varchar(30)','对方类型'],
  ['counterparty_code','varchar(50)','对方编码'],['counterparty_name','varchar(200)','对方名称'],
  ['flow_description','varchar(500)','流水描述'],['flow_time','datetime','流水时间'],
  ['is_settled','tinyint(1)','是否已结算'],['settle_code','varchar(64)','结算单编码'],
  ['status','tinyint(2)','状态'],['remark','varchar(500)','备注'],
]);
T['finance_bill'] = f([
  ['bill_code','varchar(50)','结算单编号'],['bill_type','tinyint(2)','结算类型'],
  ['target_type','varchar(30)','结算对象类型'],['target_code','varchar(50)','结算对象编码'],
  ['target_name','varchar(200)','结算对象名称'],['period_start','date','结算周期开始'],
  ['period_end','date','结算周期结束'],['order_count','int(11)','订单数量'],
  ['total_amount','decimal(14,2)','结算总额'],['commission_amount','decimal(14,2)','分销手续费金额'],
  ['refund_amount','decimal(14,2)','退款金额'],['adjust_amount','decimal(14,2)','调整金额'],
  ['final_amount','decimal(14,2)','最终结算金额'],['flow_ids','text','关联流水ID列表'],
  ['settlement_method','tinyint(2)','结算方式'],['bank_info','varchar(500)','收款银行信息'],
  ['apply_time','datetime','申请时间'],['audit_time','datetime','审核时间'],
  ['settle_time','datetime','结算完成时间'],['auditor_code','varchar(64)','审核人编码'],
  ['auditor_name','varchar(50)','审核人姓名'],['audit_remark','varchar(500)','审核备注'],
  ['bill_status','tinyint(2)','状态'],['remark','varchar(500)','备注'],
]);
T['finance_invoice'] = f([
  ['invoice_code','varchar(50)','发票编码'],['invoice_type','tinyint(2)','发票类型'],
  ['bill_code','varchar(64)','关联结算单编码'],['order_code','varchar(64)','关联订单编码'],
  ['applicant_type','varchar(30)','申请方类型'],['applicant_code','varchar(50)','申请方编码'],
  ['applicant_name','varchar(200)','申请方名称'],['title_type','tinyint(2)','抬头类型'],
  ['invoice_title','varchar(200)','发票抬头'],['tax_no','varchar(50)','纳税人识别号'],
  ['bank_name','varchar(100)','开户银行'],['bank_account','varchar(50)','银行账号'],
  ['register_address','varchar(500)','注册地址'],['register_phone','varchar(20)','注册电话'],
  ['invoice_amount','decimal(14,2)','开票金额'],['invoice_content','varchar(500)','发票内容'],
  ['receiver_name','varchar(50)','收件人姓名'],['receiver_phone','varchar(20)','收件人电话'],
  ['receiver_address','varchar(500)','收件地址'],['receiver_email','varchar(100)','收件邮箱'],
  ['invoice_no','varchar(50)','发票号码'],['invoice_url','varchar(500)','发票文件URL'],
  ['apply_time','datetime','申请时间'],['issue_time','datetime','开票时间'],
  ['send_time','datetime','寄出时间'],['invoice_status','tinyint(2)','状态'],
  ['remark','varchar(500)','备注'],
]);
T['finance_account'] = f([
  ['account_code','varchar(50)','账目编码'],['direction','tinyint(2)','账目方向'],
  ['account_type','varchar(30)','对象类型'],['target_code','varchar(50)','对象编码'],
  ['target_name','varchar(200)','对象名称'],['biz_type','varchar(50)','业务类型'],
  ['biz_code','varchar(64)','业务编码'],['total_amount','decimal(14,2)','应收/应付总额'],
  ['received_amount','decimal(14,2)','已收/已付金额'],['remain_amount','decimal(14,2)','未收/未付金额'],
  ['due_date','date','到期日期'],['last_receive_time','datetime','最近收款/付款时间'],
  ['account_status','tinyint(2)','状态'],['remark','varchar(500)','备注'],
]);
T['finance_reconciliation'] = f([
  ['recon_code','varchar(50)','对账编码'],['recon_type','tinyint(2)','对账类型'],
  ['target_code','varchar(50)','对账对象编码'],['target_name','varchar(200)','对账对象名称'],
  ['period_start','date','对账周期开始'],['period_end','date','对账周期结束'],
  ['our_order_count','int(11)','我方订单数'],['our_total_amount','decimal(14,2)','我方总金额'],
  ['their_order_count','int(11)','对方订单数'],['their_total_amount','decimal(14,2)','对方总金额'],
  ['diff_count','int(11)','差异订单数'],['diff_amount','decimal(14,2)','差异金额'],
  ['diff_detail','text','差异明细'],['recon_result','tinyint(2)','对账结果'],
  ['handle_result','text','差异处理结果'],['recon_time','datetime','对账时间'],
  ['operator_code','varchar(64)','操作人编码'],['operator_name','varchar(50)','操作人姓名'],
  ['status','tinyint(2)','状态'],['remark','varchar(500)','备注'],
]);
T['finance_payment'] = f([
  ['payment_code','varchar(50)','支付流水号'],['order_type','tinyint(2)','订单类型'],
  ['order_code','varchar(50)','订单编号'],['pay_type','tinyint(2)','支付方式'],
  ['pay_amount','decimal(14,2)','支付金额'],['trade_no','varchar(100)','第三方交易号'],
  ['payer_account','varchar(100)','付款方账号'],['payee_account','varchar(100)','收款方账号'],
  ['pay_time','datetime','支付时间'],['notify_time','datetime','回调通知时间'],
  ['pay_status','tinyint(2)','支付状态'],['pay_description','varchar(500)','支付说明'],
  ['extra_data','text','扩展数据'],
]);
T['finance_refund'] = f([
  ['refund_code','varchar(50)','退款编码'],['order_type','tinyint(2)','订单类型'],
  ['order_code','varchar(50)','订单编号'],['payment_code','varchar(64)','原支付记录编码'],
  ['refund_amount','decimal(14,2)','退款金额'],['refund_reason','varchar(500)','退款原因'],
  ['refund_type','tinyint(2)','退款类型'],['refund_channel','tinyint(2)','退款渠道'],
  ['refund_trade_no','varchar(100)','退款交易号'],['apply_time','datetime','申请时间'],
  ['audit_time','datetime','审核时间'],['refund_time','datetime','退款完成时间'],
  ['auditor_code','varchar(64)','审核人编码'],['auditor_name','varchar(50)','审核人姓名'],
  ['audit_remark','varchar(500)','审核备注'],['refund_status','tinyint(2)','状态'],
  ['remark','varchar(500)','备注'],
]);

// ----- distributor_ (1) -----
T['distributor_info'] = f([
  ['distributor_code','varchar(50)','分销商编码'],['full_name','varchar(200)','分销商全称'],
  ['short_name','varchar(50)','简称'],['subject_type','tinyint(2)','主体类型'],
  ['unified_credit_code','varchar(50)','统一社会信用代码'],['legal_person','varchar(50)','法定代表人'],
  ['business_license_no','varchar(100)','营业执照号'],['registered_capital','decimal(12,2)','注册资本'],
  ['establish_date','date','成立日期'],['id_card','varchar(20)','身份证号'],
  ['gender','tinyint(1)','性别'],['phone','varchar(20)','联系电话'],
  ['contact_person','varchar(50)','联系人'],['contact_email','varchar(100)','联系邮箱'],
  ['province_code','varchar(20)','省份编码'],['city_code','varchar(20)','城市编码'],
  ['district_code','varchar(20)','区划编码'],['address','varchar(500)','详细地址'],
  ['bank_name','varchar(100)','开户银行'],['bank_account','varchar(50)','银行账号'],
  ['bank_account_name','varchar(100)','银行户名'],['status','tinyint(2)','状态'],
  ['sort_order','int(11)','排序号'],['remark','varchar(500)','备注'],
]);

// ============= RUN =============
// Build prefix map: tableName -> module pkg
const PREFIX_TO_MOD = {
  system_: 'system', organ_: 'organ', butler_: 'butler', supplier_: 'supplier',
  park_: 'park', scene_: 'scene', channel_: 'channel', agent_: 'agent',
  client_: 'client', equity_: 'equity', service_: 'service', goods_: 'goods',
  content_: 'content', course_: 'course', order_: 'order', finance_: 'finance',
  distributor_: 'distributor',
};

const stats = {};
let totalEntities = 0;
let totalMappers = 0;
let totalTables = 0;

// 1) Build all module skeletons (pom + placeholder package-info)
for (const mod of MODULES) {
  buildModuleSkeleton(mod);
  stats[mod.pkg] = { entities: 0, mappers: 0 };
}

// 2) Generate entity + mapper for every table
for (const [tableName, fields] of Object.entries(T)) {
  // Resolve module by prefix
  let pkg = null;
  for (const [prefix, p] of Object.entries(PREFIX_TO_MOD)) {
    if (tableName.startsWith(prefix)) { pkg = p; break; }
  }
  if (!pkg) throw new Error(`No module prefix matches table: ${tableName}`);
  const mod = MODULES.find(m => m.pkg === pkg);
  if (!mod) throw new Error(`No module for pkg ${pkg} (table ${tableName})`);

  writeEntity(mod, tableName, fields);
  writeMapper(mod, tableName);
  stats[pkg].entities++;
  stats[pkg].mappers++;
  totalEntities++;
  totalMappers++;
  totalTables++;
}

console.log('=== Generation complete ===');
console.log(`Total tables: ${totalTables}, total entities: ${totalEntities}, total mappers: ${totalMappers}`);
for (const mod of MODULES) {
  const s = stats[mod.pkg];
  console.log(`  ${mod.dir.padEnd(24)} entities=${s.entities} mappers=${s.mappers}`);
}

// Sanity: count expected per spec
const EXPECTED = {
  system:18, organ:9, butler:8, supplier:10, park:15, scene:5, channel:11,
  agent:6, client:7, equity:6, service:7, goods:5, content:5, course:3,
  order:4, finance:7, distributor:1,
};
let mismatch = false;
for (const [pkg, exp] of Object.entries(EXPECTED)) {
  const got = stats[pkg].entities;
  if (got !== exp) {
    console.error(`!! ${pkg}: expected ${exp}, got ${got}`);
    mismatch = true;
  }
}
console.log(mismatch ? '!! MISMATCH detected, check counts' : 'All module counts match spec (127 total).');
