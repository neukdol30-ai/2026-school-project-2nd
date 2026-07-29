package com.siyan1234.itproject2nd.admin.dao;

import com.siyan1234.itproject2nd.admin.dto.AdminVisitOverviewDto;
import com.siyan1234.itproject2nd.admin.dto.AdminVisitSummaryDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 관리자 콘솔 방문 기록 관리 Mapper 인터페이스입니다. */
@Mapper
public interface AdminVisitDao {

    List<AdminVisitSummaryDto> findVisitSummaries(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size
    );

    Long countVisitSummaries(@Param("keyword") String keyword);

    /** 장기 일별 통계와 오늘 원본 로그를 합쳐 방문 현황을 조회합니다. */
    AdminVisitOverviewDto findVisitOverview();
}
