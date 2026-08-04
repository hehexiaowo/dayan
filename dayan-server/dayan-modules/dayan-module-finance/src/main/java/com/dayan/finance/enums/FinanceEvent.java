package com.dayan.finance.enums;

/**
 * 结算域（finance_）状态/类型常量。
 *
 * <p>结算域 7 个 status 字段均不走 StateMachineEngine（无 SM 种子），用 if-else 校验合法前置态，
 * 非法流转抛 BusinessException。本类固化所有状态/类型取值常量（public static final int），
 * 与 {@code db/migration/16_finance.sql} 注释逐字对齐。
 *
 * <p>状态码对照（DDL 注释唯一来源）：
 * <ul>
 *   <li>finance_flow.flow_type   1=收入/2=支出/3=退款/4=结算</li>
 *   <li>finance_flow.status      0=已冲正/1=正常</li>
 *   <li>finance_bill.bill_status 0=待审核/1=审核通过/2=结算中/3=已结算/4=审核拒绝</li>
 *   <li>finance_invoice.invoice_status 0=待审核/1=已审核/2=已开票/3=已寄出/4=已完成/5=已作废/6=已红冲</li>
 *   <li>finance_account.account_status 0=待收付/1=部分收付/2=已结清/3=已逾期/4=已坏账</li>
 *   <li>finance_account.direction 1=应收/2=应付</li>
 *   <li>finance_reconciliation.status 0=对账中/1=已完成/2=待确认/3=已确认</li>
 *   <li>finance_payment.pay_status 0=待支付/1=支付成功/2=支付失败/3=已退款/4=部分退款</li>
 *   <li>finance_refund.refund_status 0=待审核/1=审核通过/2=退款中/3=退款成功/4=审核拒绝/5=退款失败</li>
 * </ul>
 */
public final class FinanceEvent {

    private FinanceEvent() {
    }

    // ====== finance_flow.flow_type ======
    /** 流水类型：收入 */
    public static final int FLOW_TYPE_INCOME = 1;
    /** 流水类型：支出 */
    public static final int FLOW_TYPE_EXPENSE = 2;
    /** 流水类型：退款 */
    public static final int FLOW_TYPE_REFUND = 3;
    /** 流水类型：结算 */
    public static final int FLOW_TYPE_SETTLE = 4;

    // ====== finance_flow.status ======
    /** 流水状态：已冲正 */
    public static final int FLOW_STATUS_REVERSED = 0;
    /** 流水状态：正常 */
    public static final int FLOW_STATUS_NORMAL = 1;

    // ====== finance_bill.bill_status ======
    /** 结算单：待审核 */
    public static final int BILL_STATUS_PENDING_AUDIT = 0;
    /** 结算单：审核通过 */
    public static final int BILL_STATUS_AUDIT_PASS = 1;
    /** 结算单：结算中 */
    public static final int BILL_STATUS_SETTLING = 2;
    /** 结算单：已结算 */
    public static final int BILL_STATUS_SETTLED = 3;
    /** 结算单：审核拒绝 */
    public static final int BILL_STATUS_AUDIT_REJECT = 4;

    // ====== finance_invoice.invoice_status ======
    /** 发票：待审核 */
    public static final int INVOICE_STATUS_PENDING_AUDIT = 0;
    /** 发票：已审核 */
    public static final int INVOICE_STATUS_AUDITED = 1;
    /** 发票：已开票 */
    public static final int INVOICE_STATUS_ISSUED = 2;
    /** 发票：已寄出 */
    public static final int INVOICE_STATUS_SENT = 3;
    /** 发票：已完成 */
    public static final int INVOICE_STATUS_DONE = 4;
    /** 发票：已作废 */
    public static final int INVOICE_STATUS_VOID = 5;
    /** 发票：已红冲 */
    public static final int INVOICE_STATUS_RED_FLUSH = 6;

    // ====== finance_account.account_status ======
    /** 账目：待收/付 */
    public static final int ACCOUNT_STATUS_PENDING = 0;
    /** 账目：部分收/付 */
    public static final int ACCOUNT_STATUS_PARTIAL = 1;
    /** 账目：已结清 */
    public static final int ACCOUNT_STATUS_SETTLED = 2;
    /** 账目：已逾期 */
    public static final int ACCOUNT_STATUS_OVERDUE = 3;
    /** 账目：已坏账 */
    public static final int ACCOUNT_STATUS_BAD_DEBT = 4;

    // ====== finance_account.direction ======
    /** 账目方向：应收 */
    public static final int DIRECTION_RECEIVABLE = 1;
    /** 账目方向：应付 */
    public static final int DIRECTION_PAYABLE = 2;

    // ====== finance_reconciliation.status ======
    /** 对账：对账中 */
    public static final int RECON_STATUS_DOING = 0;
    /** 对账：已完成 */
    public static final int RECON_STATUS_DONE = 1;
    /** 对账：待确认 */
    public static final int RECON_STATUS_PENDING_CONFIRM = 2;
    /** 对账：已确认 */
    public static final int RECON_STATUS_CONFIRMED = 3;

    /** 对账结果：有差异 */
    public static final int RECON_RESULT_DIFF = 0;
    /** 对账结果：一致 */
    public static final int RECON_RESULT_MATCH = 1;

