package com.siyan1234.itproject2nd.map.dao;

import com.siyan1234.itproject2nd.map.dto.MapFavoritePlaceDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 회원별 지도 즐겨찾기 조회와 소유권 조건 삭제를 수행하는 MyBatis Mapper입니다. */
@Mapper
public interface MapFavoritePlaceDao {

    List<MapFavoritePlaceDto> findAllByMemberNo(@Param("memberNo") int memberNo);

    MapFavoritePlaceDto findByMemberNoAndPlaceKey(@Param("memberNo") int memberNo,
                                                   @Param("placeKey") String placeKey);

    int insert(MapFavoritePlaceDto favorite);

    int deleteByFavoriteNoAndMemberNo(@Param("favoriteNo") int favoriteNo,
                                       @Param("memberNo") int memberNo);
}
