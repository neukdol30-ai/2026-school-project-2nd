package com.siyan1234.itproject2nd.map.dto;

/**
 * 즐겨찾기 API 공통 응답 형식입니다.
 */
public class MapFavoriteActionResponseDto {

    private final boolean success;
    private final String message;
    private final Object data;

    public MapFavoriteActionResponseDto(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public static MapFavoriteActionResponseDto success(String message, Object data) {
        return new MapFavoriteActionResponseDto(true, message, data);
    }

    public static MapFavoriteActionResponseDto failure(String message) {
        return new MapFavoriteActionResponseDto(false, message, null);
    }
}
