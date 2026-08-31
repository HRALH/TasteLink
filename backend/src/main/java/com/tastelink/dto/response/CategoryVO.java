package com.tastelink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryVO {

    private Long id;
    private String code;
    private String name;
    private String iconUrl;
    private Integer sortOrder;
}
