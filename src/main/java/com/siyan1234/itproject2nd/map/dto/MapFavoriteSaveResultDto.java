package com.siyan1234.itproject2nd.map.dto;

/**
 * 즐겨찾기 추가 결과입니다.
 * created=false이면 이미 저장되어 있던 즐겨찾기를 반환합니다.
 */
public class MapFavoriteSaveResultDto {

    private final boolean created;
    private final MapFavoritePlaceDto favorite;

    public MapFavoriteSaveResultDto(boolean created, MapFavoritePlaceDto favorite) {
        this.created = created;
        this.favorite = favorite;
    }

    public boolean isCreated() {
        return created;
    }

    public MapFavoritePlaceDto getFavorite() {
        return favorite;
    }
}
