package com.tastelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tastelink.common.Constants;
import com.tastelink.common.PageResult;
import com.tastelink.common.ResultCode;
import com.tastelink.dto.request.CreateReportRequest;
import com.tastelink.dto.response.ReportVO;
import com.tastelink.entity.Report;
import com.tastelink.entity.User;
import com.tastelink.exception.BusinessException;
import com.tastelink.mapper.ReportMapper;
import com.tastelink.mapper.UserMapper;
import com.tastelink.service.ReportService;
import com.tastelink.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final UserMapper userMapper;

    /** 合法举报目标类型集合（防注入/校验）。 */
    private static final Set<String> VALID_TARGET_TYPES = Set.of(
            Constants.REPORT_TARGET_REVIEW,
            Constants.REPORT_TARGET_COMMENT,
            Constants.REPORT_TARGET_USER,
            Constants.REPORT_TARGET_SHOP);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void report(Long reporterId, CreateReportRequest req) {
        if (!StringUtils.hasText(req.getTargetType()) || !VALID_TARGET_TYPES.contains(req.getTargetType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "举报对象类型不合法");
        }
        Report r = new Report();
        r.setReporterId(reporterId);
        r.setTargetType(req.getTargetType());
        r.setTargetId(req.getTargetId());
        r.setReason(req.getReason());
        r.setStatus(Constants.REPORT_PENDING);
        try {
            reportMapper.insert(r);
        } catch (DuplicateKeyException dup) {
            // 幂等：同一用户已举报过同一目标 → 提示"已举报过该内容"（前端默认错误路径 toast 友好提示）
            log.debug("duplicate report ignored (idempotent): reporter={}, target={}:{}",
                    reporterId, req.getTargetType(), req.getTargetId());
            throw new BusinessException(ResultCode.ALREADY_REPORTED);
        }
    }

    @Override
    public PageResult<ReportVO> listReports(Integer page, Integer size, String status) {
        int p = page == null || page < 1 ? Constants.DEFAULT_PAGE : page;
        int s = size == null || size < 1 ? Constants.DEFAULT_SIZE : Math.min(size, Constants.MAX_SIZE);
        Page<Report> pg = new Page<>(p, s);
        LambdaQueryWrapper<Report> w = new LambdaQueryWrapper<Report>()
                .orderByDesc(Report::getCreateTime);
        if (StringUtils.hasText(status)) {
            w.eq(Report::getStatus, status);
        }
        reportMapper.selectPage(pg, w);
        List<Report> rows = pg.getRecords();
        // 批量回查举报人昵称
        List<Long> reporterIds = rows.stream().map(Report::getReporterId).distinct().toList();
        Map<Long, User> userMap = reporterIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(reporterIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
        List<ReportVO> vos = rows.stream().map(r -> ReportVO.builder()
                .id(r.getId())
                .reporterId(r.getReporterId())
                .reporterNickname(userMap.get(r.getReporterId()) == null ? null : userMap.get(r.getReporterId()).getNickname())
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .reason(r.getReason())
                .status(r.getStatus())
                .createTime(DateUtil.format(r.getCreateTime()))
                .build()).toList();
        return PageResult.from(pg, vos);
    }
}
