package com.siyan1234.itproject2nd.cookie.scheduler;

import com.siyan1234.itproject2nd.cookie.service.VisitLogMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 방문 기록 일별 집계 및 원본 로그 정리 스케줄러입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisitLogCleanupScheduler {

    private final VisitLogMaintenanceService visitLogMaintenanceService;

    /** 매일 새벽 3시 10분에 완료된 날짜를 집계하고 오래된 원본 로그를 삭제합니다. */
    @Scheduled(
            cron = "${app.visit-log.cleanup-cron:0 10 3 * * *}",
            zone = "${app.visit-log.zone:Asia/Seoul}"
    )
    public void aggregateAndCleanup() {
        try {
            visitLogMaintenanceService.aggregateAndCleanup();
        } catch (Exception e) {
            // 통계 정리 실패가 애플리케이션 전체 기능에 영향을 주지 않도록 로그만 남깁니다.
            log.error("방문 기록 일별 집계 또는 보관기간 정리에 실패했습니다.", e);
        }
    }
}
