package com.siyan1234.itproject2nd.cookie.dao;

import com.siyan1234.itproject2nd.cookie.dto.VisitLogDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 방문 기록 MyBatis Mapper 인터페이스 */
@Mapper
public interface VisitLogDao {

    /**
     * 같은 IP의 하루 첫 방문이면 INSERT하고, 당일 재방문이면 최근 방문 정보와 요청 횟수를 갱신합니다.
     */
    int upsertDailyVisit(VisitLogDto visitLogDto);

    /** 오늘 이전 날짜의 원본 로그를 개인정보 없는 일별 합계 테이블에 반영합니다. */
    int mergeCompletedDailyStats();

    /** 일별 합계가 끝난 원본 로그 중 보관기간이 지난 데이터를 삭제합니다. */
    int deleteExpiredVisitLogs(@Param("retentionDays") int retentionDays);
}
