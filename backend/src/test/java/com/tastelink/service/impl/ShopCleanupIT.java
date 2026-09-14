package com.tastelink.service.impl;

import com.tastelink.config.RabbitMQConfig;
import com.tastelink.dto.mq.ShopCleanupMessage;
import com.tastelink.entity.Review;
import com.tastelink.entity.ReviewImage;
import com.tastelink.entity.Shop;
import com.tastelink.entity.ShopCategory;
import com.tastelink.entity.User;
import com.tastelink.mapper.ReviewCommentMapper;
import com.tastelink.mapper.ReviewImageMapper;
import com.tastelink.mapper.ReviewLikeMapper;
import com.tastelink.mapper.ReviewMapper;
import com.tastelink.mapper.ShopCategoryMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.mapper.UserMapper;
import com.tastelink.service.AdminShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 店铺删除延时清理集成测试（v2 Phase C）：真 MySQL + 真 RabbitMQ（@SpringBootTest）。
 * <p>
 * 标准 {@code rabbitmq:3-management} image 即可（TTL+DLX 免插件方案）。
 * 需本机 Docker 且 {@code -DRUN_IT=true} 运行；否则整类跳过，不影响常规构建：
 * {@code mvn test -DRUN_IT=true -Dtest=ShopCleanupIT}
 * <p>
 * 覆盖：删店标记后延时消息触达→物理级联删 image/like/comment/review/shop（用户 review_count 已于标记事务回扣）；
 * 重复投递幂等。
 */
@SpringBootTest
@Testcontainers
@EnabledIfSystemProperty(named = "RUN_IT", matches = "true")
class ShopCleanupIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0").withInitScript("db/schema.sql");

    @Container
    @SuppressWarnings("resource")
    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3.13-management");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.rabbitmq.host", RABBIT::getHost);
        r.add("spring.rabbitmq.port", () -> RABBIT.getAmqpPort());
        r.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        r.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        // 令延时窗口短、对账宽松，便于 IT 内可观测
        r.add("tastelink.rank.cache-enabled", () -> "false"); // 无 Redis 容器；热度降级
        r.add("tastelink.rabbitmq.cleanup-delay-ms", () -> "1000");
        r.add("tastelink.rabbitmq.reconcile-cron", () -> "*/10 * * * * *");
        r.add("tastelink.rabbitmq.reconcile-grace-ms", () -> "0");
    }

    @Autowired
    private AdminShopService adminShopService;
    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private ReviewImageMapper reviewImageMapper;
    @Autowired
    private ReviewLikeMapper reviewLikeMapper;
    @Autowired
    private ReviewCommentMapper reviewCommentMapper;
    @Autowired
    private ShopCategoryMapper categoryMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private Long categoryId;
    private Long userId;

    @BeforeEach
    void seedBase() {
        ShopCategory c = new ShopCategory();
        c.setCode("IT_CAT_" + System.nanoTime());
        c.setName("清理IT分类");
        c.setSortOrder(99);
        categoryMapper.insert(c);
        this.categoryId = c.getId();

        User u = new User();
        u.setUsername("it_user_" + System.nanoTime());
        u.setPassword("$2a$10$placeholder");
        u.setNickname("清理IT用户");
        u.setReviewCount(0);
        u.setStatus(1);
        u.setRole("USER");
        userMapper.insert(u);
        this.userId = u.getId();
    }

    private Long seedShopWithTwoReviews() {
        Shop s = new Shop();
        s.setName("清理IT店");
        s.setCategoryId(categoryId);
        s.setCity("上海");
        s.setAddress("addr");
        s.setPhone("021-0");
        s.setStatus(1);
        shopMapper.insert(s);

        for (int i = 0; i < 2; i++) {
            Review r = new Review();
            r.setShopId(s.getId());
            r.setUserId(userId);
            r.setCity("上海");
            r.setContent("c" + i);
            r.setRating(5);
            r.setLikeCount(0);
            r.setReplyCount(0);
            r.setStatus(1);
            reviewMapper.insert(r);
            // 每点评分一条图片（url 用 local 约定；文件不存在 → deleteIfExists 静默 no-op）
            ReviewImage img = new ReviewImage();
            img.setReviewId(r.getId());
            img.setUrl("http://localhost:8080/static/uploads/2026/09/it-" + i + ".jpg");
            img.setOssKey("");
            img.setSortOrder(0);
            reviewImageMapper.insert(img);
        }
        // 用户 published 计数镜像为 2（createReview 在生产代码里会 +2，IT 直插则手动置位以便断言回扣）
        userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("review_count = review_count + 2"));
        return s.getId();
    }

    @Test
    void deleteShop_triggersDelayedPhysicalCascade() {
        Long shopId = seedShopWithTwoReviews();
        List<Long> reviewIds = reviewMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Review>()
                        .eq(Review::getShopId, shopId)).stream().map(Review::getId).toList();
        int reviewBefore = userMapper.selectById(userId).getReviewCount();

        // 标记事务：标记下架+隐藏点评+回扣 user.review_count+afterCommit 发延时消息
        adminShopService.deleteShop(shopId);

        // 标记事务内 user.review_count 应已回扣 -2
        assertEquals(2, reviewBefore - userMapper.selectById(userId).getReviewCount(),
                "标记事务应按被删点评数回扣 user.review_count");

        // 延时窗口（cleanup-delay-ms=1s）后消费者物理级联清理
        await().atMost(java.time.Duration.ofSeconds(15)).untilAsserted(() -> {
            assertNull(shopMapper.selectById(shopId), "店铺行应被物理删除");
            assertTrue(reviewMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Review>()
                    .in(Review::getId, reviewIds)).isEmpty(), "点评行应被物理删除");
            assertTrue(reviewImageMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewImage>()
                    .in(ReviewImage::getReviewId, reviewIds)).isEmpty(), "图片行应被物理删除");
            assertTrue(reviewLikeMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.tastelink.entity.ReviewLike>()
                    .in(com.tastelink.entity.ReviewLike::getReviewId, reviewIds)).isEmpty(), "点赞行应被物理删除");
            assertTrue(reviewCommentMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.tastelink.entity.ReviewComment>()
                    .in(com.tastelink.entity.ReviewComment::getReviewId, reviewIds)).isEmpty(), "评论行应被物理删除");
        });
    }

    @Test
    void duplicateMessage_isIdempotent() {
        Long shopId = seedShopWithTwoReviews();
        adminShopService.deleteShop(shopId);
        await().atMost(java.time.Duration.ofSeconds(15)).untilAsserted(() ->
                assertNull(shopMapper.selectById(shopId)));

        // 待消费者处理完，再投一条重复消息（经延时队列再投递）：幂等 no-op，不应抛错或复活数据
        ShopCleanupMessage dup = new ShopCleanupMessage(shopId, System.currentTimeMillis());
        rabbitTemplate.convertAndSend(RabbitMQConfig.SUBMIT_EXCHANGE, RabbitMQConfig.ROUTING_KEY, dup);
        await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() ->
                assertNull(shopMapper.selectById(shopId)));
    }
}
