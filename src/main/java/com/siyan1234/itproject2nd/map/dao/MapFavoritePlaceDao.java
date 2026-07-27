package com.siyan1234.itproject2nd.map.dao;

import com.siyan1234.itproject2nd.map.dto.MapFavoritePlaceDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MapFavoritePlaceDao {

    List<MapFavoritePlaceDto> findAllByMemberNo(@Param("memberNo") int memberNo);

    MapFavoritePlaceDto findByMemberNoAndPlaceKey(@Param("memberNo") int memberNo,
                                                   @Param("placeKey") String placeKey);

    int insert(MapFavoritePlaceDto favorite);

    int deleteByFavoriteNoAndMemberNo(@Param("favoriteNo") int favoriteNo,
                                       @Param("memberNo") int memberNo);
}
