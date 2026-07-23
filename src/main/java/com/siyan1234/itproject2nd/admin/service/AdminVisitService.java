package com.siyan1234.itproject2nd.admin.service;

import com.siyan1234.itproject2nd.admin.dao.AdminVisitDao;
import com.siyan1234.itproject2nd.admin.dto.AdminVisitSummaryDto;
import com.siyan1234.itproject2nd.admin.support.AdminPagingHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 관리자 콘솔 방문 기록 관리 Service입니다. */
@Service
@RequiredArgsConstructor
public class AdminVisitService {

    private final AdminVisitDao adminVisitDao;

    @Transactional(readOnly = true)
    public List<AdminVisitSummaryDto> findVisitSummaries(String keyword, int page, int size) {
        int offset = AdminPagingHelper.calculateOffset(page, size);
        return adminVisitDao.findVisitSummaries(AdminPagingHelper.cleanText(keyword), offset, size);
    }

    @Transactional(readOnly = true)
    public long countVisitSummaries(String keyword) {
        Long count = adminVisitDao.countVisitSummaries(AdminPagingHelper.cleanText(keyword));
        return count == null ? 0L : count;
    }
}