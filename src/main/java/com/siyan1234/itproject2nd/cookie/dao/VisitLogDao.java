package com.siyan1234.itproject2nd.cookie.dao;

import com.siyan1234.itproject2nd.cookie.dto.VisitLogDto;
import org.apache.ibatis.annotations.Mapper;

/** 방문 기록 MyBatis Mapper 인터페이스 */
@Mapper
public interface VisitLogDao {

    int insert(VisitLogDto visitLogDto);
}
