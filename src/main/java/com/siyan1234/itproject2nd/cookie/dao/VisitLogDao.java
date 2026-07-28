package com.siyan1234.itproject2nd.cookie.dao;

import com.siyan1234.itproject2nd.cookie.dto.VisitLogDto;
import org.apache.ibatis.annotations.Mapper;

/** 방문 기록 MyBatis Mapper 인터페이스 */
@Mapper
public interface VisitLogDao {

    /** 같은 IP의 하루 첫 방문만 저장하고, 이미 저장된 경우에는 아무 작업도 하지 않습니다. */
    int insertOncePerDay(VisitLogDto visitLogDto);
}
