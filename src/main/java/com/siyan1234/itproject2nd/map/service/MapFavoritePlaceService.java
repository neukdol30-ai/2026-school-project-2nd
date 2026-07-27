package com.siyan1234.itproject2nd.map.service;

import com.siyan1234.itproject2nd.map.dao.MapFavoritePlaceDao;
import com.siyan1234.itproject2nd.map.dto.MapFavoritePlaceDto;
import com.siyan1234.itproject2nd.map.dto.MapFavoriteSaveRequestDto;
import com.siyan1234.itproject2nd.map.dto.MapFavoriteSaveResultDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * 지도 즐겨찾기의 회원 소유권, 중복 판정 키, 좌표 정규화 규칙을 담당합니다.
 * 장소 검색 결과는 Kakao placeId로, 주소 검색 결과는 정규화한 좌표로 동일 장소를 판정합니다.
 */
@Service
public class MapFavoritePlaceService {

    private static final int COORDINATE_SCALE = 8;

    private final MapFavoritePlaceDao mapFavoritePlaceDao;

    public MapFavoritePlaceService(MapFavoritePlaceDao mapFavoritePlaceDao) {
        this.mapFavoritePlaceDao = mapFavoritePlaceDao;
    }

    @Transactional(readOnly = true)
    public List<MapFavoritePlaceDto> findAll(int memberNo) {
        validateMemberNo(memberNo);
        return mapFavoritePlaceDao.findAllByMemberNo(memberNo);
    }

    @Transactional
    public MapFavoriteSaveResultDto add(int memberNo, MapFavoriteSaveRequestDto request) {
        validateMemberNo(memberNo);

        MapFavoritePlaceDto favorite = normalize(memberNo, request);
        MapFavoritePlaceDto existing = mapFavoritePlaceDao.findByMemberNoAndPlaceKey(
                memberNo,
                favorite.getPlaceKey()
        );

        if (existing != null) {
            return new MapFavoriteSaveResultDto(false, existing);
        }

        int inserted;
        try {
            inserted = mapFavoritePlaceDao.insert(favorite);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 장소를 저장한 경우에도 DB UNIQUE 제약조건을 최종 중복 방어선으로 사용합니다.
            throw new IllegalArgumentException("이미 즐겨찾기에 저장된 장소입니다.", e);
        }

        if (inserted != 1) {
            throw new IllegalStateException("즐겨찾기를 저장하지 못했습니다.");
        }

        MapFavoritePlaceDto saved = mapFavoritePlaceDao.findByMemberNoAndPlaceKey(
                memberNo,
                favorite.getPlaceKey()
        );

        if (saved == null) {
            throw new IllegalStateException("저장된 즐겨찾기를 확인하지 못했습니다.");
        }

        return new MapFavoriteSaveResultDto(true, saved);
    }

    @Transactional
    public boolean delete(int memberNo, int favoriteNo) {
        validateMemberNo(memberNo);

        if (favoriteNo <= 0) {
            throw new IllegalArgumentException("즐겨찾기 번호가 올바르지 않습니다.");
        }

        // favoriteNo와 memberNo를 함께 조건으로 사용해 다른 회원의 즐겨찾기는 삭제할 수 없습니다.
        return mapFavoritePlaceDao.deleteByFavoriteNoAndMemberNo(favoriteNo, memberNo) == 1;
    }

    private MapFavoritePlaceDto normalize(int memberNo, MapFavoriteSaveRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("저장할 장소 정보가 없습니다.");
        }

        BigDecimal longitude = normalizeCoordinate(request.getLongitude());
        BigDecimal latitude = normalizeCoordinate(request.getLatitude());
        String sourceType = normalizeSourceType(request.getSourceType());
        String placeId = trimToNull(request.getPlaceId());

        if ("PLACE".equals(sourceType) && !StringUtils.hasText(placeId)) {
            throw new IllegalArgumentException("장소 식별자가 없습니다.");
        }

        MapFavoritePlaceDto favorite = new MapFavoritePlaceDto();
        favorite.setMemberNo(memberNo);
        favorite.setPlaceId(placeId);
        favorite.setPlaceKey(buildPlaceKey(placeId, longitude, latitude));
        favorite.setPlaceName(requiredText(request.getPlaceName(), "장소명이 없습니다."));
        favorite.setAddressName(requiredText(request.getAddressName(), "주소가 없습니다."));
        favorite.setCategoryName(trimToNull(request.getCategoryName()));
        favorite.setPhone(trimToNull(request.getPhone()));
        favorite.setPlaceUrl(trimToNull(request.getPlaceUrl()));
        favorite.setLongitude(longitude);
        favorite.setLatitude(latitude);
        favorite.setSourceType(sourceType);
        return favorite;
    }

    /** 장소 ID가 없는 주소 결과도 동일 좌표가 같은 키가 되도록 저장 좌표의 소수 자릿수를 고정합니다. */
    private String buildPlaceKey(String placeId, BigDecimal longitude, BigDecimal latitude) {
        if (StringUtils.hasText(placeId)) {
            return "KAKAO:" + placeId;
        }

        return "COORD:" + longitude.toPlainString() + "," + latitude.toPlainString();
    }

    private BigDecimal normalizeCoordinate(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("좌표 값이 없습니다.");
        }

        return value.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
    }

    private String normalizeSourceType(String value) {
        String normalized = requiredText(value, "장소 출처가 없습니다.").toUpperCase(Locale.ROOT);

        if (!"PLACE".equals(normalized) && !"ADDRESS".equals(normalized)) {
            throw new IllegalArgumentException("지원하지 않는 장소 출처입니다.");
        }

        return normalized;
    }

    private String requiredText(String value, String message) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateMemberNo(int memberNo) {
        if (memberNo <= 0) {
            throw new IllegalArgumentException("로그인 회원 정보가 올바르지 않습니다.");
        }
    }
}
