package com.dayan.system.dto;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemKnowledgeIndexConfigTest {

    @Test
    void jsonRoundTrip() {
        SystemKnowledgeIndexConfig cfg = new SystemKnowledgeIndexConfig();
        cfg.setChunkMode("regex");
        cfg.setSeparator("(?<=。)");
        cfg.setChunkSize(500);
        cfg.setOverlapSize(100);
        cfg.setEmbeddingModel("text-embedding-v3");
        cfg.setRerankModel("qwen3-rerank");
        cfg.setRerankMode("qa");
        cfg.setRerankMinScore(0.2);
        cfg.setEnableRewrite(true);
        cfg.setDenseTopK(8);
        cfg.setSparseTopK(8);
        SystemKnowledgeIndexConfig parsed = JSONUtil.toBean(JSONUtil.toJsonStr(cfg), SystemKnowledgeIndexConfig.class);
        assertEquals("regex", parsed.getChunkMode());
        assertEquals("(?<=。)", parsed.getSeparator());
        assertEquals(500, parsed.getChunkSize());
        assertEquals(100, parsed.getOverlapSize());
        assertEquals("text-embedding-v3", parsed.getEmbeddingModel());
        assertEquals("qwen3-rerank", parsed.getRerankModel());
        assertEquals("qa", parsed.getRerankMode());
        assertEquals(0.2, parsed.getRerankMinScore());
        assertEquals(Boolean.TRUE, parsed.getEnableRewrite());
        assertEquals(8, parsed.getDenseTopK());
        assertEquals(8, parsed.getSparseTopK());
    }

    @Test
    void validateRejectsInvalidRanges() {
        SystemKnowledgeIndexConfig cfg = new SystemKnowledgeIndexConfig();
        cfg.setChunkSize(6001);
        assertThrows(IllegalArgumentException.class, cfg::validate, "chunkSize > 6000 应报错");
        cfg.setChunkSize(0);
        assertThrows(IllegalArgumentException.class, cfg::validate, "chunkSize < 1 应报错");
        cfg.setChunkSize(200);
        cfg.setOverlapSize(200);
        assertThrows(IllegalArgumentException.class, cfg::validate, "overlap >= chunk 应报错");
        cfg.setOverlapSize(100);
        cfg.setOverlapSize(1025);
        assertThrows(IllegalArgumentException.class, cfg::validate, "overlapSize > 1024 应报错");
        cfg.setOverlapSize(100);
        cfg.setRerankMinScore(2.0);
        assertThrows(IllegalArgumentException.class, cfg::validate, "rerankMinScore 超界应报错");
        cfg.setRerankMinScore(0.5);
        cfg.setChunkMode("unknown");
        assertThrows(IllegalArgumentException.class, cfg::validate, "chunkMode 非法值应报错");
        cfg.setChunkMode("regex");
        assertThrows(IllegalArgumentException.class, cfg::validate, "chunkMode=regex 未填分隔符应报错");
    }

    @Test
    void validateAcceptsNullAsDefaults() {
        SystemKnowledgeIndexConfig cfg = new SystemKnowledgeIndexConfig();
        assertDoesNotThrow(cfg::validate);
    }

    @Test
    void toQueryMapContainsOnlySetFields() {
        SystemKnowledgeIndexConfig cfg = new SystemKnowledgeIndexConfig();
        cfg.setDenseTopK(8);
        cfg.setSparseTopK(8);
        cfg.setChunkMode("regex");
        cfg.setChunkSize(300);
        var map = cfg.toQueryMap();
        assertEquals("regex", map.get("chunkMode"));
        assertEquals("300", map.get("ChunkSize"));
        assertFalse(map.containsKey("Separator"), "未设置字段不应进 map");
        assertFalse(map.containsKey("denseTopK"), "已设 denseTopK 也不进 CreateIndex 参数");
        assertFalse(map.containsKey("sparseTopK"), "已设 sparseTopK 也不进 CreateIndex 参数");
    }
}
