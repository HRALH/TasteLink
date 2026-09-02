package com.tastelink.config;

import com.tastelink.entity.ShopDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * ES {@code shop} 索引初始化（v2 Phase D）：启动时若在线且索引缺失则按 {@link ShopDoc} mapping 创建。
 * <p>
 * ES 不可达不阻断启动——吞异常靠 {@code ScheduledShopReconcile} 对账重建 + SearchService 读时降级 LIKE。
 * 与 Phase A/C 同范式：连不上不影响主路径，对账兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopIndexInitializer implements ApplicationRunner {

    private final ElasticsearchOperations esOps;

    @Value("${tastelink.search.enabled:true}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        try {
            IndexOperations idx = esOps.indexOps(ShopDoc.class);
            if (!idx.exists()) {
                boolean created = idx.createWithMapping();
                log.info("shop es index created with mapping: result={} index={}", created, ShopDoc.INDEX);
            }
        } catch (Exception e) {
            // ES 未起/不可达：不阻断启动，待对账或人工启 ES 后由 reconcile 重建
            log.warn("shop es index init skipped (ES not reachable?): {}", e.getMessage());
        }
    }
}
