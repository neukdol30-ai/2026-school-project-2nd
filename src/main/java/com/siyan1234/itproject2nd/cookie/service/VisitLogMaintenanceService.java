package com.siyan1234.itproject2nd.cookie.service;

import com.siyan1234.itproject2nd.cookie.dao.VisitLogDao;
import com.siyan1234.itproject2nd.cookie.config.VisitLogProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 방문 원본 로그의 일별 통계 집계와 보관기간 만료 데이터 정리를 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitLogMaintenanceService {

    private final VisitLogDao visitLogDao;
    private final VisitLogProperties visitLogProperties;

    /**
     * 완료된 날짜의 통계를 먼저 장기 통계 테이블에 반영한 뒤,
     * 보관기간이 지난 원본 방문 로그를 삭제합니다.
     */
    @Transactional
    public void aggregateAndCleanup() {
        int aggregatedRows = visitLogDao.mergeCompletedDailyStats();
        int deletedRows = visitLogDao.deleteExpiredVisitLogs(visitLogProperties.getSafeRetentionDays());

        log.info(
                "방문 기록 정리 완료. 통계 반영={}건, 원본 삭제={}건, 보관기간={}일",
                aggregatedRows,
                deletedRows,
                visitLogProperties.getSafeRetentionDays()
        );
    }
}
