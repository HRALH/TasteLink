package com.tastelink.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopVO {

    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String city;
    private String address;
    private String coverUrl;
    /** 平均评分 0.00-5.00 */
    private BigDecimal avgRating;
    private Integer reviewCount;
}
