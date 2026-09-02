package com.tastelink.service.impl;

import com.tastelink.common.PageResult;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.ShopVO;
import com.tastelink.entity.Shop;
import com.tastelink.entity.ShopCategory;
import com.tastelink.entity.ShopDoc;
import com.tastelink.mapper.ShopCategoryMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 店铺关键词 ES 检索集成测试（v2 Phase D）：真 MySQL + 真 Elasticsearch（@SpringBootTest）。
 * <p>
 * 标准 {@code elasticsearch:8.11} image（单节点 + 关 xpack 安全）；中文 IK 未装，本 IT 用英文可分词
 * ShopDoc（如 {@code "hotpot shanghai"}）验证查询/过滤/分页机制，<b>不</b>断言中文分词质量（infra 待办 A）。
 * 需本机 Docker 且 {@code -DRUN_IT=true} 运行；否则整类跳过，不影响常规构建：
 * {@code mvn test -DRUN_IT=true -Dtest=SearchServiceIT}
 * <p>
 * 注：ES 8.x 默认开安全，本地 IT 在容器上关 {@code xpack.security.enabled=false} 用 http 直连。
 */
@SpringBootTest
@Testcontainers
@EnabledIfSystemProperty(named = "RUN_IT", matches = "true")
class SearchServiceIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0").withInitScript("db/schema.sql");

    @Container
    @SuppressWarnings("resource")
    static final ElasticsearchContainer ES =
            new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.10.4"))
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("discovery.type", "single-node");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.elasticsearch.uris", () -> "http://" + ES.getHttpHostAddress());
        r.add("tastelink.rank.cache-enabled", () -> "false");   // 无 Redis 容器
        r.add("tastelink.search.enabled", () -> "true");
    }

    @Autowired
    private SearchService searchService;
    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private ShopCategoryMapper categoryMapper;
    @Autowired
    private ElasticsearchOperations esOps;

    private Long categoryId;

    @BeforeEach
    void seedCategory() {
        ShopCategory c = new ShopCategory();
        c.setCode("IT_CAT_" + System.nanoTime());
        c.setName("搜索IT分类");
        c.setSortOrder(99);
        categoryMapper.insert(c);
        this.categoryId = c.getId();
    }

    private Long seedAndIndexShop(String name, String city) {
        Shop s = new Shop();
        s.setName(name);
        s.setCategoryId(categoryId);
        s.setCity(city);
        s.setAddress("addr");
        s.setPhone("021-0");
        s.setStatus(1);
        shopMapper.insert(s);
        ShopDoc d = new ShopDoc();
        d.setId(s.getId());
        d.setName(name);
        d.setCity(city);
        d.setAddress("addr");
        d.setStatus(1);
        d.setCategoryId(categoryId);
        esOps.save(d);   // 索引 ShopDoc；ES 默认 refresh 间隔约 1s，用 Awaitility 等可检
        return s.getId();
    }

    @Test
    void keywordHit_returnsShopAndRespectsCityFilter() {
        Long hotpotId = seedAndIndexShop("hotpot shanghai", "上海");
        seedAndIndexShop("coffee hangzhou", "杭州");

        PageQuery pq = new PageQuery();
        // 等 ES refresh（约 1s）后 "hotpot" 命中那条店
        await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
            PageResult<ShopVO> r = searchService.searchByKeyword("hotpot", null, null, pq);
            assertTrue(r != null && !r.getRecords().isEmpty());
            assertEquals(hotpotId, r.getRecords().get(0).getId());
        });

        // city 过滤：上海 命中 hotpot；北京 应空
        await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() -> {
            PageResult<ShopVO> shanghai = searchService.searchByKeyword("hotpot", null, "上海", pq);
            assertTrue(shanghai != null && !shanghai.getRecords().isEmpty());
            PageResult<ShopVO> beijing = searchService.searchByKeyword("hotpot", null, "北京", pq);
            assertTrue(beijing == null || beijing.getRecords().isEmpty(), "北京应过滤掉上海店");
        });
    }

    @Test
    void noMatch_returnsEmptyButNotNull() {
        seedAndIndexShop("coffee hangzhou", "杭州");
        await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() -> {
            PageResult<ShopVO> r = searchService.searchByKeyword("zzznope", null, null, new PageQuery());
            assertTrue(r != null);
            assertEquals(0, r.getTotal());
            assertTrue(r.getRecords().isEmpty());
        });
    }
}
