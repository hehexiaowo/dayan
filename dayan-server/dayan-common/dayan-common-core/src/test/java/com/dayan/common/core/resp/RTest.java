package com.dayan.common.core.resp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link R} 统一响应封装测试。
 */
class RTest {

    @Test
    void ok_shouldHaveSuccessCode() {
        R<String> r = R.ok("data");
        assertThat(r.getCode()).isEqualTo(R.CODE_SUCCESS);
        assertThat(r.getMessage()).isEqualTo("success");
        assertThat(r.getData()).isEqualTo("data");
        assertThat(r.success()).isTrue();
        assertThat(r.getTimestamp()).isPositive();
    }

    @Test
    void ok_noData_shouldHaveNullData() {
        R<Void> r = R.ok();
        assertThat(r.getData()).isNull();
        assertThat(r.success()).isTrue();
    }

    @Test
    void fail_shouldHaveCodeAndMessage() {
        R<Void> r = R.fail(10400, "库存不足");
        assertThat(r.getCode()).isEqualTo(10400);
        assertThat(r.getMessage()).isEqualTo("库存不足");
        assertThat(r.success()).isFalse();
    }

    @Test
    void pageResult_shouldComputePages() {
        PageResult<String> pr = new PageResult<>(2, 10, 25, java.util.List.of("a", "b"));
        assertThat(pr.getPages()).isEqualTo(3); // 25 条 / 10 每页 = 3 页
        assertThat(pr.getRecords()).hasSize(2);
    }

    @Test
    void pageResult_empty() {
        PageResult<String> pr = PageResult.empty(1, 10);
        assertThat(pr.getTotal()).isZero();
        assertThat(pr.getRecords()).isEmpty();
    }

    @Test
    void pageResult_map_shouldTransformRecords() {
        PageResult<Integer> pr = new PageResult<>(1, 10, 2, java.util.List.of(1, 2));
        PageResult<String> mapped = pr.map(i -> "n" + i);
        assertThat(mapped.getRecords()).containsExactly("n1", "n2");
        assertThat(mapped.getTotal()).isEqualTo(2);
    }
}
