package com.dayan.system.service.impl;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.system.dto.SystemKnowledgeIndexConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemKnowledgeRepoServiceImplTest {

    private static SystemKnowledgeIndexConfig base() {
        SystemKnowledgeIndexConfig cfg = new SystemKnowledgeIndexConfig();
        cfg.setChunkMode(null);
        cfg.setChunkSize(500);
        cfg.setOverlapSize(100);
        cfg.setDenseTopK(4);
        cfg.setSparseTopK(4);
        cfg.setRerankMinScore(0.01);
        return cfg;
    }

    @Test
    void updatableFieldsPass() {
        SystemKnowledgeIndexConfig incoming = base();
        incoming.setDenseTopK(8);
        incoming.setSparseTopK(8);
        incoming.setRerankMinScore(0.3);
        assertDoesNotThrow(() -> SystemKnowledgeRepoServiceImpl.assertUpdatableConfig(base(), incoming));
    }

    @Test
    void chunkChangeRejected() {
        SystemKnowledgeIndexConfig incoming = base();
        incoming.setChunkMode("regex");
        incoming.setSeparator("(?<=。)");
        BusinessException e = assertThrows(BusinessException.class,
                () -> SystemKnowledgeRepoServiceImpl.assertUpdatableConfig(base(), incoming));
        assertTrue(e.getMessage().contains("建库后不可修改"));
    }

    @Test
    void embeddingModelChangeRejected() {
        SystemKnowledgeIndexConfig incoming = base();
        incoming.setEmbeddingModel("text-embedding-v4");
        assertThrows(BusinessException.class,
                () -> SystemKnowledgeRepoServiceImpl.assertUpdatableConfig(base(), incoming));
    }

    @Test
    void nullIncomingPasses() {
        assertDoesNotThrow(() -> SystemKnowledgeRepoServiceImpl.assertUpdatableConfig(base(), null));
        assertDoesNotThrow(() -> SystemKnowledgeRepoServiceImpl.assertUpdatableConfig(null, base()));
    }
}
