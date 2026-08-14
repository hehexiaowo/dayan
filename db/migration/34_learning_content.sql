-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- migration 34: 学习中心内容表
-- 代理人端学习中心，3 个分类：1=视频课程 2=图文课程 3=雁鸣中国
-- 全局表（无 channel_code），所有代理人可见，需加入 dayan.tenant.ignore-tables

CREATE TABLE IF NOT EXISTS learning_content (
  id          BIGINT       NOT NULL COMMENT '雪花ID',
  content_code VARCHAR(50) NOT NULL COMMENT '内容编码 LC+yyyyMMdd+seq',
  title       VARCHAR(200) NOT NULL COMMENT '标题',
  summary     VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  category    TINYINT      NOT NULL COMMENT '分类 1=视频课程 2=图文课程 3=雁鸣中国',
  author      VARCHAR(100) DEFAULT NULL COMMENT '讲师/作者/来源',
  duration    VARCHAR(20)  DEFAULT NULL COMMENT '时长（视频 28:30 / 图文 约15分钟）',
  body        TEXT         COMMENT '正文（详情页用，列表不返回）',
  view_count  INT          NOT NULL DEFAULT 0 COMMENT '阅读/播放量',
  badge       VARCHAR(20)  DEFAULT NULL COMMENT '角标（热/新/要闻/人物/动态/洞察）',
  publish_time DATETIME    DEFAULT NULL COMMENT '发布时间',
  sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序权重（越大越靠前）',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1=上架 0=下架',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  creator     VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
  updater     VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删除',
  deleted_at  DATETIME     DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_content_code (content_code),
  KEY idx_category_status (category, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习中心内容';

-- ===== 视频课程 (category=1) =====
INSERT INTO learning_content (id, content_code, title, summary, category, author, duration, view_count, badge, publish_time, sort_order) VALUES
(1000001, 'LC20260808001', '泰康幸福年金产品全解析', '从保障责任到收益演示，30分钟讲透一款主力年金险', 1, '王芳 · 资深讲师', '28:30', 12450, '热', '2026-08-08 10:00:00', 40),
(1000002, 'LC20260806001', '客户异议处理实战技巧', '5步化解「再考虑考虑」「回去和家人商量」', 1, '李军 · 销售总监', '15:20', 8730, NULL, '2026-08-06 10:00:00', 30),
(1000003, 'LC20260811001', '养老社区参观体验式营销', '如何把一次参观变成一场成交', 1, '张敏 · 金牌代理', '22:10', 6210, '新', '2026-08-11 10:00:00', 20),
(1000004, 'LC20260803001', '高净值客户资产配置与年金逻辑', '用底层资产思维打开大单入口', 1, '陈伟 · 财富顾问', '35:40', 4380, NULL, '2026-08-03 10:00:00', 10);

-- ===== 图文课程 (category=2) =====
INSERT INTO learning_content (id, content_code, title, summary, category, author, duration, view_count, badge, publish_time, sort_order) VALUES
(1000011, 'LC20260807001', '2026 养老保险税优政策全解读', '个人养老金账户抵扣、递延纳税实操指南', 2, '政策研究组', '约 15 分钟', 21300, '热', '2026-08-07 10:00:00', 40),
(1000012, 'LC20260804001', '获客话术：从寒暄到需求挖掘的 20 个模板', '场景化话术卡片，开口不再难', 2, '销售训练营', '约 10 分钟', 18500, NULL, '2026-08-04 10:00:00', 30),
(1000013, 'LC20260810001', 'CCRC 持续照料社区模式科普', '独立生活—协助生活—专业护理一站式的底层逻辑', 2, '行业研究院', '约 8 分钟', 9820, '新', '2026-08-10 10:00:00', 20),
(1000014, 'LC20260802001', '转介绍技巧：让老客户主动为你背书', '3 个关键时机 + 1 套信任递进模型', 2, '资深导师团', '约 12 分钟', 15400, NULL, '2026-08-02 10:00:00', 10);

-- ===== 雁鸣中国 (category=3) =====
INSERT INTO learning_content (id, content_code, title, summary, category, author, duration, view_count, badge, publish_time, sort_order) VALUES
(1000021, 'LC20260810002', '大雁养老与平安养老达成战略合作', '共建康养生态，覆盖 30 省 200+ 城市', 3, '大雁要闻', NULL, 5670, '要闻', '2026-08-10 09:00:00', 40),
(1000022, 'LC20260806002', '第七届中国养老产业峰会精华回顾', '10 位行业领袖观点：银发经济的下一个十年', 3, '行业动态', NULL, 3920, '动态', '2026-08-06 09:00:00', 30),
(1000023, 'LC20260801001', '月度之星：代理人单月签单 30 万的秘诀', '从 0 到金牌，她只用了这三个动作', 3, '大雁人物', NULL, 8100, '人物', '2026-08-01 09:00:00', 20),
(1000024, 'LC20260728001', '银发经济蓝皮书：2026 养老消费趋势', '中高收入长者愿为什么付费？四组数据说清楚', 3, '趋势洞察', NULL, 6730, '洞察', '2026-07-28 09:00:00', 10);