    // ====== finance_payment.pay_status ======
    /** 支付：待支付 */
    public static final int PAY_STATUS_PENDING = 0;
    /** 支付：支付成功 */
    public static final int PAY_STATUS_SUCCESS = 1;
    /** 支付：支付失败 */
    public static final int PAY_STATUS_FAILED = 2;
    /** 支付：已退款 */
    public static final int PAY_STATUS_REFUNDED = 3;
    /** 支付：部分退款 */
    public static final int PAY_STATUS_PARTIAL_REFUNDED = 4;

    // ====== finance_refund.refund_status ======
    /** 退款：待审核 */
    public static final int REFUND_STATUS_PENDING_AUDIT = 0;
    /** 退款：审核通过 */
    public static final int REFUND_STATUS_AUDIT_PASS = 1;
    /** 退款：退款中 */
    public static final int REFUND_STATUS_REFUNDING = 2;
    /** 退款：退款成功 */
    public static final int REFUND_STATUS_SUCCESS = 3;
    /** 退款：审核拒绝 */
    public static final int REFUND_STATUS_AUDIT_REJECT = 4;
    /** 退款：退款失败 */
    public static final int REFUND_STATUS_FAILED = 5;

    // ====== 通用：pay_type（支付方式字典） ======
    /** 支付方式：微信支付 */
    public static final int PAY_TYPE_WECHAT = 1;
    /** 支付方式：支付宝 */
    public static final int PAY_TYPE_ALIPAY = 2;
    /** 支付方式：银行转账 */
    public static final int PAY_TYPE_BANK = 3;
    /** 支付方式：余额支付 */
    public static final int PAY_TYPE_BALANCE = 4;
    /** 支付方式：线下支付 */
    public static final int PAY_TYPE_OFFLINE = 5;

    // ====== 通用：order_type（订单类型） ======
    /** 订单类型：权益 */
    public static final int ORDER_TYPE_EQUITY = 1;
    /** 订单类型：场景 */
    public static final int ORDER_TYPE_SCENE = 2;
    /** 订单类型：课程 */
    public static final int ORDER_TYPE_COURSE = 3;
    /** 订单类型：旅居 */
    public static final int ORDER_TYPE_SOJOURN = 4;

    // ====== biz_type / account_type 字符串常量（finance_flow） ======
    /** 业务类型：权益订单 */
    public static final String BIZ_TYPE_EQUITY_ORDER = "equity_order";
    /** 业务类型：场景订单 */
    public static final String BIZ_TYPE_SCENE_ORDER = "scene_order";
    /** 业务类型：课程订单 */
    public static final String BIZ_TYPE_COURSE_ORDER = "course_order";
    /** 业务类型：旅居订单 */
    public static final String BIZ_TYPE_TRAVEL_ORDER = "travel_order";
    /** 业务类型：结算 */
    public static final String BIZ_TYPE_SETTLEMENT = "settlement";

    /** 账号类型：机构 */
    public static final String ACCOUNT_TYPE_ORGAN = "organ";
    /** 账号类型：渠道 */
    public static final String ACCOUNT_TYPE_CHANNEL = "channel";
    /** 账号类型：代理 */
    public static final String ACCOUNT_TYPE_AGENT = "agent";
    /** 账号类型：客户 */
    public static final String ACCOUNT_TYPE_CLIENT = "client";
    /** 账号类型：供应商 */
    public static final String ACCOUNT_TYPE_SUPPLIER = "supplier";

    // ====== 序列 key 与编码前缀（7 表互不冲突） ======
    /** 流水编号前缀 + 序列 key */
    public static final String FLOW_PREFIX = "FL";
    public static final String FLOW_SEQ_KEY = "code:seq:FL:0";
    public static final int FLOW_SEQ_WIDTH = 10;

    /** 结算单编号前缀 + 序列 key */
    public static final String BILL_PREFIX = "BL";
    public static final String BILL_SEQ_KEY = "code:seq:BL:0";
    public static final int BILL_SEQ_WIDTH = 10;

    /** 发票编号前缀 + 序列 key */
    public static final String INVOICE_PREFIX = "IV";
    public static final String INVOICE_SEQ_KEY = "code:seq:IV:0";
    public static final int INVOICE_SEQ_WIDTH = 10;

    /** 支付编号前缀 + 序列 key */
    public static final String PAYMENT_PREFIX = "PAY";
    public static final String PAYMENT_SEQ_KEY = "code:seq:PAY:0";
    public static final int PAYMENT_SEQ_WIDTH = 10;

    /** 退款编号前缀 + 序列 key */
    public static final String REFUND_PREFIX = "RF";
    public static final String REFUND_SEQ_KEY = "code:seq:RF:0";
    public static final int REFUND_SEQ_WIDTH = 10;

    /** 应收应付账目编号前缀 + 序列 key */
    public static final String ACCOUNT_PREFIX = "ACC";
    public static final String ACCOUNT_SEQ_KEY = "code:seq:ACC:0";
    public static final int ACCOUNT_SEQ_WIDTH = 10;

    /** 对账编号前缀 + 序列 key */
    public static final String RECON_PREFIX = "RC";
    public static final String RECON_SEQ_KEY = "code:seq:RC:0";
    public static final int RECON_SEQ_WIDTH = 10;
}
