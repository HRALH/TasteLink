package com.tastelink.service.impl;

import com.tastelink.common.Constants;
import com.tastelink.dto.request.UpdateShopRequest;
import com.tastelink.entity.Shop;
import com.tastelink.entity.ShopCategory;
import com.tastelink.mapper.ShopCategoryMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.security.JwtUtil;
import com.tastelink.service.AdminShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 管理员改店铺集成测试（v2 Phase B）：真 MySQL + 真 Spring 上下文。
 * <p>
 * 与 {@code HotRankServiceIT} 不同，本验证需驱动 {@code MybatisPlusConfig} 的
 * {@code OptimisticLockerInnerInterceptor} 真实链路，故用 {@code @SpringBootTest} 而非手工装配。
 * 需本机 Docker 且以 {@code -DRUN_IT=true} 运行；否则整类跳过，不影响常规构建：
 * {@code mvn test -DRUN_IT=true -Dtest=AdminShopServiceIT}
 * <p>
 * 覆盖：① 成功改店后 version+1；② 乐观锁真冲突（后写入影响行数 0，先写入胜出）；
 * ③ 角色鉴权端到端（无 token→401，USER token→403，ADMIN token→200）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@EnabledIfSystemProperty(named = "RUN_IT", matches = "true")
class AdminShopServiceIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0").withInitScript("db/schema.sql");

    /** 覆盖数据源指向 Testcontainers；关闭热度缓存避免依赖 Redis。 */
    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("tastelink.rank.cache-enabled", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private AdminShopService adminShopService;
    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private ShopCategoryMapper categoryMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private TestRestTemplate restTemplate;

    private Long categoryId;

    @BeforeEach
    void seedCategory() {
        ShopCategory c = new ShopCategory();
        c.setCode("IT_CAT_" + System.nanoTime());
        c.setName("集成测试分类");
        c.setSortOrder(99);
        categoryMapper.insert(c);
        this.categoryId = c.getId();
    }

    private Long insertShop(String name) {
        Shop s = new Shop();
        s.setName(name);
        s.setCategoryId(categoryId);
        s.setCity("上海");
        s.setAddress("addr");
        s.setPhone("021-0");
        s.setStatus(Constants.STATUS_NORMAL);
        shopMapper.insert(s);
        return s.getId();
    }

    // ① 真实链路成功改店，version 自增、字段更新
    @Test
    void updateShop_succeeds_andVersionIncrements() {
        Long id = insertShop("辣府");
        Shop before = shopMapper.selectById(id);
        assertNotNull(before.getVersion());
        int v0 = before.getVersion();

        UpdateShopRequest req = new UpdateShopRequest();
        req.setName("辣府(新店名)");
        adminShopService.updateShop(id, req);

        Shop after = shopMapper.selectById(id);
        assertEquals("辣府(新店名)", after.getName());
        assertEquals(v0 + 1, after.getVersion(), "乐观锁 version 应自增");
    }

    // ② 乐观锁真冲突：两份过期快照并发写，先胜后败
    @Test
    void optimisticLock_staleSnapshotLosesAndFirstWriteWins() {
        Long id = insertShop("双改店");
        Shop s1 = shopMapper.selectById(id);   // 快照 A：version=0
        Shop s2 = shopMapper.selectById(id);  // 快照 B：version=0（另一份）

        s1.setName("A 改名");
        s2.setName("B 改名");
        int affected1 = shopMapper.updateById(s1);
        int affected2 = shopMapper.updateById(s2);

        assertEquals(1, affected1, "先写入应成功");
        assertEquals(0, affected2, "后写入用过期 version 应影响 0 行（乐观锁冲突）");

        Shop current = shopMapper.selectById(id);
        assertEquals("A 改名", current.getName(), "先写入胜出，后写入被拒");
        assertNotEquals("B 改名", current.getName());
        assertEquals(1, current.getVersion(), "version 仅自增一次");
    }

    // ③ 角色鉴权端到端
    @Test
    void adminEndpoint_roleGating() {
        Long id = insertShop("鉴权店");
        String url = "http://localhost:" + port + "/api/v1/admin/shops/" + id;
        String body = "{\"name\":\"通过鉴权后的改名\"}";

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);

        // 无 token → 401
        ResponseEntity<String> unauth = restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(body, h), String.class);
        assertEquals(401, unauth.getStatusCode().value());

        // USER token → 403
        String userToken = jwtUtil.generate(2L, "u", Constants.ROLE_USER);
        HttpHeaders uh = new HttpHeaders(h);
        uh.setBearerAuth(userToken);
        ResponseEntity<String> forbidden = restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(body, uh), String.class);
        assertEquals(403, forbidden.getStatusCode().value());

        // ADMIN token → 200
        String adminToken = jwtUtil.generate(1L, "admin", Constants.ROLE_ADMIN);
        HttpHeaders ah = new HttpHeaders(h);
        ah.setBearerAuth(adminToken);
        ResponseEntity<String> ok = restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(body, ah), String.class);
        assertEquals(200, ok.getStatusCode().value());
    }
}
