package com.siyan1234.itproject2nd.cookie.service;

import com.siyan1234.itproject2nd.cookie.dao.VisitLogDao;
import com.siyan1234.itproject2nd.cookie.dto.VisitLogDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 방문 기록 저장 Service
 *
 * 방문 기록 저장 실패가 페이지 이용 실패로 이어지면 안 되므로 예외는 로그만 남깁니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitLogService {

    private final VisitLogDao visitLogDao;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveVisitLog(VisitLogDto visitLogDto) {
        if (visitLogDto == null || visitLogDto.getIpAddress() == null || visitLogDto.getIpAddress().isBlank()) {
            return;
        }

        try {
            if (visitLogDao.countTodayByIp(visitLogDto.getIpAddress()) > 0) {
                return;
            }

            visitLogDao.insert(visitLogDto);
        } catch (Exception e) {
            log.warn("방문 기록 저장 실패. visit_log 테이블 생성 여부를 확인하세요.", e);
        }
    }
}
