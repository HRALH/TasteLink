package com.tastelink.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * XXL-JOB 执行器配置（B5）：把三个对账/重建定时任务的触发权交给调度中心。
 * <p>
 * <b>开关默认 false，是刻意选择</b>：关闭时不创建执行器 bean、不绑端口、不连 admin，
 * 三个任务仍由 {@code @Scheduled + ShedLock} 驱动，行为与引入 XXL-JOB 之前完全一致（零回归）。
 * 开启后：
 * <ul>
 *   <li>cron 改由 admin 侧配置，本项目 yml 里的 {@code *-cron} 不再决定触发时机；</li>
 *   <li>各 Scheduler 的 {@code @Scheduled} 方法自动空转（见 {@code xxlJobEnabled} 判断），
 *       避免「admin 触发 + 本地 cron」跑两遍；</li>
 *   <li>多实例去重交给 admin 的路由策略（默认 FIRST，只挑一台执行），ShedLock 退居为
 *       关闭开关时的降级保障。</li>
 * </ul>
 * admin 不可达时不影响启动：注册由执行器内部线程周期重试并打日志，与 Redis/RabbitMQ/ES
 * 的「中间件缺位仍可启动」范式一致。
 * <p>
 * <b>勿给 {@code @XxlJob} handler 加 {@code @SchedulerLock}</b>：{@code enabled=true} 时本地
 * {@code @Scheduled} 方法只是空转返回，但它仍会按原 cron 去抢同名 ShedLock（{@code lockAtLeastFor}
 * 5~10 秒）。handler 若也用同名锁，admin 触发的运行就可能正好落在这个窗口里被静默跳过——
 * 多实例去重交给 admin 的路由策略（默认 FIRST）即可。
 * <p>
 * 任务一律使用 <b>BEAN 模式</b>（{@code @XxlJob} 绑定宿主 bean 方法），<b>不要</b>改用 GLUE：
 * xxl-job-core 2.4.1 的 {@code SpringGlueFactory.injectService(...)} 会解析
 * {@code javax.annotation.Resource}，而 Spring Boot 3 的类路径上只有 {@code jakarta.annotation-api}
 * （经 {@code dependency:build-classpath} 核实无 javax），GLUE 任务一执行即 NoClassDefFoundError。
 * <p>
 * 但<b>不能</b>据此推断「BEAN 模式不加载这个类」：{@code XxlJobSpringExecutor.afterSingletonsInstantiated()}
 * 在启动时就会调 {@code GlueFactory.refreshInstance(1)}，把 {@code SpringGlueFactory} 实例化出来
 * （字节码核实）。安全的真正原因是——解析 javax 的代码只存在于 {@code injectService} 内，而 BEAN 模式
 * 走 {@code MethodJobHandler}（字节码核实：无任何 injectService/GlueFactory 引用），那条路径永不执行。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "tastelink.xxl-job", name = "enabled", havingValue = "true")
public class XxlJobConfig {

    @Value("${tastelink.xxl-job.admin-addresses:}")
    private String adminAddresses;

    @Value("${tastelink.xxl-job.access-token:default_token}")
    private String accessToken;

    @Value("${tastelink.xxl-job.executor.appname:tastelink-executor}")
    private String appname;

    @Value("${tastelink.xxl-job.executor.address:}")
    private String address;

    @Value("${tastelink.xxl-job.executor.ip:}")
    private String ip;

    @Value("${tastelink.xxl-job.executor.port:9999}")
    private int port;

    @Value("${tastelink.xxl-job.executor.log-path:./logs/xxl-job/jobhandler}")
    private String logPath;

    @Value("${tastelink.xxl-job.executor.log-retention-days:30}")
    private int logRetentionDays;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        if (!StringUtils.hasText(adminAddresses)) {
            // 开关开着但没配地址：任务不会被触发（admin 侧也没有），显式告警避免静默失效
            log.warn("xxl-job enabled but tastelink.xxl-job.admin-addresses is blank — "
                    + "tasks will NOT be triggered; set XXL_JOB_ADMIN_ADDRESSES or disable the switch");
        }
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAccessToken(accessToken);
        executor.setAppname(appname);
        executor.setAddress(address);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        log.info("xxl-job executor init: appname={}, port={}, admin={}", appname, port, adminAddresses);
        return executor;
    }
}
