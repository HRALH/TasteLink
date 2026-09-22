package com.tastelink.config;

import com.tastelink.entity.Shop;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.service.HotRankService;
import com.tastelink.service.ShopCleanupService;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * XXL-JOB 接入契约单测（Mockito，不依赖 Docker/admin，随 {@code mvn test} 运行）。
 * <p>
 * 覆盖三类真实故障面：
 * <ol>
 *   <li><b>handler 名契约</b>——代码里的 {@code @XxlJob} 值必须与 admin 侧任务配置的 JobHandler 一致，
 *       不一致时任务<b>静默不触发</b>（admin 只在执行日志里报「handler not found」）；
 *   <li><b>委托正确性</b>——handler 确实调到对应服务，而非空实现；
 *   <li><b>开关互斥</b>——{@code xxl-job.enabled=true} 时本地 {@code @Scheduled} 必须让位，
 *       否则「admin 触发 + 本地 cron」会跑两遍。
 * </ol>
 * 真实 admin 联动（注册、派发、回调）需部署 xxl-job-admin，见 docs/08。
 */
@ExtendWith(MockitoExtension.class)
class XxlJobHandlerTest {

    @Mock
    private HotRankService hotRankService;
    @Mock
    private ShopMapper shopMapper;
    @Mock
    private ShopCleanupService shopCleanupService;
    @Mock
    private ElasticsearchOperations esOps;

    // ---------- handler 名契约（与 admin 侧配置必须一致） ----------

    @Test
    void handlerNames_matchAdminConfiguration() throws Exception {
        assertEquals("rankRebuildHandler", xxlJobName(ScheduledRankRebuild.class, "rebuildByXxlJob"));
        assertEquals("shopCleanupReconcileHandler",
                xxlJobName(ScheduledShopCleanupReconcile.class, "reconcileByXxlJob"));
        assertEquals("shopIndexReconcileHandler",
                xxlJobName(ScheduledShopReconcile.class, "reconcileByXxlJob"));
    }

    // ---------- 委托正确性 ----------

    @Test
    void rankRebuildHandler_delegatesWithConfiguredTopSize() {
        ScheduledRankRebuild s = new ScheduledRankRebuild(hotRankService);
        ReflectionTestUtils.setField(s, "rebuildTopSize", 200);

        s.rebuildByXxlJob();

        verify(hotRankService).rebuild(200);
    }

    @Test
    void shopCleanupHandler_cleansLingeringShops() {
        ScheduledShopCleanupReconcile s = new ScheduledShopCleanupReconcile(shopMapper, shopCleanupService);
        ReflectionTestUtils.setField(s, "delayMs", 5000L);
        ReflectionTestUtils.setField(s, "graceMs", 30000L);
        Shop hidden = new Shop();
        hidden.setId(7L);
        hidden.setStatus(0);
        when(shopMapper.selectList(any())).thenReturn(List.of(hidden));

        s.reconcileByXxlJob();

        verify(shopCleanupService).cleanup(7L);
    }

    @Test
    void shopIndexHandler_rebuildsIndexWhenEnabled() {
        ScheduledShopReconcile s = new ScheduledShopReconcile(shopMapper, esOps);
        ReflectionTestUtils.setField(s, "enabled", true);

        s.reconcileByXxlJob();

        // 空库：走完一轮分页即退出，不触碰 ES（Page 默认 records 为空）
        verify(shopMapper).selectPage(any(), any());
        verifyNoInteractions(esOps);
    }

    @Test
    void shopIndexHandler_honorsSearchSwitchEvenWhenTriggeredByAdmin() {
        ScheduledShopReconcile s = new ScheduledShopReconcile(shopMapper, esOps);
        ReflectionTestUtils.setField(s, "enabled", false);

        s.reconcileByXxlJob();

        // SEARCH_ENABLED=false 时即便 admin 派发也不查库不灌 ES
        verifyNoInteractions(shopMapper, esOps);
    }

    // ---------- 开关互斥：xxl-job 开启时本地 cron 必须让位 ----------

    @Test
    void scheduledTick_skipsWhenXxlJobEnabled() {
        ScheduledRankRebuild rank = new ScheduledRankRebuild(hotRankService);
        ReflectionTestUtils.setField(rank, "rebuildTopSize", 200);
        ReflectionTestUtils.setField(rank, "xxlJobEnabled", true);
        rank.rebuild();
        verifyNoInteractions(hotRankService);

        ScheduledShopCleanupReconcile cleanup =
                new ScheduledShopCleanupReconcile(shopMapper, shopCleanupService);
        ReflectionTestUtils.setField(cleanup, "xxlJobEnabled", true);
        cleanup.reconcile();
        verifyNoInteractions(shopMapper, shopCleanupService);

        ScheduledShopReconcile index = new ScheduledShopReconcile(shopMapper, esOps);
        ReflectionTestUtils.setField(index, "xxlJobEnabled", true);
        index.reconcile();
        verifyNoInteractions(shopMapper, esOps);
    }

    @Test
    void scheduledTick_stillRunsWhenXxlJobDisabled() {
        ScheduledRankRebuild rank = new ScheduledRankRebuild(hotRankService);
        ReflectionTestUtils.setField(rank, "rebuildTopSize", 200);
        ReflectionTestUtils.setField(rank, "xxlJobEnabled", false);

        rank.rebuild();

        // 默认关闭时保持引入 XXL-JOB 之前的本地调度行为（零回归）
        verify(hotRankService).rebuild(200);
        verify(hotRankService, never()).rebuild(0);
    }

    private static String xxlJobName(Class<?> type, String methodName) throws Exception {
        Method m = type.getDeclaredMethod(methodName);
        XxlJob ann = m.getAnnotation(XxlJob.class);
        assertNotNull(ann, methodName + " 缺少 @XxlJob 注解——admin 侧无法派发");
        return ann.value();
    }
}
