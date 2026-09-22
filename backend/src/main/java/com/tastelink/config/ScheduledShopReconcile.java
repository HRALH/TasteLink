package com.tastelink.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tastelink.common.Constants;
import com.tastelink.entity.Shop;
import com.tastelink.entity.ShopDoc;
import com.tastelink.mapper.ShopMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
 * <p>
 * B3-3 起分批灌（每批 {@code BATCH_SIZE} 条），避免大数据量时打满 DB/ES 并阻塞其他调度。
 * <p>
 * B5 起触发方式二选一：{@code tastelink.xxl-job.enabled=true} 时由调度中心按 admin 侧 cron 触发
 * {@link #reconcileByXxlJob()}，本地 {@code @Scheduled} 空转；为 false（默认）时仍走本地 cron + ShedLock。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledShopReconcile {

    private static final int BATCH_SIZE = 500;

    private final ShopMapper shopMapper;
    private final ElasticsearchOperations esOps;

    @Value("${tastelink.search.enabled:true}")
    private boolean enabled;

    @Value("${tastelink.xxl-job.enabled:false}")
    private boolean xxlJobEnabled;

    /** 默认每 10 分钟；cron 由 tastelink.search.rebuild-cron 配置。xxl-job 开启时本方法让位。 */
    @Scheduled(cron = "${tastelink.search.rebuild-cron:0 */10 * * * *}")
    @SchedulerLock(name = "shop-index-reconcile", lockAtMostFor = "${tastelink.shedlock.shop-index-reconcile:PT8M}",
            lockAtLeastFor = "PT10S")
    public void reconcile() {
        if (xxlJobEnabled) {
            return;
        }
        doReconcile();
    }

    /**
     * XXL-JOB 触发器：handler 名须与 admin 侧任务的 JobHandler 完全一致。
     * 仍受 {@code tastelink.search.enabled} 约束——ES 关闭时即便 admin 触发也直接返回成功。
     * <p>勿加 {@code @SchedulerLock}——本地空转仍会短暂持同名锁，可能把本次运行静默跳过（见 XxlJobConfig 类注释）。
     */
    @XxlJob("shopIndexReconcileHandler")
    public void reconcileByXxlJob() {
        int total = doReconcile();
        XxlJobHelper.handleSuccess("shop docs indexed: " + total);
    }

    /**
     * 全量快照灌 ES，返回本轮写入的文档数。
     * <p>
     * ES 异常在此吞掉（与 Phase A/C 同范式：不抛、靠下一周期重试），故 XXL-JOB 侧恒为成功，
     * 失败详情看应用日志 warn。{@code SEARCH_ENABLED=false} 时返回 0 且不触碰 ES。
     */
    private int doReconcile() {
        if (!enabled) {
            return 0;
        }
        try {
            int total = 0;
            long current = 1;
            // 分页拉取 status=NORMAL 店铺，每批转 ShopDoc 后灌 ES；MP Page 1-based
            for (;;) {
                Page<Shop> page = new Page<>(current, BATCH_SIZE, false);
                shopMapper.selectPage(page, new LambdaQueryWrapper<Shop>()
                        .eq(Shop::getStatus, Constants.STATUS_NORMAL));
                List<Shop> shops = page.getRecords();
                if (shops.isEmpty()) {
                    break;
                }
                List<ShopDoc> docs = shops.stream().map(this::toDoc).toList();
                esOps.save(docs);
                total += docs.size();
                if (shops.size() < BATCH_SIZE) {
                    break;
                }
                current++;
            }
            if (total > 0) {
                log.info("shop index reconciled: {} docs", total);
            }
            return total;
        } catch (Exception e) {
            log.warn("shop index reconcile failed, will retry next cycle: {}", e.getMessage());
            return 0;
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
