package com.tastelink.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tastelink.common.Constants;
import com.tastelink.entity.Shop;
import com.tastelink.entity.ShopDoc;
import com.tastelink.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ES 店铺索引对账重建（v2 Phase D）。
 * <p>
 * Canal 增量同步延后（infra 待办 B）时，此处是 ES↔MySQL 同步的唯一源；Canal 落地后退化为 drift 修正。
 * 以 MySQL {@code status=NORMAL} 全量快照灌 ES（快照式：覆盖上次脏成员）。ES 任意异常吞掉，
 * 与 Phase A {@code ScheduledRankRebuild} 同范式——不抛、靠下一周期重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledShopReconcile {

    private final ShopMapper shopMapper;
    private final ElasticsearchOperations esOps;

    @Value("${tastelink.search.enabled:true}")
    private boolean enabled;

    /** 默认每 10 分钟；cron 由 tastelink.search.rebuild-cron 配置。 */
    @Scheduled(cron = "${tastelink.search.rebuild-cron:0 */10 * * * *}")
    public void reconcile() {
        if (!enabled) {
            return;
        }
        try {
            List<Shop> shops = shopMapper.selectList(new LambdaQueryWrapper<Shop>()
                    .eq(Shop::getStatus, Constants.STATUS_NORMAL));
            if (shops.isEmpty()) {
                return;
            }
            List<ShopDoc> docs = shops.stream().map(this::toDoc).toList();
            esOps.save(docs);
            log.info("shop index reconciled: {} docs", docs.size());
        } catch (Exception e) {
            log.warn("shop index reconcile failed, will retry next cycle: {}", e.getMessage());
        }
    }

    private ShopDoc toDoc(Shop s) {
        ShopDoc d = new ShopDoc();
        d.setId(s.getId());
        d.setName(s.getName());
        d.setAddress(s.getAddress());
        d.setDescription(s.getDescription());
        d.setStatus(s.getStatus());
        d.setCategoryId(s.getCategoryId());
        d.setCity(s.getCity());
        return d;
    }
}
