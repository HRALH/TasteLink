package com.tastelink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tastelink.entity.ReviewImage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewImageMapper extends BaseMapper<ReviewImage> {

    /**
     * 单 SQL 批量插入（点评图片 ≤9 张，B3-3）。
     * 不用 MP saveBatch：其另开独立 SqlSession 执行批量，不参与当前 Spring 事务
     * （连接归还连接池时未提交即被回滚，反而丢数据）；注解 SQL 走 SqlSessionTemplate，
     * 与 createReview 的 @Transactional 同事务。create_time 由 SQL NOW() 写入
     * （绕过 MetaObjectHandler 的实体填充）。
     */
    @Insert("<script>"
            + "INSERT INTO t_review_image (review_id, url, oss_key, sort_order, create_time) VALUES "
            + "<foreach collection='list' item='i' separator=','>"
            + "(#{i.reviewId}, #{i.url}, #{i.ossKey}, #{i.sortOrder}, NOW())"
            + "</foreach>"
            + "</script>")
    int insertBatch(@Param("list") List<ReviewImage> list);
}
