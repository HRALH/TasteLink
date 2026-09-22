package com.tastelink.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.glue.impl.SpringGlueFactory;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * XXL-JOB 执行器与 Spring Boot 3（jakarta 命名空间）的类路径兼容性守卫。
 * <p>
 * 补的是 {@link XxlJobHandlerTest} 覆盖不到的一环：那套用例是 Mockito 直接 new 出宿主 bean 测
 * handler 契约，**不经过执行器**；而「执行器在 jakarta 类路径上能不能起来」只靠注释论证过。
 * 这里用不依赖 Docker/admin 的两条断言把它钉住：
 * <ol>
 *   <li>{@code XxlJobConfig.xxlJobExecutor()} 能正常构造（执行器及其依赖类在纯 jakarta 类路径上可加载）；</li>
 *   <li>启动路径上的 {@code GlueFactory.refreshInstance(1)} → {@code SpringGlueFactory} 实例化不抛。
 *       <b>背景</b>：{@code SpringGlueFactory.injectService(...)} 引用了 {@code javax.annotation.Resource}，
 *       而 Spring Boot 3 类路径上只有 {@code jakarta.annotation-api}（经 {@code dependency:build-classpath}
 *       核实无 javax）。BEAN 模式走 {@code MethodJobHandler}，永不进入该方法（字节码核实），故安全；
 *       一旦有人把任务改成 GLUE，这条断言覆盖不到执行期——届时会是 {@code NoClassDefFoundError}。</li>
 * </ol>
 * 真实的 admin 注册/派发/回调联动仍需部署 xxl-job-admin，见 {@code docs/08 §5.4}。
 */
class XxlJobExecutorCompatTest {

    @Test
    void executorBean_constructsAndWiresOnJakartaClasspath() {
        XxlJobConfig config = new XxlJobConfig();
        ReflectionTestUtils.setField(config, "adminAddresses", "http://localhost:8888/xxl-job-admin");
        ReflectionTestUtils.setField(config, "accessToken", "default_token");
        ReflectionTestUtils.setField(config, "appname", "tastelink-executor");
        ReflectionTestUtils.setField(config, "address", "");
        ReflectionTestUtils.setField(config, "ip", "");
        ReflectionTestUtils.setField(config, "port", 9999);
        ReflectionTestUtils.setField(config, "logPath", "./logs/xxl-job/jobhandler");
        ReflectionTestUtils.setField(config, "logRetentionDays", 30);

        // 注意：xxl-job-core 的 XxlJobExecutor 只提供 setter、没有 getter，故这里无法断言字段值，
        // 断言的是「构造+rsetters 在 jakarta 类路径上不抛异常」——即 xuxueli/xxl-job 与 SB3 的兼容边界。
        XxlJobSpringExecutor executor = config.xxlJobExecutor();

        assertNotNull(executor, "执行器构造失败：xxl-job-core 与当前 Spring 版本可能不兼容");
    }

    @Test
    void startupPath_glueFactoryInstantiatesOnJakartaClasspath() {
        // XxlJobSpringExecutor.afterSingletonsInstantiated() 在应用启动时会做这一步
        GlueFactory.refreshInstance(1);

        assertInstanceOf(SpringGlueFactory.class, GlueFactory.getInstance(),
                "GlueFactory 实例化失败——BEAN 模式虽然不执行 injectService，但该类仍会在启动时被创建");
    }
}
