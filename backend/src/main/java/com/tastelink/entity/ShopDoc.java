package com.tastelink.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * 店铺 ES 检索文档（v2 Phase D）。
 * <p>
 * 索引名 {@link #INDEX} 为代码常量（{@code @Document} 的 indexName 不支持 ${} 占位符解析，
 * 故不走 yml；如需改名改此处并 reindex）。
 * <p>
 * 仅放「搜索/过滤」必需字段：name/address/description 供 keyword 多字段匹配；status/categoryId/city 供过滤。
 * <b>不放</b> avgRating/reviewCount/categoryName —— 搜索结果经 ShopService 组装 ShopVO 时从 MySQL
 * 回查，保证计数/分类名与库一致（避免索引内冗余并随取消同步漂移）。
 * <p>
 * 默认 analyzer（标准镜像可跑通）；中文 IK 分词见 backend/docs/02 §4.9「infra 待办 A」，
 * 需安装 analysis-ik 并以 ik_max_word/ik_smart analyzer 重建索引。
 */
@Data
@Document(indexName = ShopDoc.INDEX, createIndex = false)
public class ShopDoc {

    /** ES 索引名——SearchService/Reconcile/Initializer 共用此常量。 */
    public static final String INDEX = "tastelink-shop";

    @Id
    @Field(type = FieldType.Long)
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String address;

    @Field(type = FieldType.Text)
    private String description;

    /** 1 正常 / 0 下架；搜索过滤 status=1 */
    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String city;
}
