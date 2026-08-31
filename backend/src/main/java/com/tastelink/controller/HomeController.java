package com.tastelink.controller;

import com.tastelink.common.Constants;
import com.tastelink.common.R;
import com.tastelink.dto.response.HomeVO;
import com.tastelink.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页：热门店铺 + 热门点评，可按城市筛选。
 */
@RestController
@RequestMapping(Constants.API_V1 + "/home")
@RequiredArgsConstructor
public class HomeController {

    private final RecommendService recommendService;

    @GetMapping
    public R<HomeVO> home(@RequestParam(required = false) String city) {
        return R.ok(recommendService.home(city));
    }
}
