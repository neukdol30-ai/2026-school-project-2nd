package com.siyan1234.itproject2nd.map.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 브라우저에서 즐겨찾기 추가 시 전달하는 장소 정보입니다.
 */
public class MapFavoriteSaveRequestDto {

    @Size(max = 100, message = "장소 식별자는 100자 이하여야 합니다.")
    private String placeId;

    @NotBlank(message = "장소명이 없습니다.")
    @Size(max = 200, message = "장소명은 200자 이하여야 합니다.")
    private String placeName;

    @NotBlank(message = "주소가 없습니다.")
    @Size(max = 500, message = "주소는 500자 이하여야 합니다.")
    private String addressName;

    @Size(max = 200, message = "카테고리는 200자 이하여야 합니다.")
    private String categoryName;

    @Size(max = 50, message = "전화번호는 50자 이하여야 합니다.")
    private String phone;

    @Size(max = 1000, message = "장소 URL은 1000자 이하여야 합니다.")
    private String placeUrl;

    @NotNull(message = "경도 값이 없습니다.")
    @DecimalMin(value = "123.0", message = "경도 값이 올바르지 않습니다.")
    @DecimalMax(value = "132.5", message = "경도 값이 올바르지 않습니다.")
    private BigDecimal longitude;

    @NotNull(message = "위도 값이 없습니다.")
    @DecimalMin(value = "32.0", message = "위도 값이 올바르지 않습니다.")
    @DecimalMax(value = "39.8", message = "위도 값이 올바르지 않습니다.")
    private BigDecimal latitude;

    @NotBlank(message = "장소 출처가 없습니다.")
    @Size(max = 20, message = "장소 출처는 20자 이하여야 합니다.")
    private String sourceType;

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getAddressName() {
        return addressName;
    }

    public void setAddressName(String addressName) {
        this.addressName = addressName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPlaceUrl() {
        return placeUrl;
    }

    public void setPlaceUrl(String placeUrl) {
        this.placeUrl = placeUrl;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
}
